package com.example.springhotel.controller;

import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173")
public class RegistrationLoginController {

    // Rôle forcé pour l'inscription publique — on ne laisse JAMAIS le client choisir
    private static final String DEFAULT_PUBLIC_ROLE = "ROLE_USER";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationLoginController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {

        // Validation
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Requête invalide"));
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email obligatoire"));
        }

        String email = request.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format d'email invalide"));
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Mot de passe trop court (min 6 caractères)"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email déjà utilisé"));
        }

        // Récupération du rôle par défaut - on force ROLE_USER, pas de choix client
        Role userRole = roleRepository.findByName(DEFAULT_PUBLIC_ROLE);
        if (userRole == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", DEFAULT_PUBLIC_ROLE + " manquant en base"));
        }

        // Création utilisateur
        Users users = new Users();
        users.setFirstName(sanitize(request.getFirstName()));
        users.setLastName(sanitize(request.getLastName()));
        users.setEmail(email);
        users.setTelephone(sanitize(request.getTelephone()));
        users.setPassword(passwordEncoder.encode(request.getPassword()));
        users.setEnabled(true);
        users.setRoles(List.of(userRole));

        try {
            userRepository.save(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création du compte"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Utilisateur créé avec succès",
                        "id", users.getId(),
                        "email", users.getEmail()
                ));
    }

    // Évite les NullPointerException et trim les espaces
    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }

    // DTO complet
    public static class RegistrationRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String telephone;
        private String password;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}