// src/components/DetailPages/RoomCard.jsx
import React from "react";
import { Users, Maximize2, Bed, CheckCircle } from "lucide-react";

// Fallback images pour chambres sans photo
const FALLBACK_ROOM_IMAGES = [
  "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80",
  "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&q=80",
  "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&q=80",
];

export default function RoomCard({ chambre, onReserver }) {
  if (!chambre) return null;

  // Résolution image : priorité à imageUrls[0], sinon imageUrl, sinon fallback aléatoire
  const fallback = FALLBACK_ROOM_IMAGES[(chambre.id || 0) % FALLBACK_ROOM_IMAGES.length];
  const rawImage = chambre.imageUrls?.[0] || chambre.imageUrl || fallback;
  const imageSrc = rawImage.startsWith("http")
      ? rawImage
      : `${import.meta.env.VITE_API_URL}${rawImage}`;

  const equipements = chambre.equipment || chambre.amenities || [];

  // Disponibilité fictive (à brancher sur vraie data quand dispo)
  const available = chambre.disponibles ?? null;

  return (
      <div className="bg-white border border-gray-100 rounded-2xl overflow-hidden hover:border-[#0EA5E9]/40 hover:shadow-md hover:shadow-[#0EA5E9]/10 transition-all duration-300 flex flex-col md:flex-row">

        {/* Image gauche */}
        <div className="relative flex-shrink-0 w-full md:w-60 h-48 md:h-auto">
          <img
              src={imageSrc}
              alt={chambre.nom || "Chambre"}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = fallback;
              }}
          />

          {/* Badge disponibilité */}
          {available !== null && available <= 2 && (
              <span className="absolute top-3 left-3 bg-[#F59E0B] text-white text-[11px] font-semibold px-2.5 py-1 rounded-lg shadow">
            Plus que {available} disponible{available > 1 ? "s" : ""}
          </span>
          )}
        </div>

        {/* Contenu centre */}
        <div className="flex-1 p-4 md:p-5 flex flex-col md:flex-row gap-4 md:gap-6">

          {/* Infos */}
          <div className="flex-1 space-y-3 min-w-0">
            <div>
              <h3 className="font-bold text-gray-900 text-base mb-1">
                {chambre.nom || chambre.name || "Chambre"}
              </h3>
              {chambre.description && (
                  <p className="text-gray-500 text-xs line-clamp-2 leading-relaxed">
                    {chambre.description}
                  </p>
              )}
            </div>

            {/* Caractéristiques */}
            <div className="flex flex-wrap items-center gap-3 text-xs text-gray-600">
              {(chambre.capacity || chambre.capacite) && (
                  <div className="flex items-center gap-1">
                    <Users size={13} className="text-[#0EA5E9]" />
                    <span>{chambre.capacity || chambre.capacite} personnes</span>
                  </div>
              )}
              {(chambre.superficie || chambre.size) && (
                  <div className="flex items-center gap-1">
                    <Maximize2 size={13} className="text-[#0EA5E9]" />
                    <span>{chambre.superficie || chambre.size} m²</span>
                  </div>
              )}
              {(chambre.typeLit || chambre.bedType) && (
                  <div className="flex items-center gap-1">
                    <Bed size={13} className="text-[#0EA5E9]" />
                    <span>{chambre.typeLit || chambre.bedType}</span>
                  </div>
              )}
            </div>

            {/* Équipements */}
            {equipements.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 pt-1">
                  {equipements.slice(0, 4).map((eq) => (
                      <div key={eq} className="flex items-center gap-2 text-xs text-gray-600">
                        <CheckCircle size={13} className="text-[#0EA5E9] flex-shrink-0" fill="#E0F2FE" />
                        <span className="line-clamp-1">{eq}</span>
                      </div>
                  ))}
                </div>
            )}
          </div>

          {/* Prix + CTA à droite (desktop) / en bas (mobile) */}
          <div className="md:w-44 md:border-l md:border-gray-100 md:pl-5 flex md:flex-col items-end md:items-stretch justify-between md:justify-end gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-gray-100">
            <div className="md:text-right">
              <p className="text-[#0369A1] font-bold text-xl leading-none">
                {chambre.prixParNuit || chambre.pricePerNight}
                <span className="text-sm">€</span>
                <span className="text-gray-400 text-xs font-normal"> / nuit</span>
              </p>
              <p className="text-[10px] text-gray-400 mt-1">Taxes et frais inclus</p>
            </div>

            <button
                onClick={() => onReserver?.(chambre)}
                className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold px-5 py-2.5 rounded-xl transition-colors whitespace-nowrap"
            >
              Réserver
            </button>
          </div>
        </div>
      </div>
  );
}