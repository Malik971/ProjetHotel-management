import { useState, useCallback } from 'react';

export const useHotelSearch = () => {
  const [hotels, setHotels] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const searchHotels = useCallback(async (searchParams) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('http://localhost:8080/api/hotels/search', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(searchParams),
      });

      if (!response.ok) {
        throw new Error('Erreur lors de la recherche');
      }

      const data = await response.json();
      setHotels(data);
    } catch (err) {
      setError(err.message);
      console.error('Erreur de recherche:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const getAllHotels = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('http://localhost:8080/api/hotels/all');

      if (!response.ok) {
        throw new Error('Erreur lors du chargement');
      }

      const data = await response.json();
      setHotels(data);
    } catch (err) {
      setError(err.message);
      console.error('Erreur:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const resetSearch = useCallback(() => {
    setHotels([]);
    setError(null);
  }, []);

  return { hotels, loading, error, searchHotels, getAllHotels, resetSearch };
};