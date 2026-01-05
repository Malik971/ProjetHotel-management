import { useState, useEffect } from 'react';
import chambreService from '../services/chambreService';

export const useChambres = (hotelId = null) => {
  const [chambres, setChambres] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (hotelId) {
      fetchChambres();
    } else {
      setLoading(false);
    }
  }, [hotelId]);

  const fetchChambres = async () => {
    try {
      setLoading(true);
      setError(null);

      const data = hotelId
        ? await chambreService.getChambresByHotel(hotelId)
        : await chambreService.getAllChambres();

      setChambres(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors du chargement des chambres');
      setChambres([]);
    } finally {
      setLoading(false);
    }
  };

  const creerChambre = async (chambre) => {
    try {
      const nouvelle = await chambreService.creerChambre(chambre);
      setChambres([...chambres, nouvelle]);
      return { success: true, data: nouvelle };
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.message || 'Erreur lors de la création'
      };
    }
  };

  const updateChambre = async (id, chambre) => {
    try {
      const updated = await chambreService.updateChambre(id, chambre);
      setChambres(chambres.map(c => c.id === id ? updated : c));
      return { success: true, data: updated };
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.message || 'Erreur lors de la modification'
      };
    }
  };

  const deleteChambre = async (id) => {
    try {
      await chambreService.deleteChambre(id);
      setChambres(chambres.filter(c => c.id !== id));
      return { success: true };
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.message || 'Erreur lors de la suppression'
      };
    }
  };

  return {
    chambres,
    loading,
    error,
    fetchChambres,
    creerChambre,
    updateChambre,
    deleteChambre
  };
};