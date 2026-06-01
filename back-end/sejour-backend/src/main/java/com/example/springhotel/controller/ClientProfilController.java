package com.example.springhotel.controller;

import com.example.springhotel.dto.MeResponseDTO;
import com.example.springhotel.dto.UpdateProfilRequestDTO;
import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de gestion du profil de l'utilisateur connecte.
 * <p>
 * Tous les endpoints de ce controller lisent l'identite depuis le JWT
 * (via Authentication.getName() qui contient l'email), jamais depuis
 * un parametre URL. C'est la garantie qu'un utilisateur ne peut modifier
 * que son propre profil.
 *
 * @see JwtAuthenticationFilterOldJWT
 */
@RestController
@RequestMapping("/api/client")
public class ClientProfilController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientProfilController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Modifie le profil de l'utilisateur connecte.
     * <p>
     * Regles :
     *   un, firstName et lastName sont toujours mis a jour si non vides,
     *   deux, telephone est mis a jour (peut etre null pour effacer),
     *   trois, le mot de passe n'est change que si currentPassword ET
     *          newPassword sont fournis et non vides,
     *   quatre, si currentPassword ne correspond pas au hash en base, on
     *           renvoie 400 avec un message explicite.
     *
     * @param auth    authentication Spring Security, fournie par le filtre JWT
     * @param request corps de la requete
     * @return le profil mis a jour sous forme de MeResponseDTO
     */
    @PutMapping("/profil")
    public ResponseEntity<?> updateProfil(
            Authentication auth,
            @RequestBody UpdateProfilRequestDTO request
    ) {
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .map(user -> {
                    // Mise a jour des champs texte
                    if (request.firstName() != null && !request.firstName().isBlank()) {
                        user.setFirstName(request.firstName().trim());
                    }
                    if (request.lastName() != null && !request.lastName().isBlank()) {
                        user.setLastName(request.lastName().trim());
                    }
                    // Le telephone peut etre mis a null (effacement)
                    user.setTelephone(
                            request.telephone() != null
                                    ? request.telephone().trim()
                                    : null
                    );

                    // Changement de mot de passe si demande
                    if (hasValue(request.currentPassword()) && hasValue(request.newPassword())) {
                        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                            return ResponseEntity
                                    .badRequest()
                                    .body(Map.of("error", "Mot de passe actuel incorrect"));
                        }
                        if (request.newPassword().length() < 6) {
                            return ResponseEntity
                                    .badRequest()
                                    .body(Map.of("error", "Le nouveau mot de passe doit contenir au moins 6 caracteres"));
                        }
                        user.setPassword(passwordEncoder.encode(request.newPassword()));
                    }

                    Users saved = userRepository.save(user);
                    return ResponseEntity.ok((Object) toDto(saved));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

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