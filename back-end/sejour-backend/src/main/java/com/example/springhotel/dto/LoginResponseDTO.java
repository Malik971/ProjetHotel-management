package com.example.springhotel.dto;

import java.util.List;

/**
 * Contrat de reponse pour POST /api/v1/login apres l'introduction du JWT.
 * <p>
 * Pourquoi un DTO dedie : avant le lot 0, le LoginController renvoyait un
 * objet UserResponse inline qui exposait directement la structure interne
 * de l'entite Users. Apres le lot 0, on renvoie un token plus un minimum
 * d'infos pour que le front puisse afficher la navbar sans appeler /api/me
 * immediatement. Le contrat est explicite, le code est lisible.
 * <p>
 * Le password hash de l'utilisateur n'est evidemment jamais expose ici.
 *
 * @param token token JWT signe, a stocker par le client et a renvoyer en
 *              header Authorization Bearer sur les appels suivants
 * @param email email de l'utilisateur connecte
 * @param roles liste des roles applicatifs, par exemple ROLE_USER ou ROLE_ADMIN
 */
public record LoginResponseDTO(
        String token,
        String email,
        List<String> roles
) {
}