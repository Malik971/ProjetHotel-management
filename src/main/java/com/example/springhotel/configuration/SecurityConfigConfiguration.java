package com.example.springhotel.configuration;

import com.example.springhotel.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfigConfiguration {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfigConfiguration(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                // Autorisation des URL publiques
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",        // Page login HTML
                                "/register",     // Page register HTML
                                "/api/v1/register",  // API register
                                "/api/v1/login"      // API login (si tu l’ajoutes)
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Formulaire de login personnalisé
                .formLogin(form -> form
                        .loginPage("/login")               // URL GET de la page login
                        .loginProcessingUrl("/login")       // URL POST du form
                        .defaultSuccessUrl("/home", true)   // Page après connexion réussie
                        .permitAll()
                )

                // Gestion du logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       PasswordEncoder passwordEncoder)
            throws Exception {

        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        builder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        return builder.build();
    }
}


