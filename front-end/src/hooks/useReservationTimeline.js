// src/hooks/useReservationTimeline.js
import { useState, useEffect, useRef } from "react";
import { httpClient } from "../api/httpClient";

/**
 * Hook React qui charge et rafraichit periodiquement la timeline d'une
 * reservation.
 *
 * Pourquoi le polling : la timeline reflete l'etat Pastell qui evolue en
 * fond (validation par un agent, confirmation, terminaison). Plutot que
 * d'imposer a l'utilisateur de recharger la page, on rafraichit en
 * arriere-plan toutes les 30 secondes.
 *
 * Nettoyage automatique : si le composant est demonte (navigation), le
 * timer est arrete pour eviter les fuites memoire et les appels orphelins.
 *
 * @param {number|string} reservationId id de la reservation a suivre
 * @param {number} pollIntervalMs intervalle de polling, defaut 30s
 * @returns {{ timeline, loading, error, refresh }}
 */
export function useReservationTimeline(reservationId, pollIntervalMs = 30000) {
    const [timeline, setTimeline] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Ref pour eviter les memory leaks si on demonte pendant un fetch en cours
    const isMounted = useRef(true);

    const fetchTimeline = async () => {
        if (!reservationId) return;
        try {
            const { data } = await httpClient.get(
                `/api/client/reservations/${reservationId}/timeline`
            );
            if (isMounted.current) {
                setTimeline(data);
                setError(null);
            }
        } catch (err) {
            if (isMounted.current) {
                setError(
                    err.response?.status === 404
                        ? "Reservation introuvable"
                        : err.response?.status === 403
                        ? "Vous n'avez pas acces a cette reservation"
                        : "Erreur lors du chargement du suivi"
                );
            }
        } finally {
            if (isMounted.current) {
                setLoading(false);
            }
        }
    };

    useEffect(() => {
        isMounted.current = true;
        setLoading(true);

        // Premier chargement immediat
        fetchTimeline();

        // Polling regulier
        const intervalId = setInterval(fetchTimeline, pollIntervalMs);

        return () => {
            isMounted.current = false;
            clearInterval(intervalId);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [reservationId, pollIntervalMs]);

    return {
        timeline,
        loading,
        error,
        refresh: fetchTimeline,
    };
}