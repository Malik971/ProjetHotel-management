import { useState } from 'react';

export const useDisponibilite = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const verifierDisponibilite = async (chambreId, dateArrivee, dateDepart) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/disponibilite/chambre/${chambreId}?dateArrivee=${dateArrivee}&dateDepart=${dateDepart}`
      );

      if (!response.ok) {
        throw new Error('Erreur lors de la vérification');
      }

      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  const getChambresDisponibles = async (hotelId, dateArrivee, dateDepart) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:8080/api/disponibilite/hotel/${hotelId}?dateArrivee=${dateArrivee}&dateDepart=${dateDepart}`
      );

      if (!response.ok) {
        throw new Error('Erreur lors de la récupération');
      }

      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      return [];
    } finally {
      setLoading(false);
    }
  };

  return { verifierDisponibilite, getChambresDisponibles, loading, error };
};