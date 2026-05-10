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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la securite du mock Pastell.
 *
 * Posture choisie :
 *   - HTTP Basic uniquement (Pastell n'accepte rien d'autre)
 *   - Stateless : pas de session, chaque requete porte ses credentials
 *   - CSRF desactive : API REST, pas de formulaire HTML
 *   - CORS permissif : c'est un MOCK DEV/CI, le dashboard demo est ouvert
 *     depuis file:// ou localhost. Cette config ne va JAMAIS en production.
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
     */
    @Value("${pastell.mock.username}")
    private String mockUsername;

    /**
     * Mot de passe de l'utilisateur Pastell mock.
     */
    @Value("${pastell.mock.password}")
    private String mockPassword;

    @Bean
    public SecurityFilterChain mockSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // Active CORS et le branche sur le bean corsConfigurationSource defini ci-dessous.
                // Sans cette ligne, le serveur ignore les headers CORS meme si le bean existe.
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight CORS (OPTIONS) : autoriser sans authentification.
                        // Sans ca, le navigateur recoit un 401 sur le pre-flight et bloque
                        // tout, meme avant d'envoyer la vraie requete.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Handshake anonyme
                        .requestMatchers(HttpMethod.GET, "/api/version.php").permitAll()

                        // API Pastell : protegee par HTTP Basic
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * Configuration CORS permissive : autorise toutes les origines, methodes
     * et headers, et permet l'envoi de credentials (Authorization Basic).
     *
     * POURQUOI AUSSI PERMISSIF ?
     *   - Ce mock est un outil de dev/demo local. Il n'est jamais expose sur
     *     internet, ni en production.
     *   - Le dashboard de demo (springhotel-pastell-demo.html) peut etre ouvert
     *     depuis file:// (origine "null"), depuis localhost:5173, depuis un IDE,
     *     etc. On ne veut pas avoir a maintenir une whitelist d'origines.
     *   - La vraie barriere de securite reste l'authentification HTTP Basic :
     *     un attaquant qui aurait le mot de passe pourrait deja appeler le mock
     *     directement sans passer par CORS.
     *
     * EN PRODUCTION (sur un vrai Pastell) :
     *   - Cette config n'existe pas, parce que le mock n'existe pas en prod.
     *   - Le vrai Pastell de Libriciel a sa propre politique CORS, decidee
     *     par les administrateurs de la plateforme.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // allowedOriginPatterns au lieu de allowedOrigins : permet les wildcards
        // ET le credentials=true en meme temps (allowedOrigins("*") + credentials=true
        // est rejete par Spring depuis la version 5.3 pour des raisons de securite).
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService mockUserDetailsService() {
        UserDetails sejourUser = User.builder()
                .username(mockUsername)
                .password(mockPassword)
                .authorities("PASTELL_USER")
                .build();
        return new InMemoryUserDetailsManager(sejourUser);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder mockPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}