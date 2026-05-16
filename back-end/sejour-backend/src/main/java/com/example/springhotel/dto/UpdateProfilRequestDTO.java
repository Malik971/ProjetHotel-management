package com.example.springhotel.dto;

/**
 * Payload de la requete PUT /api/client/profil.
 * <p>
 * currentPassword et newPassword sont optionnels : si absents ou vides,
 * le mot de passe n'est pas modifie. Si presents, currentPassword est
 * verifie contre le hash en base avant d'accepter le changement.
 * <p>
 * L'email n'est pas modifiable ici : il sert d'identifiant de connexion
 * et un changement d'email necessite une verification par email (hors scope).
 */
public record UpdateProfilRequestDTO(
        String firstName,
        String lastName,
        String telephone,
        String currentPassword,
        String newPassword
) {
}