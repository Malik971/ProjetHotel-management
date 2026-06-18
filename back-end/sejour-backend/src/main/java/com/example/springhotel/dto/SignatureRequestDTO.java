package com.example.springhotel.dto;

/**
 * Corps de la requete POST /api/admin/reservations/{id}/signer.
 *
 * signatureBase64 : image PNG du trace canvas, encode en base64 (format
 *   data:image/png;base64,... ou base64 brut, les deux sont acceptes).
 *
 * nomSignataire : nom et prenom de l'agent tel que saisi dans le formulaire.
 *   Immutable une fois persiste : il fait partie du recepisse PDF.
 *
 * Point de migration niveau 3 : au niveau 3, ce DTO est envoye tel quel au
 * mock parapheur via REST, sans modification. Le mock retourne un PDF signe
 * et un identifiant de transaction qui remplacent la generation locale.
 */
public record SignatureRequestDTO(
        String signatureBase64,
        String nomSignataire
) {}