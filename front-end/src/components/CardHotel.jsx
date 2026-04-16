import React from "react";
import { Link } from "react-router-dom";
import { MapPin } from "lucide-react";

// Même style d'étoiles que les filtres (amber ★)
function EtoilesHotel({ categorie }) {
    if (!categorie) return null;
    return (
        <div className="flex items-center gap-0.5">
            {Array.from({ length: 5 }, (_, i) => (
                <span
                    key={i}
                    className={`text-sm leading-none ${
                        i < categorie ? "text-amber-400" : "text-gray-200"
                    }`}
                >
          ★
        </span>
            ))}
        </div>
    );
}

export default function CardHotel({ hotel }) {
    if (!hotel) return null;

    const imageSrc = hotel.imageUrl
        ? hotel.imageUrl.startsWith("http")
            ? hotel.imageUrl
            : `${import.meta.env.VITE_API_URL}${hotel.imageUrl}`
        : "https://placehold.co/400x250/e2e8f0/64748b?text=Hotel";

    const equipements = (hotel.equipements || []).slice(0, 3);

    return (
        <div className="bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-shadow duration-200 border border-gray-100 flex flex-col">
            {/* Image + badge catégorie */}
            <div className="relative flex-shrink-0">
                <img
                    src={imageSrc}
                    alt={hotel.nom || "Hôtel"}
                    className="w-full h-48 object-cover"
                    onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = "https://placehold.co/400x250/e2e8f0/64748b?text=Hotel";
                    }}
                />
                {hotel.categorie && (
                    <span className="absolute top-3 right-3 bg-white/90 backdrop-blur-sm text-gray-800 text-xs font-semibold px-2 py-1 rounded-lg">
            {hotel.categorie}★
          </span>
                )}
            </div>

            <div className="p-4 space-y-2.5 flex flex-col flex-1">
                {/* Nom */}
                <h3 className="font-bold text-gray-900 text-sm leading-tight line-clamp-1">
                    {hotel.nom || "Nom non disponible"}
                </h3>

                {/* Localisation */}
                <div className="flex items-center gap-1 text-gray-400 text-xs">
                    <MapPin size={11} className="flex-shrink-0" />
                    <span>{hotel.ville || "Ville"}, France</span>
                </div>

                {/* Étoiles catégorie + note */}
                <div className="flex items-center gap-2">
                    <EtoilesHotel categorie={hotel.categorie} />
                    {hotel.noteMoyenne && (
                        <span className="text-xs text-gray-400">
              {hotel.noteMoyenne}/5 · {hotel.nombreAvis || "350"} avis
            </span>
                    )}
                </div>

                {/* Pills équipements */}
                {equipements.length > 0 && (
                    <div className="flex flex-wrap gap-1">
                        {equipements.map((eq) => (
                            <span
                                key={eq}
                                className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full"
                            >
                {eq}
              </span>
                        ))}
                    </div>
                )}

                {/* Description */}
                {hotel.description && (
                    <p className="text-gray-400 text-xs line-clamp-2 leading-relaxed flex-1">
                        {hotel.description}
                    </p>
                )}

                {/* Prix + bouton */}
                <div className="flex items-center justify-between pt-2 border-t border-gray-100 mt-auto">
                    <div className="leading-tight">
                        <p className="text-xs text-gray-400">À partir de</p>
                        <p className="text-blue-600 font-bold text-base">
                            {hotel.prixMoyenNuit ? `${hotel.prixMoyenNuit}€` : "—"}
                            <span className="text-gray-400 text-xs font-normal">/nuit</span>
                        </p>
                    </div>
                    <Link
                        to={`/hotel/${hotel.id}`}
                        className="bg-gray-900 text-white text-xs font-semibold px-3.5 py-2 rounded-xl hover:bg-gray-700 transition-colors whitespace-nowrap"
                    >
                        Voir les chambres →
                    </Link>
                </div>
            </div>
        </div>
    );
}