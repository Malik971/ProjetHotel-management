import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/chambres';

const chambreService = {
  // Récupérer toutes les chambres
  getAllChambres: async () => {
    const response = await axios.get(API_BASE_URL);
    return response.data;
  },

  // Récupérer une chambre par ID
  getChambreById: async (id) => {
    const response = await axios.get(`${API_BASE_URL}/${id}`);
    return response.data;
  },

  // Récupérer les chambres d'un hôtel
  getChambresByHotel: async (hotelId) => {
    const response = await axios.get(`${API_BASE_URL}/hotel/${hotelId}`);
    return response.data;
  },

  // Créer une chambre
  creerChambre: async (chambre) => {
    const response = await axios.post(API_BASE_URL, chambre);
    return response.data;
  },

  // Modifier une chambre
  updateChambre: async (id, chambre) => {
    const response = await axios.put(`${API_BASE_URL}/${id}`, chambre);
    return response.data;
  },

  // Supprimer une chambre
  deleteChambre: async (id) => {
    await axios.delete(`${API_BASE_URL}/${id}`);
  },

  // Vérifier la disponibilité
  getChambresDisponibles: async (dateDebut, dateFin, hotelId = null) => {
    const params = { dateDebut, dateFin };
    if (hotelId) params.hotelId = hotelId;

    const response = await axios.get(`${API_BASE_URL}/disponibles`, { params });
    return response.data;
  }
};

export default chambreService;