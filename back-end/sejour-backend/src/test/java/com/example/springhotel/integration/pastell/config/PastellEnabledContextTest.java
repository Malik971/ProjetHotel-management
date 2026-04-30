package com.example.springhotel.integration.pastell.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie que lorsque pastell.enabled=true avec une config valide,
 * tous les beans d'integration sont crees et correctement configures.
 *
 * Ce test ne fait AUCUN appel reseau : il valide uniquement que le contexte
 * Spring se leve correctement avec les bonnes properties. C'est l'equivalent
 * d'un "smoke test de configuration".
 *
 * NB : on utilise une URL bidon (localhost:9999) parce qu'aucun appel HTTP
 * n'est effectue au Lot 1. Le premier appel reel aura lieu au Lot 3.
 *
 * Pourquoi @ActiveProfiles("test") ?
 *   Active le profil "test" qui charge application-test.properties.
 *   Resultat : H2 en memoire au lieu de PostgreSQL, Flyway desactive.
 *   Permet de lancer ce test sans Docker / PostgreSQL local demarre.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pastell.enabled=true",
        "pastell.mode=MOCK",
        "pastell.url=http://localhost:9999",
        "pastell.username=test-user",
        "pastell.password=test-password",
        "pastell.entite-id=1",
        "pastell.type-dossier=reservation-hoteliere",
        "pastell.timeout-ms=3000"
})
class PastellEnabledContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PastellProperties properties;

    @Autowired
    @Qualifier(PastellConfig.PASTELL_REST_CLIENT)
    private RestClient pastellRestClient;

    @Test
    void pastellProperties_should_be_loaded_correctly() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMode()).isEqualTo(PastellProperties.Mode.MOCK);
        assertThat(properties.getUrl()).isEqualTo("http://localhost:9999");
        assertThat(properties.getUsername()).isEqualTo("test-user");
        assertThat(properties.getPassword()).isEqualTo("test-password");
        assertThat(properties.getEntiteId()).isEqualTo(1L);
        assertThat(properties.getTypeDossier()).isEqualTo("reservation-hoteliere");
        assertThat(properties.getTimeoutMs()).isEqualTo(3000);
    }

    @Test
    void pastellRestClient_bean_should_be_present_and_autowirable() {
        // Le bean existe
        assertThat(pastellRestClient).isNotNull();

        // Il est bien recuperable par son nom aussi
        RestClient byName = context.getBean(PastellConfig.PASTELL_REST_CLIENT, RestClient.class);
        assertThat(byName).isSameAs(pastellRestClient);
    }

    @Test
    void pastellConfig_bean_should_be_present() {
        PastellConfig config = context.getBean(PastellConfig.class);
        assertThat(config).isNotNull();
    }

    @Test
    void webhook_and_polling_defaults_should_apply() {
        // Valeurs par defaut appliquees meme sans property explicite
        assertThat(properties.getWebhook().isEnabled()).isFalse();
        assertThat(properties.getPolling().getIntervalMs()).isEqualTo(30000L);
    }
}