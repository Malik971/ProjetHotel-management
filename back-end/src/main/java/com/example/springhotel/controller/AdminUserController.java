package com.example.springhotel.controller;

import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.User;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Pour debug React
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Récupérer tous les utilisateurs
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Ajouter un utilisateur
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user, @RequestParam String role) {

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        // Chercher le rôle en base
        Role roleEntity = roleRepository.findByName("ROLE_" + role.toUpperCase());
        if (roleEntity == null) {
            return ResponseEntity.badRequest().body("Role not found");
        }
        user.addRole(roleEntity);

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    // Supprimer un utilisateur
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
