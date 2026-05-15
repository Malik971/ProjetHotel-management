package com.example.springhotel.controller;

import com.example.springhotel.dto.MeResponseDTO;
import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expose GET /api/me, point d'entree standard pour qu'un client recupere
 * les informations de l'utilisateur courant.
 * <p>
 * Pourquoi cet endpoint : apres un login reussi, le front a un token mais
 * pas forcement toutes les infos de l'utilisateur (prenom, nom, telephone)
 * pour personnaliser l'interface. Plutot que de coder le payload du JWT
 * cote client (faisable mais sale et fragile), on appelle /api/me qui
 * renvoie tout en JSON propre.
 * <p>
 * L'authentification est faite en amont par JwtAuthenticationFilter. A ce
 * stade, Authentication.getName() renvoie l'email pose dans le claim sub.
 *
 * @see com.example.springhotel.security.jwt.JwtAuthenticationFilter
 */
@RestController
@RequestMapping("/api")
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me(Authentication authentication) {

        // Garde de securite : si on est ici sans authentication, c'est qu'il y a
        // un bug de configuration. Le filtre SecurityConfig aurait du renvoyer
        // 401 avant qu'on arrive jusqu'a la methode.
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    /**
     * Conversion entite vers DTO. La logique est volontairement triviale,
     * pas besoin d'un MapperStruct pour ca.
     */
    private MeResponseDTO toDto(Users user) {
        List<String> roleNames = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(Role::getName).toList();

        return new MeResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getTelephone(),
                roleNames
        );
    }
}