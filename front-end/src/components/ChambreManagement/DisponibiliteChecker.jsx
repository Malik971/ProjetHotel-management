import React, { useState } from 'react';
import chambreService from '../../services/chambreService';

export default function DisponibiliteChecker({ hotelId = null }) {
  const [dateDebut, setDateDebut] = useState('');
  const [dateFin, setDateFin] = useState('');
  const [chambresDisponibles, setChambresDisponibles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();

    setLoading(true);
    try {
      const chambres = await chambreService.getChambresDisponibles(
        dateDebut,
        dateFin,
        hotelId
      );
      setChambresDisponibles(chambres);
      setSearched(true);
    } catch (err) {
      alert('Erreur lors de la recherche');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow">
      <h2 className="text-xl font-bold mb-4">Vérifier la disponibilité</h2>

      <form onSubmit={handleSearch} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Date d'arrivée</label>
            <input
              type="date"
              value={dateDebut}
              onChange={(e) => setDateDebut(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
              required
              className="w-full border rounded-lg px-3 py-2"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Date de départ</label>
            <input
              type="date"
              value={dateFin}
              onChange={(e) => setDateFin(e.target.value)}
              min={dateDebut || new Date().toISOString().split('T')[0]}
              required
              className="w-full border rounded-lg px-3 py-2"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-300"
        >
          {loading ? 'Recherche...' : 'Rechercher'}
        </button>
      </form>

      {searched && (
        <div className="mt-6">
          <h3 className="font-semibold mb-3">
            {chambresDisponibles.length} chambre(s) disponible(s)
          </h3>

          {chambresDisponibles.length === 0 ? (
            <p className="text-gray-500">Aucune chambre disponible pour ces dates.</p>
          ) : (
            <div className="space-y-2">
              {chambresDisponibles.map((chambre) => (
                <div key={chambre.id} className="border rounded-lg p-3 flex justify-between items-center">
                  <div>
                    <p className="font-medium">{chambre.nom}</p>
                    <p className="text-sm text-gray-600">{chambre.hotelNom}</p>
                  </div>
                  <p className="font-semibold">{chambre.prixParNuit}€/nuit</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}