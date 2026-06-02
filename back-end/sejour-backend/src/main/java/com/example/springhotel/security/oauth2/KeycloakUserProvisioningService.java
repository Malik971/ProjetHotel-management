package com.example.springhotel.security.oauth2;

import com.example.springhotel.entity.Role;
import com.example.springhotel.entity.Users;
import com.example.springhotel.repository.RoleRepository;
import com.example.springhotel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provisionnement JIT (Just-In-Time) des utilisateurs Keycloak.
 * <p>
 * Pourquoi ce service existe :
 * Keycloak gere les identites, sejour-backend gere les reservations.
 * Les deux bases de donnees sont separees. Quand un utilisateur Keycloak
 * fait une reservation, le backend a besoin d'un enregistrement Users
 * local pour rattacher la reservation a un user_id (cle etrangere en base).
 * Ce service cree ce profil local automatiquement a la premiere connexion,
 * sans que l'utilisateur ait a s'inscrire une deuxieme fois.
 * <p>
 * Declenchement :
 * Spring Security publie un AuthenticationSuccessEvent apres chaque
 * authentification reussie (token valide pose dans le SecurityContext).
 * Ce listener ne s'active que si l'Authentication est un JwtAuthenticationToken
 * et que le token contient un claim "iss" pointant sur Keycloak. Les tokens
 * JWT maison ne declenchent pas le provisioning.
 * <p>
 * Idempotence :
 * Le service cherche d'abord un profil existant par keycloak_sub. S'il en
 * trouve un, il ne fait rien. Le provisioning n'a lieu qu'une seule fois,
 * lors de la toute premiere connexion Keycloak de cet utilisateur.
 * <p>
 * Profil cree :
 *   keycloak_sub : claim "sub" du token (UUID Keycloak, stable)
 *   email        : claim "email" si present, sinon sub@keycloak.local
 *   firstName    : claim "given_name" si present, sinon "Utilisateur"
 *   lastName     : claim "family_name" si present, sinon "Keycloak"
 *   password     : chaine vide encodee BCrypt (inutile pour ce flux)
 *   roles        : ROLE_USER par defaut
 *   enabled      : true
 *
 * @see com.example.springhotel.security.oauth2.CompositeJwtDecoder
 * @see com.example.springhotel.security.oauth2.KeycloakJwtAuthenticationConverter
 */
@Service
public class KeycloakUserProvisioningService {

    private static final Logger log =
            LoggerFactory.getLogger(KeycloakUserProvisioningService.class);

    /** Valeur de l'issuer Keycloak attendu dans le claim "iss". */
    private static final String CLAIM_ISSUER = "iss";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public KeycloakUserProvisioningService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ecoute les evenements d'authentification reussie et provisionne un profil
     * local si l'utilisateur vient de Keycloak et n'a pas encore de profil.
     * <p>
     * Le listener est transactionnel : si la creation du profil echoue (ex :
     * contrainte UNIQUE sur l'email), l'exception remonte mais le token reste
     * valide. L'utilisateur peut naviguer sur les endpoints publics mais ne peut
     * pas creer de reservation sans profil local. Ce cas est gere cote frontend
     * par une redirection vers une page de completion de profil (Lot K4).
     *
     * @param event evenement publie par Spring Security apres validation du token
     */
    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {

        // On ne traite que les tokens JWT (maison ou Keycloak)
        if (!(event.getAuthentication() instanceof JwtAuthenticationToken jwtAuth)) {
            return;
        }

        Jwt jwt = (Jwt) jwtAuth.getPrincipal();

        // On ne provisionne que les tokens emis par Keycloak
        // Les tokens JWT maison n'ont pas de claim "iss" ou ont un issuer different
        String issuer = jwt.getClaimAsString(CLAIM_ISSUER);
        if (issuer == null || !issuer.contains("/realms/")) {
            return;
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            log.warn("Token Keycloak sans claim sub, provisioning ignore.");
            return;
        }

        // Idempotence : si le profil existe deja, on ne fait rien
        if (userRepository.findByKeycloakSub(sub).isPresent()) {
            log.debug("Profil Keycloak deja present pour sub={}, provisioning ignore.", sub);
            return;
        }

        // Creation du profil local
        Users user = buildUserFromToken(jwt, sub);
        userRepository.save(user);

        log.info("Profil Keycloak provisionne : sub={}, email={}.", sub, user.getEmail());
    }

    /**
     * Construit un profil Users a partir des claims du token Keycloak.
     * <p>
     * Les claims "email", "given_name" et "family_name" sont optionnels dans
     * OpenID Connect. On fournit des valeurs par defaut raisonnables si absents.
     * L'email de fallback (sub@keycloak.local) est fictif mais unique en base
     * grace a l'unicite du sub UUID.
     */
    private Users buildUserFromToken(Jwt jwt, String sub) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = sub + "@keycloak.local";
        }

        String firstName = jwt.getClaimAsString("given_name");
        if (firstName == null || firstName.isBlank()) {
            firstName = "Utilisateur";
        }

        String lastName = jwt.getClaimAsString("family_name");
        if (lastName == null || lastName.isBlank()) {
            lastName = "Keycloak";
        }

        // Si l'email existe deja en base (compte maison avec le meme email),
        // on suffixe avec un UUID pour eviter la violation de contrainte UNIQUE.
        // Cas rare mais possible : un utilisateur avait un compte maison avant
        // de se connecter via Keycloak avec la meme adresse email.
        if (userRepository.existsByEmail(email)) {
            log.warn("Email {} deja utilise en base, suffixe UUID applique pour sub={}.",
                    email, sub);
            email = sub + "+" + UUID.randomUUID().toString().substring(0, 8) + "@keycloak.local";
        }

        Role roleUser = roleRepository.findByName("ROLE_USER");

        Users user = new Users();
        user.setKeycloakSub(sub);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        // Mot de passe inutile pour ce flux : l'utilisateur se connecte via Keycloak.
        // On encode une chaine vide pour satisfaire la contrainte NOT NULL en base.
        user.setPassword(passwordEncoder.encode(""));
        user.setEnabled(true);
        user.setRoles(List.of(roleUser));

        return user;
    }
}