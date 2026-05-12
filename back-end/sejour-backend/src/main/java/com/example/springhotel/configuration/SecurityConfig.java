package com.example.springhotel.configuration;

import com.example.springhotel.filter.DemoRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration Spring Security de sejour-backend.
 * <p>
 * <b>Evolution Lot 6 :</b> ajout du {@link DemoRateLimitFilter} avant le filtre
 * d'authentification, et autorisation explicite des endpoints {@code /actuator/health}
 * et {@code /actuator/info} pour les sondes Render et UptimeRobot.
 * <p>
 * Le rate limit filter est branche AVANT le filtre d'authentification standard,
 * pour qu'une IP qui spam ne consomme meme pas le cout d'une authentification
 * (verification BCrypt en CPU). C'est aussi ce qui permet de limiter les
 * tentatives anonymes sur {@code /api/v1/login}.
 */
@Configuration
public class SecurityConfig {

    private final DemoRateLimitFilter demoRateLimitFilter;

    public SecurityConfig(DemoRateLimitFilter demoRateLimitFilter) {
        this.demoRateLimitFilter = demoRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Desactive CSRF pour API REST (React gere le front)
                .csrf(AbstractHttpConfigurer::disable)

                // Active CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Le rate limit passe avant l'authentification standard.
                // OncePerRequestFilter de Spring + position explicite via addFilterBefore.
                .addFilterBefore(demoRateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // Configuration des acces
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Healthcheck Actuator anonyme (sondes Render + UptimeRobot)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Auth et register accessibles a tous
                        .requestMatchers("/api/v1/login", "/api/v1/register").permitAll()

                        // Fichiers statiques / images
                        .requestMatchers("/uploads/**").permitAll()

                        // Routes admin : restent en permitAll pour la demo
                        // (le AdminPastellController utilise un header X-Demo-Token pour
                        //  les operations destructives, voir DEMO_PUBLIQUE.md)
                        .requestMatchers("/api/admin/**").permitAll()

                        // Routes employe protegees
                        .requestMatchers("/api/employe/**").hasRole("EMPLOYE")

                        // Autres API publiques
                        .requestMatchers("/api/**").permitAll()

                        // UptimeRobot toutes les 5 min
                        .requestMatchers("/test").permitAll()

                        .anyRequest().authenticated()
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("https://hotel-montpellier.netlify.app");
        config.addAllowedOriginPattern("https://pastell-demo.netlify.app");
        config.addAllowedOriginPattern("https://*.netlify.app");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
