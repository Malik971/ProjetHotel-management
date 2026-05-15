package com.example.springhotel.dto;

import java.util.List;

/**
 * Contrat de reponse pour GET /api/me.
 * <p>
 * Renvoie les informations de l'utilisateur courant a partir du token JWT
 * deja valide par le filtre. Pratique pour le front qui veut afficher le
 * prenom dans la navbar, ou les roles pour adapter le menu, sans avoir a
 * decoder le token lui-meme cote client.
 *
 * @param id        id en base de l'utilisateur
 * @param email     email de connexion
 * @param firstName prenom
 * @param lastName  nom de famille
 * @param telephone numero de telephone, peut etre null
 * @param roles     roles applicatifs
 */
public record MeResponseDTO(
        Long id,
        String email,
        String firstName,
        String lastName,
        String telephone,
        List<String> roles
) {
}