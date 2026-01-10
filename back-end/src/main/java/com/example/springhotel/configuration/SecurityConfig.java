package com.example.springhotel.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ❌ Désactive CSRF pour API REST (React gère le front)
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Active CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ Configuration des accès
                .authorizeHttpRequests(auth -> auth
                        // Préflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth & register accessibles à tous
                        .requestMatchers("/api/v1/login", "/api/v1/register").permitAll()

                        // Fichiers statiques / images
                        .requestMatchers("/uploads/**").permitAll()

                        // Routes admin protégées
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Routes employé protégées
                        .requestMatchers("/api/employe/**").hasRole("EMPLOYE")

                        // Autres API publiques
                        .requestMatchers("/api/**").permitAll()

                        // Tout le reste nécessite authentification
                        .anyRequest().authenticated()
                )

                // ❌ Pas de login HTML (React gère le front)
                .formLogin(AbstractHttpConfigurer::disable)

                // ❌ Pas de logout HTML
                .logout(AbstractHttpConfigurer::disable)

                .build();
    }

    // ✅ Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ Configuration CORS globale pour React
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // Autorise React sur localhost:5173 ou tout autre port
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
