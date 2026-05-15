package com.example.springhotel.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de JwtService.
 * <p>
 * On instancie JwtService a la main avec un JwtProperties controle, pour
 * pouvoir manipuler la duree d'expiration dans certains tests sans
 * dependre du contexte Spring complet.
 */
class JwtServiceTest {

    /**
     * Une cle de 64 caracteres ASCII, suffisante pour HS256. Cle de test
     * uniquement, jamais utilisee en prod.
     */
    private static final String TEST_SECRET =
            "test-secret-tres-long-et-vraiment-aleatoire-pour-les-tests-jwt-ok";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpirationMillis(60_000L); // 1 minute
        jwtService = new JwtService(props);
    }

    @Nested
    @DisplayName("generation et lecture")
    class GenerationEtLecture {

        @Test
        @DisplayName("un token genere puis decode rend les bonnes valeurs")
        void roundTrip() {
            String token = jwtService.generateToken(
                    "malik@example.com",
                    42L,
                    List.of("ROLE_USER")
            );

            assertThat(token).isNotBlank();
            assertThat(jwtService.extractEmail(token)).isEqualTo("malik@example.com");
            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
            assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_USER");
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("un token avec plusieurs roles preserve l'ordre")
        void plusieursRoles() {
            String token = jwtService.generateToken(
                    "admin@example.com",
                    1L,
                    List.of("ROLE_ADMIN", "ROLE_USER")
            );

            assertThat(jwtService.extractRoles(token))
                    .containsExactly("ROLE_ADMIN", "ROLE_USER");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("un token null est invalide")
        void tokenNull() {
            assertThat(jwtService.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("un token vide est invalide")
        void tokenVide() {
            assertThat(jwtService.isTokenValid("")).isFalse();
            assertThat(jwtService.isTokenValid("   ")).isFalse();
        }

        @Test
        @DisplayName("un token corrompu est invalide")
        void tokenCorrompu() {
            assertThat(jwtService.isTokenValid("ceci-n-est-pas-un-jwt")).isFalse();
            assertThat(jwtService.isTokenValid("a.b.c")).isFalse();
        }

        @Test
        @DisplayName("un token signe avec une autre cle est invalide")
        void tokenSigneAvecAutreCle() {
            // Genere un token avec une cle differente
            JwtProperties otherProps = new JwtProperties();
            otherProps.setSecret("autre-cle-tres-longue-et-non-aleatoire-mais-suffisante-haha-ok");
            otherProps.setExpirationMillis(60_000L);
            JwtService otherService = new JwtService(otherProps);

            String tokenForeign = otherService.generateToken(
                    "intrus@example.com", 99L, List.of("ROLE_ADMIN"));

            // Notre service doit rejeter ce token signe par quelqu'un d'autre
            assertThat(jwtService.isTokenValid(tokenForeign)).isFalse();
        }

        @Test
        @DisplayName("un token expire est invalide")
        void tokenExpire() throws InterruptedException {
            // Service avec une expiration de 1 ms
            JwtProperties shortProps = new JwtProperties();
            shortProps.setSecret(TEST_SECRET);
            shortProps.setExpirationMillis(1L);
            JwtService shortService = new JwtService(shortProps);

            String token = shortService.generateToken("a@b.com", 1L, List.of("ROLE_USER"));

            // On attend que le token expire reellement
            Thread.sleep(50);

            assertThat(shortService.isTokenValid(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("integrite des claims")
    class IntegriteClaims {

        @Test
        @DisplayName("le sub correspond a l'email passe en entree")
        void subjectEqualsEmail() {
            String token = jwtService.generateToken("x@y.fr", 10L, List.of("ROLE_USER"));
            assertThat(jwtService.extractEmail(token)).isEqualTo("x@y.fr");
        }

        @Test
        @DisplayName("userId conserve son type Long meme apres serialisation")
        void userIdEstLong() {
            String token = jwtService.generateToken("x@y.fr", 9_999_999L, List.of("ROLE_USER"));
            assertThat(jwtService.extractUserId(token)).isEqualTo(9_999_999L);
        }

        @Test
        @DisplayName("la cle est bien construite et stockee")
        void cleConstruite() throws Exception {
            Field f = JwtService.class.getDeclaredField("signingKey");
            f.setAccessible(true);
            assertThat(f.get(jwtService)).isNotNull();
        }
    }
}