const API_URL = import.meta.env.VITE_API_URL + '/api';

export const reservationService = {
  /**
   * Créer une nouvelle réservation
   */
  async creerReservation(data) {
    console.log('📤 reservationService.creerReservation - data:', data);

    try {
      const response = await fetch(`${API_URL}/reservations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data)
      });

      console.log('📥 Response status:', response.status);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Erreur API:', errorText);
        throw new Error(`Erreur ${response.status}: ${errorText}`);
      }

      const result = await response.json();
      console.log('✅ Réservation créée:', result);
      return result;
    } catch (error) {
      console.error('❌ Erreur lors de la création de la réservation:', error);
      throw error;
    }
  },

  /**
   * Récupérer les réservations d'un utilisateur
   */
  async getMesReservations(userId) {
    try {
      const response = await fetch(`${API_URL}/reservations/user/${userId}`);

      if (!response.ok) {
        throw new Error(`Erreur ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('❌ Erreur lors de la récupération des réservations:', error);
      throw error;
    }
  },

  /**
   * Annuler une réservation
   */
  async annulerReservation(reservationId) {
    try {
      const response = await fetch(`${API_URL}/reservations/${reservationId}/annuler`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) {
        throw new Error(`Erreur ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('❌ Erreur lors de l\'annulation:', error);
      throw error;
    }
  }
};