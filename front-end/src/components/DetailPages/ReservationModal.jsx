import React, { useState } from 'react';
import { useReservations } from '../../hooks/useReservations';

export default function ReservationModal({ chambre, onClose, onSuccess }) {
  const { creerReservation, verifierDisponibilite, loading, error } = useReservations();

  const [formData, setFormData] = useState({
    dateDebut: '',
    dateFin: '',
    nomClient: '',
    emailClient: '',
    telephoneClient: '',
    nombrePersonnes: 1
  });

  const [disponibilite, setDisponibilite] = useState(null);
  const [prixTotal, setPrixTotal] = useState(0);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    setDisponibilite(null); // Reset disponibilité quand on change les dates
  };

  // Calculer le nombre de nuits et le prix
  const calculerPrix = () => {
    if (formData.dateDebut && formData.dateFin) {
      const debut = new Date(formData.dateDebut);
      const fin = new Date(formData.dateFin);
      const nuits = Math.ceil((fin - debut) / (1000 * 60 * 60 * 24));

      if (nuits > 0) {
        const prix = chambre.prixParNuit * nuits;
        setPrixTotal(prix);
        return nuits;
      }
    }
    return 0;
  };

  // Vérifier la disponibilité
  const handleVerifierDisponibilite = async () => {
    const nuits = calculerPrix();
    if (nuits <= 0) {
      alert('Veuillez sélectionner des dates valides');
      return;
    }

    const result = await verifierDisponibilite(chambre.id, {
      dateDebut: formData.dateDebut,
      dateFin: formData.dateFin,
      chambreId: chambre.id
    });

    if (result.success) {
      setDisponibilite(result.disponible);
    }
  };

  // Créer la réservation
  const handleReserver = async (e) => {
    e.preventDefault();

    if (!disponibilite) {
      alert('Veuillez d\'abord vérifier la disponibilité');
      return;
    }

    const reservation = {
      ...formData,
      chambreId: chambre.id,
      prixTotal,
      statut: 'EN_ATTENTE'
    };

    const result = await creerReservation(reservation);

    if (result.success) {
      alert('Réservation effectuée avec succès !');
      onSuccess?.();
      onClose();
    }
  };

  const nombreNuits = calculerPrix();

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex justify-between items-start mb-6">
            <div>
              <h2 className="text-2xl font-bold">{chambre.nom}</h2>
              <p className="text-gray-600">{chambre.hotelNom}</p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 text-2xl"
            >
              ×
            </button>
          </div>

          {/* Formulaire */}
          <form onSubmit={handleReserver} className="space-y-4">
            {/* Dates */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  Date d'arrivée
                </label>
                <input
                  type="date"
                  name="dateDebut"
                  value={formData.dateDebut}
                  onChange={handleChange}
                  min={new Date().toISOString().split('T')[0]}
                  required
                  className="w-full border rounded-lg px-3 py-2"
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  Date de départ
                </label>
                <input
                  type="date"
                  name="dateFin"
                  value={formData.dateFin}
                  onChange={handleChange}
                  min={formData.dateDebut || new Date().toISOString().split('T')[0]}
                  required
                  className="w-full border rounded-lg px-3 py-2"
                />
              </div>
            </div>

            {/* Nombre de personnes */}
            <div>
              <label className="block text-sm font-medium mb-1">
                Nombre de personnes
              </label>
              <input
                type="number"
                name="nombrePersonnes"
                value={formData.nombrePersonnes}
                onChange={handleChange}
                min="1"
                max={chambre.capacite}
                required
                className="w-full border rounded-lg px-3 py-2"
              />
              <p className="text-xs text-gray-500 mt-1">
                Capacité max : {chambre.capacite} personne(s)
              </p>
            </div>

            {/* Informations client */}
            <div>
              <label className="block text-sm font-medium mb-1">
                Nom complet
              </label>
              <input
                type="text"
                name="nomClient"
                value={formData.nomClient}
                onChange={handleChange}
                required
                className="w-full border rounded-lg px-3 py-2"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Email
              </label>
              <input
                type="email"
                name="emailClient"
                value={formData.emailClient}
                onChange={handleChange}
                required
                className="w-full border rounded-lg px-3 py-2"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Téléphone
              </label>
              <input
                type="tel"
                name="telephoneClient"
                value={formData.telephoneClient}
                onChange={handleChange}
                required
                className="w-full border rounded-lg px-3 py-2"
              />
            </div>

            {/* Récapitulatif prix */}
            {nombreNuits > 0 && (
              <div className="bg-gray-50 p-4 rounded-lg">
                <div className="flex justify-between mb-2">
                  <span>{chambre.prixParNuit}€ × {nombreNuits} nuit{nombreNuits > 1 ? 's' : ''}</span>
                  <span className="font-semibold">{prixTotal}€</span>
                </div>
              </div>
            )}

            {/* Bouton vérifier disponibilité */}
            {!disponibilite && (
              <button
                type="button"
                onClick={handleVerifierDisponibilite}
                disabled={loading || !formData.dateDebut || !formData.dateFin}
                className="w-full bg-gray-600 text-white py-3 rounded-lg hover:bg-gray-700 disabled:bg-gray-300"
              >
                {loading ? 'Vérification...' : 'Vérifier la disponibilité'}
              </button>
            )}

            {/* Message disponibilité */}
            {disponibilite !== null && (
              <div className={`p-4 rounded-lg ${
                disponibilite ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'
              }`}>
                {disponibilite
                  ? '✓ Chambre disponible pour ces dates'
                  : '✗ Chambre non disponible pour ces dates'
                }
              </div>
            )}

            {/* Erreur */}
            {error && (
              <div className="p-4 bg-red-50 text-red-800 rounded-lg">
                {error}
              </div>
            )}

            {/* Bouton réserver */}
            {disponibilite && (
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 disabled:bg-gray-300"
              >
                {loading ? 'Réservation...' : `Réserver pour ${prixTotal}€`}
              </button>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
