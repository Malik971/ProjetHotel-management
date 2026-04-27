package com.example.pastellmock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de la securite du mock Pastell.
 *
 * Posture choisie :
 *   - HTTP Basic uniquement (Pastell n'accepte rien d'autre)
 *   - Stateless : pas de session, chaque requete porte ses credentials
 *   - CSRF desactive : API REST, pas de formulaire HTML
 *   - {@code /api/version.php} : ANONYME, sert de handshake / healthcheck
 *   - Tout le reste de {@code /api/**} : protege par HTTP Basic
 *
 * Decision : version.php anonyme.
 * Justification : ce endpoint sert de "ping" pour verifier que le mock est
 * joignable, avant meme d'avoir configure les credentials cote client. C'est
 * aussi pratique pour les sondes UptimeRobot/healthcheck en CI.
 *
 * Securite des mots de passe :
 *   - Le mock utilise NoOpPasswordEncoder (mots de passe en clair en memoire).
 *     C'est ACCEPTABLE ici parce que :
 *       1. C'est un mock dev/CI, jamais expose en production
 *       2. Les credentials viennent de variables d'environnement (cf. .env.example),
 *          ils ne sont pas commites
 *       3. Pastell reel utilise lui-meme du HTTP Basic, donc la securite reelle
 *          de l'integration repose sur HTTPS, pas sur le hash du mot de passe
 *     En production, on n'utilise JAMAIS NoOpPasswordEncoder.
 */
@Configuration
public class MockSecurityConfig {

    /**
     * Login de l'utilisateur Pastell mock.
     * Doit etre exporte comme variable d'environnement avant le demarrage :
     * {@code export PASTELL_MOCK_USERNAME=sejour}
     */
    @Value("${pastell.mock.username}")
    private String mockUsername;

    /**
     * Mot de passe de l'utilisateur Pastell mock.
     * Doit etre exporte comme variable d'environnement avant le demarrage :
     * {@code export PASTELL_MOCK_PASSWORD=sejour-mock-pwd}
     */
    @Value("${pastell.mock.password}")
    private String mockPassword;

    @Bean
    public SecurityFilterChain mockSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Handshake anonyme : sert a verifier la joignabilite du mock
                        // sans avoir besoin de credentials valides cote client.
                        .requestMatchers(HttpMethod.GET, "/api/version.php").permitAll()

                        // Tout le reste de l'API Pastell est protege par HTTP Basic
                        .requestMatchers("/api/**").authenticated()

                        // Tout autre chemin (ex. /, /error) : on laisse passer
                        // pour ne pas casser les pages d'erreur Spring par defaut
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * Utilisateur unique du mock.
     * En vrai Pastell, plusieurs utilisateurs peuvent exister. Pour le mock,
     * un seul suffit : c'est l'utilisateur technique que sejour-backend
     * utilisera pour s'authentifier.
     */
    @Bean
    public UserDetailsService mockUserDetailsService() {
        UserDetails sejourUser = User.builder()
                .username(mockUsername)
                .password(mockPassword)
                // Pas de role specifique : Pastell ne fait pas d'autorisation
                // par role HTTP, c'est gere au niveau metier (entite_id)
                .authorities("PASTELL_USER")
                .build();
        return new InMemoryUserDetailsManager(sejourUser);
    }

    /**
     * NoOpPasswordEncoder pour stocker les mots de passe en clair en memoire.
     * Voir le javadoc de la classe pour la justification.
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder mockPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}