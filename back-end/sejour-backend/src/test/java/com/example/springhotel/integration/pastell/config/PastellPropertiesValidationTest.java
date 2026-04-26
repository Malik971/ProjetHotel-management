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
 *   - entite-id manquante
 *
 * Approche : on lance SpringApplication avec une config incomplete et on verifie
 * que le contexte echoue avec un IllegalStateException qui contient le nom de
 * la property manquante. Le message d'erreur doit etre auto-explicatif pour
 * que le developpeur sache immediatement quoi corriger.
 *
 * DevRel note : tester les messages d'erreur est souvent neglige. Pour un
 * produit destine a etre integre par des tiers, la qualite des messages d'echec
 * vaut autant que celle de la documentation nominale.
 */
class PastellPropertiesValidationTest {

    @Test
    void startup_fails_when_url_is_missing() {
        assertThatThrownBy(() -> startWithProperties(
                "--pastell.enabled=true",
                "--pastell.username=u",
                "--pastell.password=p",
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
                "--pastell.password=p",
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
     */
    private void startWithProperties(String... args) {
        SpringApplication app = new SpringApplication(MinimalTestApp.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args).close();
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