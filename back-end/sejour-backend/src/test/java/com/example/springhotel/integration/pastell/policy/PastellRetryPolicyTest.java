package com.example.springhotel.integration.pastell.policy;

import com.example.springhotel.integration.pastell.client.PastellApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la politique de decision retryable / non-retryable.
 *
 * On teste la table de decision exhaustivement, parce que c'est le centre
 * nevralgique du Lot 4 : se tromper ici = soit on spamme Pastell sur des
 * erreurs incorrigibles, soit on abandonne des erreurs transitoires.
 */
class PastellRetryPolicyTest {

    private final PastellRetryPolicy policy = new PastellRetryPolicy();

    @ParameterizedTest
    @CsvSource({
            "200, false",  // succes (ne devrait pas arriver mais on teste)
            "400, false",  // bad request : payload invalide
            "401, false",  // mauvais credentials
            "403, false",  // pas le droit
            "404, false",  // ressource introuvable
            "409, false",  // conflit metier
            "408, true",   // request timeout : retryable
            "429, true",   // too many requests : retryable
            "500, true",   // internal server error
            "502, true",   // bad gateway
            "503, true",   // service unavailable
            "504, true"    // gateway timeout
    })
    void isRetryable_codeHttp(int status, boolean expected) {
        PastellApiException e = new PastellApiException(status, "{}", "test");
        assertThat(policy.isRetryable(e)).isEqualTo(expected);
    }

    @Test
    void isRetryable_erreurReseau_estRetryable() {
        PastellApiException e = new PastellApiException("timeout", new RuntimeException());
        assertThat(policy.isRetryable(e)).isTrue();
        assertThat(e.hasHttpResponse()).isFalse();
    }

    @Test
    void isRetryable_exceptionNull_pasRetryable() {
        assertThat(policy.isRetryable((PastellApiException) null)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "'[NETWORK] timeout',                  true",
            "'[500] internal',                     true",
            "'[502] bad gateway',                  true",
            "'[408] request timeout',              true",
            "'[429] rate limit',                   true",
            "'[401] bad creds',                    false",
            "'[403] forbidden',                    false",
            "'[404] not found',                    false",
            "'[400] bad request',                  false",
            "'message sans prefixe',               false",
            "'[abc] format casse',                 false"
    })
    void isRetryable_messageStocke(String message, boolean expected) {
        assertThat(policy.isRetryable(message)).isEqualTo(expected);
    }

    @Test
    void isRetryable_messageVide_retryParDefaut() {
        // Cas du PENDING orphelin : pas de derniereErreur. On retry.
        assertThat(policy.isRetryable("")).isTrue();
        assertThat(policy.isRetryable((String) null)).isTrue();
    }
}