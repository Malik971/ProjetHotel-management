package com.example.springhotel.controller;

import com.example.springhotel.dto.MeResponseDTO;
import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Expose GET /api/me, point d'entree standard pour qu'un client recupere
 * les informations de l'utilisateur courant.
 * <p>
 * Evolution Lot K3 : resolution de l'utilisateur en deux etapes pour
 * couvrir les deux flux d'authentification en coexistence.
 * <p>
 * Flux JWT maison : authentication.getName() renvoie l'email (claim "sub"
 * contient l'email). On cherche d'abord par keycloak_sub (null pour ce flux,
 * donc empty), puis par email (trouve). Comportement identique au Lot 0.
 * <p>
 * Flux Keycloak : authentication.getName() renvoie un UUID Keycloak (claim
 * "sub"). On cherche d'abord par keycloak_sub (trouve grace au JIT provisioning
 * effectue a la connexion). Pas de fallback necessaire.
 * <p>
 * Le JIT provisioning garantit que le profil Users existe en base avant
 * que /api/me soit appele : KeycloakUserProvisioningService cree le profil
 * lors de l'evenement AuthenticationSuccessEvent, donc avant que la requete
 * HTTP ne soit traitee par ce controller.
 *
 * @see com.example.springhotel.security.oauth2.KeycloakUserProvisioningService
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

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Users> found = resolveUser(authentication);

        return found
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    /**
     * Resout le profil utilisateur selon le type de token.
     * <p>
     * Etape 1 : si le token est un JwtAuthenticationToken (OAuth2), on extrait
     * le claim "sub" et on cherche par keycloak_sub. Couvre les tokens Keycloak.
     * <p>
     * Etape 2 : fallback par email via authentication.getName(). Couvre les
     * tokens JWT maison pour lesquels getName() renvoie l'email.
     * <p>
     * Les deux etapes sont necessaires car les tokens maison ne contiennent
     * pas de keycloak_sub en base (valeur null), donc la recherche par sub
     * ne trouve rien et on tombe naturellement sur le fallback email.
     */
    private Optional<Users> resolveUser(Authentication authentication) {

        // Etape 1 : recherche par keycloak_sub pour les tokens Keycloak
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                Optional<Users> byKeycloakSub = userRepository.findByKeycloakSub(sub);
                if (byKeycloakSub.isPresent()) {
                    return byKeycloakSub;
                }
            }
        }

        // Etape 2 : fallback par email (tokens JWT maison, ou token Keycloak
        // dont le provisioning n'a pas encore eu lieu)
        String nameOrEmail = authentication.getName();
        return userRepository.findByEmail(nameOrEmail);
    }

    /**
     * Conversion entite vers DTO.
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