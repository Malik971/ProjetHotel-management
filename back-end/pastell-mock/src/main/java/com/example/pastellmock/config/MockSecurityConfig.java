package com.example.pastellmock.config;

import com.example.pastellmock.security.MockCredentialsProvider;
import com.example.pastellmock.security.RotatingPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>
 * <b>Deux modes d'authentification, choisis par configuration :</b>
 * <ol>
 *   <li><b>Mode statique (dev local, CI, defaut) :</b>
 *       {@code pastell.mock.master-secret} non defini. Les credentials sont lus en clair
 *       depuis {@code pastell.mock.username} et {@code pastell.mock.password}.
 *       PasswordEncoder : NoOp. Comportement identique au Lot 2.</li>
 *   <li><b>Mode rotatif (Lot 6, prod) :</b>
 *       {@code pastell.mock.master-secret} defini. Le username et le password sont
 *       derives par HMAC-SHA256 du secret maitre. Le password tourne automatiquement
 *       chaque jour a minuit UTC. Voir {@link MockCredentialsProvider} et
 *       {@link RotatingPasswordEncoder}. Le password d'hier reste accepte 24h.</li>
 * </ol>
 * <p>
 * Le choix est implemente via {@code @ConditionalOnProperty} sur le bean
 * {@link MockCredentialsProvider} et un {@link ObjectProvider} cote consommateurs :
 * si le provider n'est pas declare, on retombe automatiquement sur les credentials
 * statiques.
 * <p>
 * <b>Autres choix de posture (inchanges depuis le Lot 2) :</b>
 *   <ul>
 *     <li>HTTP Basic uniquement (Pastell n'accepte rien d'autre).</li>
 *     <li>Stateless : pas de session, chaque requete porte ses credentials.</li>
 *     <li>CSRF desactive : API REST.</li>
 *     <li>CORS permissif : c'est un mock, le dashboard de demo peut etre ouvert
 *         depuis n'importe quelle origine.</li>
 *     <li>{@code /api/version.php} reste anonyme (handshake / healthcheck).</li>
 *     <li>{@code /actuator/health} et {@code /actuator/info} anonymes pour
 *         permettre les sondes Render et UptimeRobot.</li>
 *   </ul>
 */
@Configuration
public class MockSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(MockSecurityConfig.class);

    @Value("${pastell.mock.username}")
    private String mockUsername;

    @Value("${pastell.mock.password}")
    private String mockPassword;

    @Value("${pastell.mock.master-secret:}")
    private String mockMasterSecret;

    @Bean
    public SecurityFilterChain mockSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/version.php").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/version.php").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
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

    /**
     * Provider de rotation, declare uniquement si {@code pastell.mock.master-secret}
     * est defini ET non vide. Cf. {@code matchIfMissing=false} : sans la propriete,
     * le bean n'est pas cree, le mode statique s'applique automatiquement.
     */
    @Bean
    @ConditionalOnProperty(name = "pastell.mock.master-secret", matchIfMissing = false)
    public MockCredentialsProvider mockCredentialsProvider() {
        log.info("Mode auth Pastell mock : ROTATIF (derivation HMAC-SHA256, rotation quotidienne UTC).");
        return new MockCredentialsProvider(mockMasterSecret);
    }

    /**
     * UserDetailsService construit a partir du provider si present, sinon
     * a partir des valeurs statiques de l'environnement.
     */
    @Bean
    public UserDetailsService mockUserDetailsService(ObjectProvider<MockCredentialsProvider> providerObjectProvider) {
        MockCredentialsProvider provider = providerObjectProvider.getIfAvailable();
        String effectiveUsername;
        String effectivePassword;

        if (provider != null) {
            effectiveUsername = provider.getUsername();
            // Champ "password" ignore par RotatingPasswordEncoder, valeur arbitraire.
            effectivePassword = "stored-value-ignored";
        } else {
            log.info("Mode auth Pastell mock : STATIQUE (username/password depuis l'environnement).");
            effectiveUsername = mockUsername;
            effectivePassword = mockPassword;
        }

        UserDetails sejourUser = User.builder()
                .username(effectiveUsername)
                .password(effectivePassword)
                .authorities("PASTELL_USER")
                .build();
        return new InMemoryUserDetailsManager(sejourUser);
    }

    /**
     * PasswordEncoder rotatif si le provider existe, NoOp sinon.
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder mockPasswordEncoder(ObjectProvider<MockCredentialsProvider> providerObjectProvider) {
        MockCredentialsProvider provider = providerObjectProvider.getIfAvailable();
        if (provider != null) {
            return new RotatingPasswordEncoder(provider);
        }
        return NoOpPasswordEncoder.getInstance();
    }
}
