package com.example.springhotel.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination generique exposee par les endpoints admin.
 * <p>
 * Pourquoi ce wrapper plutot qu'un {@link Page} direct :
 *   - Spring Data serialise {@link org.springframework.data.domain.PageImpl}
 *     d'une facon qui peut evoluer entre versions de Spring Boot (un avertissement
 *     de depreciation existe depuis Spring Boot 3.3 autour de cette serialisation).
 *   - Avec ce DTO, le contrat JSON est fige cote API : peu importe ce que Spring
 *     fait en interne, le front recoit toujours les memes champs aux memes noms.
 *   - Le code de mapping est centralise dans {@link #from(Page)}, donc tous les
 *     endpoints paginees partagent la meme forme de reponse.
 * <p>
 * Forme JSON renvoyee :
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 137,
 *   "totalPages": 7,
 *   "first": true,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> type des elements contenus dans la page
 */
public record PagedResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Construit un {@link PagedResponseDTO} a partir d'une {@link Page} Spring Data.
     * Conversion sans copie superflue : on reprend tels quels le contenu et les
     * metadonnees fournies par Spring Data.
     *
     * @param page page Spring Data deja paginee
     * @param <T>  type des elements
     * @return le wrapper pret a etre serialise en JSON
     */
    public static <T> PagedResponseDTO<T> from(Page<T> page) {
        return new PagedResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}