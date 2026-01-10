import React, { useState } from 'react';
import { reservationService } from '../../services/reservationService';

export default function ReservationModal({ chambre, onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    dateDebut: '',
    dateFin: '',
    nomClient: '',
    emailClient: '',
    telephoneClient: '',
    nombrePersonnes: 1
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  console.log('🎯 ReservationModal - chambre:', chambre);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    console.log('📤 Envoi de la réservation:', {
      chambreId: chambre.id,
      ...formData
    });

    try {
      const response = await reservationService.creerReservation({
        chambreId: chambre.id,
        ...formData
      });

      console.log('✅ Réservation créée:', response);
      alert(`✅ Réservation confirmée !\nCode: ${response.codeConfirmation}`);

      if (onSuccess) onSuccess(response);
      onClose();
    } catch (err) {
      console.error('❌ Erreur réservation:', err);
      setError(err.message || 'Erreur lors de la réservation');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-6 rounded-t-lg">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-2xl font-bold mb-2">Réserver une chambre</h2>
              <p className="text-blue-100">{chambre.nom}</p>
            </div>
            <button
              onClick={onClose}
              className="text-white hover:text-gray-200 text-3xl leading-none"
            >
              ×
            </button>
          </div>
        </div>

        {/* Formulaire */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="bg-red-50 text-red-800 p-4 rounded-lg">
              ❌ {error}
            </div>
          )}

          {/* Dates */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Date d'arrivée *
              </label>
              <input
                type="date"
                name="dateDebut"
                value={formData.dateDebut}
                onChange={handleChange}
                min={new Date().toISOString().split('T')[0]}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Date de départ *
              </label>
              <input
                type="date"
                name="dateFin"
                value={formData.dateFin}
                onChange={handleChange}
                min={formData.dateDebut || new Date().toISOString().split('T')[0]}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Informations client */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nom complet *
            </label>
            <input
              type="text"
              name="nomClient"
              value={formData.nomClient}
              onChange={handleChange}
              required
              placeholder="Jean Dupont"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email *
            </label>
            <input
              type="email"
              name="emailClient"
              value={formData.emailClient}
              onChange={handleChange}
              required
              placeholder="jean.dupont@example.com"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Téléphone *
            </label>
            <input
              type="tel"
              name="telephoneClient"
              value={formData.telephoneClient}
              onChange={handleChange}
              required
              placeholder="06 12 34 56 78"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Nombre de personnes *
            </label>
            <input
              type="number"
              name="nombrePersonnes"
              value={formData.nombrePersonnes}
              onChange={handleChange}
              min="1"
              max={chambre.capacite || 10}
              required
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Prix estimé */}
          {formData.dateDebut && formData.dateFin && (
            <div className="bg-blue-50 p-4 rounded-lg">
              <p className="text-sm text-gray-600">Prix estimé</p>
              <p className="text-2xl font-bold text-blue-600">
                {(() => {
                  const debut = new Date(formData.dateDebut);
                  const fin = new Date(formData.dateFin);
                  const nuits = Math.ceil((fin - debut) / (1000 * 60 * 60 * 24));
                  const total = nuits * (chambre.prixParNuit || 0);
                  return `${total.toFixed(2)}€`;
                })()}
              </p>
              <p className="text-xs text-gray-500">
                {(() => {
                  const debut = new Date(formData.dateDebut);
                  const fin = new Date(formData.dateFin);
                  const nuits = Math.ceil((fin - debut) / (1000 * 60 * 60 * 24));
                  return `${nuits} nuit${nuits > 1 ? 's' : ''} × ${chambre.prixParNuit}€`;
                })()}
              </p>
            </div>
          )}

          {/* Boutons */}
          <div className="flex gap-4 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-semibold transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-semibold transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {loading ? 'Réservation...' : 'Confirmer la réservation'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}