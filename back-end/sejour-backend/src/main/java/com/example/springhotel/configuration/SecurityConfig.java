package com.example.springhotel.configuration;

import com.example.springhotel.filter.DemoRateLimitFilter;
import com.example.springhotel.security.jwt.JwtAuthenticationEntryPoint;
import com.example.springhotel.security.jwt.JwtAuthenticationFilter;
import com.example.springhotel.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
 * Evolution Lot 0 : passage en mode stateless avec authentification par JWT.
 * <p>
 * Changements principaux par rapport au Lot 6 :
 *   un, ajout de sessionCreationPolicy STATELESS : Spring ne maintient plus
 *   de session HTTP cote serveur, chaque requete porte son propre token,
 *   deux, branchement de JwtAuthenticationFilter AVANT
 *   UsernamePasswordAuthenticationFilter,
 *   trois, branchement de JwtAuthenticationEntryPoint pour renvoyer du JSON
 *   sur les 401 au lieu de la page HTML par defaut,
 *   quatre, durcissement des routes admin : passage en hasRole("ADMIN") sauf
 *   pour /api/admin/pastell/status et /poll qui restent en permitAll pour le
 *   dashboard demo (controle via X-Demo-Token comme avant),
 *   cinq, /api/reservations passe en authenticated : il faut etre logue pour
 *   reserver une chambre, la reservation est automatiquement liee a
 *   l'utilisateur courant via Authentication.getName().
 * <p>
 * Ce qui ne change pas : CORS, DemoRateLimitFilter, BCryptPasswordEncoder,
 * MyUserDetailsService.
 *
 * @see com.example.springhotel.security.jwt.JwtService
 * @see com.example.springhotel.security.jwt.JwtAuthenticationFilter
 * @see com.example.springhotel.security.jwt.JwtAuthenticationEntryPoint
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final DemoRateLimitFilter demoRateLimitFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(
            DemoRateLimitFilter demoRateLimitFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) {
        this.demoRateLimitFilter = demoRateLimitFilter;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF inutile pour une API REST consommee par un front qui envoie un JWT
                .csrf(AbstractHttpConfigurer::disable)

                // CORS conserve depuis le Lot 6, durci avec origines explicites
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Sessions desactivees : authentification stateless via JWT, plus de
                // JSESSIONID, plus de session HTTP cote serveur
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reponse 401 propre en JSON quand un endpoint protege est appele sans token
                .exceptionHandling(eh ->
                        eh.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // Ordre des filtres maison :
                // un, rate limit (avant tout, pour ne pas consommer du CPU sur du spam),
                // deux, JWT (transforme le token en Authentication),
                // trois, filtre Spring standard (qui finira par appeler le controller).
                .addFilterBefore(demoRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth

                        // Pre-flight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Sondes Render et UptimeRobot
                        .requestMatchers("/actuator/health", "/actuator/info", "/test").permitAll()

                        // Login et inscription, evidemment publics
                        .requestMatchers("/api/v1/login", "/api/v1/register").permitAll()

                        // Catalogue lecture seule, consultable sans compte
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chambres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hotels/search").permitAll()

                        // Fichiers statiques uploades
                        .requestMatchers("/uploads/**").permitAll()

                        // Endpoints du dashboard demo Pastell, restent ouverts pour la demo,
                        // le controle d'acces sur le poll est porte par X-Demo-Token comme avant
                        .requestMatchers(HttpMethod.GET, "/api/admin/pastell/status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/pastell/poll").permitAll()

                        // Routes employe
                        .requestMatchers("/api/employe/**").hasRole("EMPLOYE")

                        // Routes admin : tout le reste de /api/admin/** exige ROLE_ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Reservation : il faut etre logue pour reserver
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()

                        // Espace client : mes reservations
                        .requestMatchers("/api/client/**").authenticated()

                        // /api/me : identite de l'utilisateur courant
                        .requestMatchers("/api/me").authenticated()

                        // Toute autre requete inconnue exige une authentification
                        .anyRequest().authenticated()
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
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
        // Le front a besoin de pouvoir lire le header Authorization si on l'expose
        // un jour. Pas critique aujourd'hui mais ca ne coute rien.
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}