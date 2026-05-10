package com.example.springhotel.integration.pastell.repository;

import com.example.springhotel.integration.pastell.entity.PastellPollingCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data pour le curseur de polling Pastell.
 *<p>
 * Le curseur est mono-ligne et identifie en base par PK = 1
 * (cf. contrainte CHECK dans la migration V4). On expose une methode
 * {@link #findCursor()} qui materialise cette convention "il y a toujours
 * une ligne unique" en lecture sucree, plus parlante que findById(1L).
 *<p>
 * Aucune methode delete : par contrat, la ligne ne doit JAMAIS etre supprimee.
 * Si on voulait "reinitialiser" le curseur, on ferait UPDATE
 * SET last_processed_id_j = 0, ce qui est une operation de service, pas
 * de repository (et qui n'a pas de cas d'usage prevu pour le moment).
 */
@Repository
public interface PastellPollingCursorRepository extends JpaRepository<PastellPollingCursor, Long> {

    /**
     * Identifiant fige du curseur unique. Constante exposee ici pour eviter
     * que les services manipulent la magic value "1L" en dur.
     */
    Long SINGLETON_ID = 1L;

    /**
     * Retourne le curseur unique (PK = 1).
     *<p>
     * En prod : la ligne est garantie presente par la migration V4, donc
     * l'Optional sera toujours peuple. Le service appelant gere quand meme
     * le cas isEmpty pour les contextes de test ou pour les premiers boots
     * sous H2 (Flyway desactive en profil "test").
     */
    default Optional<PastellPollingCursor> findCursor() {
        return findById(SINGLETON_ID);
    }
}