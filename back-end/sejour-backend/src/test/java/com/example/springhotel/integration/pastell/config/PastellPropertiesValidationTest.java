package com.example.springhotel.integration.pastell.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifie que la validation conditionnelle dans {@link PastellProperties#validateIfEnabled()}
 * echoue au demarrage avec un message clair quand pastell.enabled=true mais qu'une
 * configuration obligatoire manque.
 *
 * Cas couverts :
 *   - url manquante
 *   - username manquant
 *   - password manquant
 *   - entite-id invalide
 *
 * Approche : on lance SpringApplication avec une config incomplete et on verifie
 * que le contexte echoue avec un IllegalStateException qui contient le nom de
 * la property manquante. Le message d'erreur doit etre auto-explicatif pour
 * que le developpeur sache immediatement quoi corriger.
 *
 * DevRel note : tester les messages d'erreur est souvent neglige. Pour un
 * produit destine a etre integre par des tiers, la qualite des messages d'echec
 * vaut autant que celle de la documentation nominale.
 *
 * Robustesse face aux profils :
 *   On force EXPLICITEMENT spring.profiles.active=test pour ce test, et non plus
 *   via setAdditionalProfiles. Raison : setAdditionalProfiles AJOUTE un profil
 *   aux profils deja actifs (variable d'environnement, configuration IntelliJ,
 *   etc.). Si "dev" est actif par ailleurs, application-dev.properties est
 *   charge et fournit potentiellement des valeurs (url, username, password)
 *   qui empechent la validation de planter. En forcant via l'argument CLI
 *   --spring.profiles.active=test, on remplace la liste complete des profils
 *   actifs et on garantit que seul application-test.properties est charge.
 *
 * Robustesse face aux properties par defaut :
 *   Meme avec uniquement le profil "test", application.properties (toujours
 *   charge en base) peut contenir des valeurs Pastell par defaut. On surcharge
 *   donc EXPLICITEMENT chaque property qu'on veut "vider" avec une chaine vide.
 *   La validation utilise isBlank() qui traite "" comme invalide, donc le
 *   comportement est garanti independamment de la cascade de properties.
 *
 *   Lot 6 : on inclut aussi pastell.master-secret dans la liste des properties
 *   a vider explicitement. Raison : Spring Boot resout les variables d'environnement
 *   (PASTELL_MASTER_SECRET) automatiquement, et si un dev a expose cette variable
 *   dans son shell (pour le mode rotatif), elle serait reprise ici et la
 *   validation considererait le mode rotatif actif, donc skiperait le check
 *   username/password. En vidant explicitement master-secret, on force le mode
 *   statique pour ces tests, ce qui restaure le comportement attendu.
 *
 * Le test ne devient ainsi sensible qu'aux arguments qu'il passe, pas a
 * l'environnement de l'utilisateur. C'est ce qu'on attend d'un test unitaire.
 */
class PastellPropertiesValidationTest {

    @Test
    void startup_fails_when_url_is_missing() {
        assertThatThrownBy(() -> startWithProperties(
                "--pastell.enabled=true",
                "--pastell.url=",                          // explicitement vide
                "--pastell.username=u",
                "--pastell.password=p",
                "--pastell.master-secret=",                // Lot 6 : force le mode statique
                "--pastell.entite-id=1"
        ))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pastell.url");
    }

    @Test
    void startup_fails_when_username_is_missing() {
        assertThatThrownBy(() -> startWithProperties(
                "--pastell.enabled=true",
                "--pastell.url=http://localhost:9999",
                "--pastell.username=",                     // explicitement vide
                "--pastell.password=p",
                "--pastell.master-secret=",                // Lot 6 : force le mode statique
                "--pastell.entite-id=1"
        ))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pastell.username");
    }

    @Test
    void startup_fails_when_password_is_missing() {
        assertThatThrownBy(() -> startWithProperties(
                "--pastell.enabled=true",
                "--pastell.url=http://localhost:9999",
                "--pastell.username=u",
                "--pastell.password=",                     // explicitement vide
                "--pastell.master-secret=",                // Lot 6 : force le mode statique
                "--pastell.entite-id=1"
        ))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pastell.password");
    }

    @Test
    void startup_fails_when_entite_id_is_invalid() {
        assertThatThrownBy(() -> startWithProperties(
                "--pastell.enabled=true",
                "--pastell.url=http://localhost:9999",
                "--pastell.username=u",
                "--pastell.password=p",
                "--pastell.master-secret=",                // Lot 6 : force le mode statique
                "--pastell.entite-id=0"
        ))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pastell.entite-id");
    }

    /**
     * Demarre un contexte Spring minimal (non-web) uniquement pour declencher
     * la validation dans PastellConfig. On evite un @SpringBootTest complet
     * parce qu'on veut que l'application CRASHE au demarrage, pas qu'elle
     * reussisse a se lever.
     *
     * Force le profil "test" via argument CLI plutot que via
     * setAdditionalProfiles : cette forme REMPLACE la liste de profils actifs
     * (alors que setAdditionalProfiles n'aurait fait qu'ajouter "test" aux
     * profils deja actifs comme "dev", ce qui rendait le test fragile).
     */
    private void startWithProperties(String... args) {
        SpringApplication app = new SpringApplication(MinimalTestApp.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        // Construire le tableau d'arguments final : --spring.profiles.active=test
        // en premier, suivi des args du test. Cet argument CLI surcharge toute
        // configuration externe (variables d'env, IntelliJ run config, etc.).
        String[] finalArgs = new String[args.length + 1];
        finalArgs[0] = "--spring.profiles.active=test";
        System.arraycopy(args, 0, finalArgs, 1, args.length);

        app.run(finalArgs).close();
    }

    /**
     * Application minimale n'incluant que la configuration Pastell.
     * Permet de tester la validation de maniere isolee, sans embarquer
     * la base de donnees ni le reste du backend Sejour.
     */
    @SpringBootApplication(scanBasePackages = "com.example.springhotel.integration.pastell.config")
    static class MinimalTestApp {
    }
}