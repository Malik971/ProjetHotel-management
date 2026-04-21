package com.example.springhotel.controller;

import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminUserController {

    // Rôles autorisés pour la création côté admin (empêche un admin de créer
    // autre chose que ce qu'on a prévu)
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EMPLOYE", "ADMIN");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Users users, @RequestParam String role) {

        // Validation email
        if (users.getEmail() == null || users.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email obligatoire"));
        }
        if (users.getPassword() == null || users.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Mot de passe trop court (min 6 caractères)"));
        }

        // Fix bug critique : findByEmail retourne Optional, jamais null
        if (userRepository.existsByEmail(users.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }

        // Validation du rôle demandé
        String roleUpper = role == null ? "" : role.toUpperCase();
        if (!ALLOWED_ROLES.contains(roleUpper)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rôle invalide. Attendu : USER, EMPLOYE ou ADMIN"));
        }

        Role roleEntity = roleRepository.findByName("ROLE_" + roleUpper);
        if (roleEntity == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role not found in database"));
        }

        users.setPassword(passwordEncoder.encode(users.getPassword()));
        users.setEnabled(true);
        users.addRole(roleEntity);

        try {
            Users savedUsers = userRepository.save(users);
            return ResponseEntity.ok(savedUsers);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur lors de la création"));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur introuvable"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}