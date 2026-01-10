import { useState } from 'react';
import reservationService from '../services/reservationService';

export const useReservations = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const creerReservation = async (reservation) => {
    try {
      setLoading(true);
      setError(null);
      const data = await reservationService.creerReservation(reservation);
      return { success: true, data };
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Erreur lors de la réservation';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  const verifierDisponibilite = async (chambreId, reservation) => {
    try {
      setLoading(true);
      setError(null);
      const disponible = await reservationService.verifierDisponibilite(chambreId, reservation);
      return { success: true, disponible };
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Erreur lors de la vérification';
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    creerReservation,
    verifierDisponibilite
  };
};