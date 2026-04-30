package com.example.springhotel.integration.pastell.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifie que lorsque pastell.enabled=false (valeur par defaut),
 * AUCUN bean d'integration Pastell n'est present dans le contexte Spring.
 *
 * Ce test garantit le principe "integration optionnelle, zero impact quand desactivee" :
 *   - L'application peut demarrer sans aucune configuration Pastell
 *   - Aucune tentative d'appel reseau n'est faite
 *   - Impossible d'injecter par erreur un bean Pastell quelque part
 *
 * Si ce test echoue, c'est que la condition @ConditionalOnProperty ne fonctionne pas,
 * et l'application risque de faire des appels reseau meme desactivee.
 *
 * Pourquoi @ActiveProfiles("test") ?
 *   Active le profil "test" qui charge application-test.properties.
 *   Resultat : H2 en memoire au lieu de PostgreSQL, Flyway desactive.
 *   Permet de lancer ce test sans Docker / PostgreSQL local demarre.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pastell.enabled=false"
})
class PastellDisabledByDefaultTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void pastellRestClient_bean_should_not_exist() {
        // Le bean "pastellRestClient" ne doit pas exister quand pastell.enabled=false
        assertThatThrownBy(() -> context.getBean(PastellConfig.PASTELL_REST_CLIENT, RestClient.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void pastellConfig_bean_should_not_exist() {
        // La classe de config elle-meme ne doit pas etre instanciee
        assertThatThrownBy(() -> context.getBean(PastellConfig.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void pastellProperties_bean_is_still_available() {
        // En revanche, PastellProperties reste disponible (il est charge par PastellStartupLogger
        // qui, lui, tourne toujours pour afficher le banner "DISABLED"). C'est intentionnel.
        PastellProperties props = context.getBean(PastellProperties.class);
        assertThat(props).isNotNull();
        assertThat(props.isEnabled()).isFalse();
    }
}