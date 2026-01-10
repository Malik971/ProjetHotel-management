const API_BASE_URL = 'http://localhost:8080/api';

export const hotelSearchService = {
  searchHotels: async (searchParams) => {
    const response = await fetch(`${API_BASE_URL}/hotels/search`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(searchParams),
    });

    if (!response.ok) {
      throw new Error('Erreur lors de la recherche');
    }

    return response.json();
  },

  getHotelsByLocation: async (ville, latitude, longitude, radiusKm = 10) => {
    const params = new URLSearchParams({
      ville,
      ...(latitude && { latitude }),
      ...(longitude && { longitude }),
      radiusKm,
    });

    const response = await fetch(`${API_BASE_URL}/hotels/search/by-location?${params}`);

    if (!response.ok) {
      throw new Error('Erreur lors de la recherche par localisation');
    }

    return response.json();
  },
};