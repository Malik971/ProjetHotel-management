package com.example.springhotel.security.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Convertisseur qui transforme un Jwt valide en Authentication Spring Security.
 * <p>
 * Pourquoi ce convertisseur est necessaire :
 * Le convertisseur par defaut de Spring Security lit les scopes OAuth2 (claim
 * "scp" ou "scope") et les prefixe avec "SCOPE_". Il ne lit pas le claim
 * "roles" au format ROLE_*. Or nos deux types de tokens (maison et Keycloak)
 * publient tous les deux leurs roles dans le claim "roles" sous la forme
 * ["ROLE_ADMIN", "ROLE_USER"]. Ce convertisseur unifie la lecture des deux.
 * <p>
 * Sources d'autorite extraites :
 *   un, le claim "roles" (liste de chaines) : produit des ROLE_* authorities,
 *       couvre a la fois les tokens JWT maison et les tokens Keycloak (grace
 *       au ProtocolMapper "roles-claim" configure dans realm-export.json),
 *   deux, le claim "scope" (chaine separee par des espaces) : produit des
 *       SCOPE_* authorities, couvre les scopes OAuth2 Keycloak dont
 *       "SCOPE_pastell-admin" utilise pour proteger les endpoints Pastell.
 * <p>
 * Resultat : les regles hasRole("ADMIN") et hasAuthority("SCOPE_pastell-admin")
 * dans SecurityConfig fonctionnent sans aucune modification des controllers.
 *
 * @see com.example.springhotel.configuration.SecurityConfig
 */
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log =
            LoggerFactory.getLogger(KeycloakJwtAuthenticationConverter.class);

    /** Nom du claim qui porte les roles applicatifs dans les deux types de tokens. */
    private static final String CLAIM_ROLES = "roles";

    /** Nom du claim qui porte les scopes OAuth2 dans les tokens Keycloak. */
    private static final String CLAIM_SCOPE = "scope";

    /**
     * Convertit un Jwt en JwtAuthenticationToken avec les autorites extraites.
     * <p>
     * Authentication.getName() renverra le claim "sub" du token, soit l'email
     * pour les tokens maison, soit le sub UUID Keycloak pour les tokens OIDC.
     * Le Lot K3 (JIT provisioning) s'appuiera sur ce sub pour retrouver ou
     * creer le profil utilisateur en base.
     *
     * @param jwt le token valide produit par CompositeJwtDecoder
     * @return un JwtAuthenticationToken pret a etre pose dans le SecurityContext
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        log.debug("Token converti : sub={}, autorites={}", jwt.getSubject(), authorities);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /**
     * Extrait et fusionne les autorites issues des claims "roles" et "scope".
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(extractRolesAuthorities(jwt));
        authorities.addAll(extractScopeAuthorities(jwt));
        return authorities;
    }

    /**
     * Lit le claim "roles" et produit des SimpleGrantedAuthority.
     * <p>
     * Les valeurs sont utilisees telles quelles : si le claim contient
     * "ROLE_ADMIN", l'autorite produite est "ROLE_ADMIN", ce qui est
     * exactement ce qu'attend hasRole("ADMIN") dans Spring Security
     * (Spring prefixe ROLE_ dans hasRole, donc "ADMIN" suffit).
     */
    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> extractRolesAuthorities(Jwt jwt) {
        Object raw = jwt.getClaim(CLAIM_ROLES);
        if (!(raw instanceof List)) {
            return List.of();
        }
        List<String> roles = (List<String>) raw;
        return roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    /**
     * Lit le claim "scope" (chaine separee par des espaces) et produit des
     * autorites prefixees SCOPE_.
     * <p>
     * Exemple : scope="openid profile pastell-admin" produit :
     *   SCOPE_openid, SCOPE_profile, SCOPE_pastell-admin.
     * La regle hasAuthority("SCOPE_pastell-admin") dans SecurityConfig
     * s'appuie sur cette autorite.
     */
    private List<GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        String scopeClaim = jwt.getClaimAsString(CLAIM_SCOPE);
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(scopeClaim.split(" "))
                .filter(s -> !s.isBlank())
                .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
                .toList();
    }
}