package com.example.springhotel.configuration;

import com.example.springhotel.filter.DemoRateLimitFilter;
import com.example.springhotel.security.jwt.JwtAuthenticationEntryPoint;
import com.example.springhotel.security.jwt.JwtProperties;
import com.example.springhotel.security.jwt.JwtService;
import com.example.springhotel.security.oauth2.CompositeJwtDecoder;
import com.example.springhotel.security.oauth2.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration Spring Security de sejour-backend.
 * <p>
 * Evolution Lot K2 : migration du JWT maison vers un Resource Server OAuth2
 * en mode coexistence. Les deux types de tokens (JWT maison HS256 et tokens
 * Keycloak RS256) sont acceptes simultanement via un CompositeJwtDecoder.
 * <p>
 * Changements par rapport au Lot 0 (JWT maison pur) :
 *   un, suppression du branchement de JwtAuthenticationFilter : le filtre
 *       BearerTokenAuthenticationFilter de Spring Security le remplace. Il
 *       fait exactement la meme chose (lit le header Authorization Bearer,
 *       valide le token, pose l'Authentication) mais en delegant a un
 *       JwtDecoder pluggable plutot qu'a un filtre maison,
 *   deux, ajout de oauth2ResourceServer(jwt -> ...) avec CompositeJwtDecoder
 *       et KeycloakJwtAuthenticationConverter : le decoder dispatche par
 *       claim "iss", le converter extrait les roles depuis le claim "roles"
 *       au format ROLE_* (meme format que le JWT maison),
 *   trois, les endpoints /api/admin/pastell/status et /api/admin/pastell/poll
 *       passent de permitAll a hasAuthority("SCOPE_pastell-admin") : ils
 *       exigent desormais un token Keycloak avec le scope pastell-admin,
 *       ce qui demonstre la gestion des scopes OAuth2.
 * <p>
 * Ce qui ne change pas : CORS, DemoRateLimitFilter, BCryptPasswordEncoder,
 * JwtAuthenticationEntryPoint, toutes les regles d'autorisation metier.
 * <p>
 * JwtAuthenticationFilter est supprime (remplace par BearerTokenAuthenticationFilter).
 * JwtService est conserve (utilise par CompositeJwtDecoder pour valider les tokens maison).
 *
 * @see CompositeJwtDecoder
 * @see KeycloakJwtAuthenticationConverter
 * @see com.example.springhotel.security.jwt.JwtService
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final DemoRateLimitFilter demoRateLimitFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtService jwtService;

    /**
     * Valeur du claim "iss" dans les tokens Keycloak. Utilise uniquement
     * pour le dispatch dans CompositeJwtDecoder (comparaison de l'emetteur),
     * pas pour telecharger le JWKS au demarrage.
     * Exemple : http://localhost:8180/realms/springhotel
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakIssuerUri;

    /**
     * URL directe du JWKS Keycloak. Utilise par NimbusJwtDecoder pour
     * telecharger les cles publiques RSA sans passer par la decouverte OIDC.
     * Avantage : evite une double requete HTTP au demarrage et resout les
     * problemes de timing si Keycloak met du temps a etre pret.
     * Exemple : http://localhost:8180/realms/springhotel/protocol/openid-connect/certs
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String keycloakJwkSetUri;

    public SecurityConfig(
            DemoRateLimitFilter demoRateLimitFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtService jwtService
    ) {
        this.demoRateLimitFilter = demoRateLimitFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtService = jwtService;
    }

    /**
     * Decodeur JWT composite : lit le claim "iss" pour dispatcher vers
     * le decodeur Keycloak (RS256 via JWKS) ou le decodeur maison (HS256).
     * <p>
     * On utilise withJwkSetUri() plutot que withIssuerLocation() pour pointer
     * directement sur les cles publiques RSA sans passer par la decouverte OIDC.
     * withIssuerLocation() fait deux requetes HTTP au demarrage du bean
     * (.well-known/openid-configuration puis /certs) et peut echouer si Keycloak
     * n'est pas encore completement pret. withJwkSetUri() fait une seule requete
     * differee (au premier token recu, pas au demarrage), ce qui est plus robuste.
     */
    @Bean
    public CompositeJwtDecoder compositeJwtDecoder() {
        NimbusJwtDecoder keycloakDecoder =
                NimbusJwtDecoder.withJwkSetUri(keycloakJwkSetUri).build();
        return new CompositeJwtDecoder(keycloakDecoder, jwtService, keycloakIssuerUri);
    }

    /**
     * Convertisseur qui extrait les roles (claim "roles") et les scopes
     * (claim "scope") d'un Jwt valide pour alimenter le SecurityContext.
     */
    @Bean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF inutile pour une API REST stateless consommee par un front SPA
                .csrf(AbstractHttpConfigurer::disable)

                // CORS conserve depuis le Lot 0
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Sessions desactivees : chaque requete porte son propre token Bearer
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reponse 401 propre en JSON quand un endpoint protege est appele sans token
                .exceptionHandling(eh ->
                        eh.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // DemoRateLimitFilter conserve, avant le filtre OAuth2
                .addFilterBefore(demoRateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // Resource Server OAuth2 avec JWT :
                // BearerTokenAuthenticationFilter remplace JwtAuthenticationFilter.
                // Il lit le header Authorization Bearer, appelle compositeJwtDecoder,
                // puis keycloakJwtAuthenticationConverter pour poser l'Authentication.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(compositeJwtDecoder())
                                .jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth

                        // Pre-flight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Sondes Render et UptimeRobot
                        .requestMatchers("/actuator/health", "/actuator/info", "/test").permitAll()

                        // Login et inscription : publics (flux JWT maison conserve)
                        .requestMatchers("/api/v1/login", "/api/v1/register").permitAll()

                        // Catalogue lecture seule
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chambres/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hotels/search").permitAll()

                        // Fichiers statiques uploades
                        .requestMatchers("/uploads/**").permitAll()

                        // Endpoints Pastell : status + poll.
                        // Le scope Keycloak pastell-admin reste accepte, mais on
                        // ouvre aussi a ROLE_ADMIN et ROLE_EMPLOYE pour que le compte
                        // employe-demo puisse superviser le bus depuis le dashboard
                        // admin (lecture des compteurs + relance manuelle) sans avoir
                        // a demander le scope pastell-admin.
                        .requestMatchers(HttpMethod.GET, "/api/admin/pastell/status")
                        .hasAnyAuthority("SCOPE_pastell-admin", "ROLE_ADMIN", "ROLE_EMPLOYE")
                        .requestMatchers(HttpMethod.POST, "/api/admin/pastell/poll")
                        .hasAnyAuthority("SCOPE_pastell-admin", "ROLE_ADMIN", "ROLE_EMPLOYE")

                        // Suppressions reservees aux administrateurs : un employe ne
                        // supprime JAMAIS (ni hotel, ni chambre, ni utilisateur).
                        // Ces regles DELETE sont placees AVANT les regles d'ecriture
                        // et avant la regle generale /api/admin/** : l'ordre compte,
                        // le premier matcher qui matche gagne.
                        .requestMatchers(HttpMethod.DELETE, "/api/hotels/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/chambres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")

                        // Ecritures catalogue (creation, modification) : ADMIN ou EMPLOYE.
                        // POST /api/hotels/search reste public (matche plus haut).
                        .requestMatchers(HttpMethod.POST, "/api/hotels/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.PUT, "/api/hotels/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.POST, "/api/chambres/**").hasAnyRole("ADMIN", "EMPLOYE")
                        .requestMatchers(HttpMethod.PUT, "/api/chambres/**").hasAnyRole("ADMIN", "EMPLOYE")

                        // Reste de l'espace admin (GET, POST, PUT) : ADMIN ou EMPLOYE.
                        // Les DELETE sous /api/admin/** ont deja ete capturees ci-dessus
                        // et restent reservees a ADMIN.
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "EMPLOYE")

                        // Reservation : il faut etre logue
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()

                        // Espace client
                        .requestMatchers("/api/client/**").authenticated()

                        // Identite de l'utilisateur courant
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
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}