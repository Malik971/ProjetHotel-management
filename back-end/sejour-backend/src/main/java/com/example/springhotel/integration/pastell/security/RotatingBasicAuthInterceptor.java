package com.example.springhotel.integration.pastell.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Interceptor RestClient qui pose un header {@code Authorization: Basic ...}
 * dont le mot de passe est recalcule a chaque requete depuis le
 * {@link PastellCredentialsProvider}.
 * <p>
 * Difference avec {@code BasicAuthenticationInterceptor} de Spring : celui-ci
 * fige les credentials a la construction. Si on l'utilisait avec un mot de passe
 * rotatif, le RestClient continuerait a envoyer hier indefiniment apres minuit
 * UTC. Le present interceptor lit le provider a chaque {@link #intercept} pour
 * eviter ce piege.
 * <p>
 * <b>Cout :</b> une derivation HMAC-SHA256 par requete sortante Pastell. C'est
 * negligeable (microsecondes), inutile de mettre en cache.
 */
public class RotatingBasicAuthInterceptor implements ClientHttpRequestInterceptor {

    private final PastellCredentialsProvider provider;

    public RotatingBasicAuthInterceptor(PastellCredentialsProvider provider) {
        this.provider = provider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String username = provider.getUsername();
        String password = provider.getCurrentPassword();
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return execution.execute(request, body);
    }
}
