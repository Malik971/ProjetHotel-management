import React from 'react';

export default function ChambreList({ chambres, loading, error, onEdit, onDelete }) {
  if (loading) {
    return <div className="text-center py-10">Chargement...</div>;
  }

  if (error) {
    return <div className="bg-red-50 text-red-800 p-4 rounded-lg">{error}</div>;
  }

  if (chambres.length === 0) {
    return <div className="text-center py-10 text-gray-500">Aucune chambre</div>;
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Chambre
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Hôtel
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Prix/Nuit
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Capacité
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-200">
          {chambres.map((chambre) => (
            <tr key={chambre.id} className="hover:bg-gray-50">
              <td className="px-6 py-4">
                <div className="font-medium">{chambre.nom}</div>
                <div className="text-sm text-gray-500">{chambre.typeLit}</div>
              </td>
              <td className="px-6 py-4 text-sm">{chambre.hotelNom}</td>
              <td className="px-6 py-4 text-sm font-semibold">{chambre.prixParNuit}€</td>
              <td className="px-6 py-4 text-sm">{chambre.capacite} pers.</td>
              <td className="px-6 py-4 text-sm space-x-2">
                <button
                  onClick={() => onEdit(chambre)}
                  className="text-blue-600 hover:text-blue-800"
                >
                  Modifier
                </button>
                <button
                  onClick={() => onDelete(chambre.id)}
                  className="text-red-600 hover:text-red-800"
                >
                  Supprimer
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}