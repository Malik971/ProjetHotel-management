import React, { useState } from 'react';

export default function RoomCard({ chambre, onReserver }) {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // ✅ PROTECTION : Vérifier que chambre existe
  if (!chambre) {
    console.error('❌ RoomCard: chambre est undefined');
    return (
      <div className="bg-white rounded-lg shadow-md p-6 text-center text-gray-500">
        Chambre non disponible
      </div>
    );
  }

  console.log('🛏️ RoomCard - chambre reçue:', chambre);

  const nextImage = () => {
    if (chambre.imageUrls && chambre.imageUrls.length > 0) {
      setCurrentImageIndex((prev) =>
        prev === chambre.imageUrls.length - 1 ? 0 : prev + 1
      );
    }
  };

  const prevImage = () => {
    if (chambre.imageUrls && chambre.imageUrls.length > 0) {
      setCurrentImageIndex((prev) =>
        prev === 0 ? chambre.imageUrls.length - 1 : prev - 1
      );
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition">
      {/* Image Slider */}
      <div className="relative h-64 bg-gray-200">
        {chambre.imageUrls && chambre.imageUrls.length > 0 ? (
          <>
            <img
              src={chambre.imageUrls[currentImageIndex]}
              alt={chambre.nom || 'Chambre'}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.src = 'https://via.placeholder.com/400x300?text=Image+non+disponible';
              }}
            />

            {/* Navigation slider */}
            {chambre.imageUrls.length > 1 && (
              <>
                <button
                  onClick={prevImage}
                  className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full p-2 hover:bg-white"
                >
                  ←
                </button>
                <button
                  onClick={nextImage}
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full p-2 hover:bg-white"
                >
                  →
                </button>

                {/* Dots indicateurs */}
                <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
                  {chambre.imageUrls.map((_, idx) => (
                    <div
                      key={idx}
                      className={`w-2 h-2 rounded-full ${
                        idx === currentImageIndex ? 'bg-white' : 'bg-white/50'
                      }`}
                    />
                  ))}
                </div>
              </>
            )}
          </>
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            Aucune image
          </div>
        )}
      </div>

      {/* Informations */}
      <div className="p-4">
        <h3 className="text-xl font-semibold mb-2">{chambre.nom || 'Chambre sans nom'}</h3>

        <div className="space-y-2 text-sm text-gray-600 mb-4">
          <p>
            💶 <span className="font-semibold text-lg text-gray-900">
              {chambre.prixParNuit || 0}€
            </span> / nuit
          </p>

          <p>👥 {chambre.capacite || 0} personne{chambre.capacite > 1 ? 's' : ''}</p>
          <p>📐 {chambre.superficie || 0} m²</p>
          <p>🛏️ {chambre.typeLit || 'Non spécifié'}</p>
        </div>

        {/* Description */}
        {chambre.description && (
          <div className="mb-4">
            <p className="text-sm text-gray-600 line-clamp-2">{chambre.description}</p>
          </div>
        )}

        {/* Équipements */}
        {chambre.equipements && chambre.equipements.length > 0 ? (
          <div className="mb-4">
            <p className="text-sm font-medium text-gray-700 mb-1">Équipements :</p>
            <div className="flex flex-wrap gap-2">
              {chambre.equipements.slice(0, 3).map((eq, idx) => (
                <span
                  key={idx}
                  className="text-xs bg-gray-100 px-2 py-1 rounded"
                >
                  {eq}
                </span>
              ))}
              {chambre.equipements.length > 3 && (
                <span className="text-xs text-gray-500">
                  +{chambre.equipements.length - 3} autres
                </span>
              )}
            </div>
          </div>
        ) : (
          <div className="mb-4">
            <p className="text-xs text-gray-400 italic">Aucun équipement listé</p>
          </div>
        )}

        {/* Bouton réserver */}
        <button
          onClick={() => onReserver(chambre)}
          className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition"
        >
          Réserver
        </button>
      </div>
    </div>
  );
}