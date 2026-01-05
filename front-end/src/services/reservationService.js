import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/reservations';

const reservationService = {
  // Créer une réservation
  creerReservation: async (reservation) => {
    const response = await axios.post(API_BASE_URL, reservation);
    return response.data;
  },

  // Récupérer toutes les réservations
  getAllReservations: async () => {
    const response = await axios.get(API_BASE_URL);
    return response.data;
  },

  // Récupérer une réservation par ID
  getReservationById: async (id) => {
    const response = await axios.get(`${API_BASE_URL}/${id}`);
    return response.data;
  },

  // Récupérer les réservations d'une chambre
  getReservationsByChambre: async (chambreId) => {
    const response = await axios.get(`${API_BASE_URL}/chambre/${chambreId}`);
    return response.data;
  },

  // Récupérer les réservations d'un client
  getReservationsByClient: async (email) => {
    const response = await axios.get(`${API_BASE_URL}/client/${email}`);
    return response.data;
  },

  // Vérifier la disponibilité
  verifierDisponibilite: async (chambreId, reservation) => {
    const response = await axios.post(
      `${API_BASE_URL}/verifier-disponibilite/${chambreId}`,
      reservation
    );
    return response.data;
  },

  // Modifier le statut
  updateStatut: async (id, statut) => {
    const response = await axios.patch(
      `${API_BASE_URL}/${id}/statut`,
      null,
      { params: { statut } }
    );
    return response.data;
  },

  // Annuler une réservation
  deleteReservation: async (id) => {
    await axios.delete(`${API_BASE_URL}/${id}`);
  }
};

export default reservationService;