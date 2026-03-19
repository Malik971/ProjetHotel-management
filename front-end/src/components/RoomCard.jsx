import React from 'react';

export default function RoomCard({ chambre, onReserver }) {
  console.log('🛏️ RoomCard - chambre reçue:', chambre);
  console.log('🔧 RoomCard - onReserver:', typeof onReserver);

  const handleClick = () => {
    console.log('🖱️ Bouton Réserver cliqué pour:', chambre);
    if (onReserver && typeof onReserver === 'function') {
      onReserver(chambre);
    } else {
      console.error('❌ onReserver n\'est pas une fonction valide');
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow">
      {/* Image de la chambre */}
      <div className="h-48 bg-gray-200 relative">
        {chambre.imageUrl ? (
          <img
            src={chambre.imageUrl}
            alt={chambre.nom}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="flex items-center justify-center h-full">
            <span className="text-gray-400 text-4xl">🛏️</span>
          </div>
        )}
      </div>

      {/* Informations de la chambre */}
      <div className="p-4">
        <h3 className="text-xl font-bold mb-2">{chambre.nom || 'Chambre sans nom'}</h3>

        <div className="space-y-2 mb-4">
          <p className="text-gray-600 flex items-center">
            <span className="mr-2">👥</span>
            Capacité : {chambre.capacity || 'N/A'} personnes
          </p>

          <p className="text-gray-600 flex items-center">
            <span className="mr-2">📏</span>
            Surface : {chambre.surface || 'N/A'} m²
          </p>

          {chambre.equipements && chambre.equipements.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-2">
              {chambre.equipements.slice(0, 3).map((equipement, index) => (
                <span
                  key={index}
                  className="bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded"
                >
                  {equipement}
                </span>
              ))}
              {chambre.equipements.length > 3 && (
                <span className="text-xs text-gray-500">
                  +{chambre.equipements.length - 3} autres
                </span>
              )}
            </div>
          )}
        </div>

        {/* Prix et bouton */}
        <div className="flex items-center justify-between pt-4 border-t">
          <div>
            <p className="text-2xl font-bold text-blue-600">
              {chambre.prixParNuit ? `${chambre.prixParNuit}€` : 'Prix N/A'}
            </p>
            <p className="text-sm text-gray-500">par nuit</p>
          </div>

          <button
            onClick={handleClick}
            className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg font-semibold transition-colors"
          >
            Réserver
          </button>
        </div>
      </div>
    </div>
  );
}