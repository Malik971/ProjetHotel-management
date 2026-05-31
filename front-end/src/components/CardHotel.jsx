// src/components/CardHotel.jsx
import { Link } from "react-router-dom";
import { MapPin } from "lucide-react";
import EtoilesHotel from "./EtoilesHotel";
import { makeHotelSlug } from "../utils/slugify";

export default function CardHotel({ hotel, isSelected = false }) {
    if (!hotel) return null;

    const imageSrc = hotel.imageUrls?.[0]
        || "https://placehold.co/400x250/0EA5E9/ffffff?text=Hotel";

    const equipements = (hotel.equipements || []).slice(0, 3);
    const hotelSlug = makeHotelSlug(hotel);

    return (
        <div
            className={`group w-full bg-white rounded-2xl overflow-hidden border transition-all duration-300 flex flex-col h-full ${
                isSelected
                    ? "border-[#0EA5E9] shadow-[0_0_0_2px_#0EA5E940]"
                    : "border-gray-100 hover:border-[#0EA5E9]/40 hover:shadow-lg hover:shadow-[#0EA5E9]/10"
            }`}
        >
            {/* Image */}
            <div className="relative flex-shrink-0 overflow-hidden h-44">
                <img
                    src={imageSrc}
                    alt={hotel.nom || "Hotel"}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = "https://placehold.co/400x250/0EA5E9/ffffff?text=Hotel";
                    }}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 via-transparent to-transparent" />
            </div>

            {/* Contenu */}
            <div className="p-4 flex flex-col flex-1">

                <div className="flex-1 space-y-2">
                    <h3 className="font-semibold text-gray-900 text-sm leading-snug line-clamp-1">
                        {hotel.nom || "Nom non disponible"}
                    </h3>

                    <div className="flex items-center gap-1.5 text-gray-400 text-xs">
                        <MapPin size={11} className="flex-shrink-0 text-[#0EA5E9]" />
                        <span className="line-clamp-1">{hotel.ville || "Ville"}, France</span>
                    </div>

                    <div className="flex items-center gap-3 py-2 border-t border-b border-gray-50">
                        <div className="flex flex-col gap-0.5">
                            <span className="text-[9px] font-semibold text-gray-400 uppercase tracking-wider">
                                Classement
                            </span>
                            <EtoilesHotel categorie={hotel.categorie} size="xs" />
                        </div>

                        <div className="w-px h-6 bg-gray-100 flex-shrink-0" />

                        <div className="flex flex-col gap-0.5">
                            <span className="text-[9px] font-semibold text-gray-400 uppercase tracking-wider">
                                Avis clients
                            </span>
                            <div className="flex items-center gap-1">
                                {hotel.noteMoyenne ? (
                                    <>
                                        <span className="text-sm font-semibold text-gray-800">{hotel.noteMoyenne}</span>
                                        <span className="text-xs text-gray-400">/5</span>
                                        {hotel.nombreAvis && (
                                            <span className="text-[10px] text-gray-400">· {hotel.nombreAvis} avis</span>
                                        )}
                                    </>
                                ) : (
                                    <span className="text-xs text-gray-300">—</span>
                                )}
                            </div>
                        </div>
                    </div>

                    {equipements.length > 0 && (
                        <div className="flex flex-wrap gap-1">
                            {equipements.map((eq) => (
                                <span key={eq} className="text-[10px] text-[#0369A1] bg-[#E0F2FE] px-2 py-0.5 rounded-full">
                                    {eq}
                                </span>
                            ))}
                        </div>
                    )}

                    {hotel.description && (
                        <p className="text-gray-400 text-xs line-clamp-1 leading-relaxed">
                            {hotel.description}
                        </p>
                    )}
                </div>

                {/* Prix + CTA */}
                <div className="flex items-center justify-between pt-3 mt-3 border-t border-gray-100">
                    <div className="leading-tight">
                        <p className="text-[10px] text-gray-400 uppercase tracking-wider">A partir de</p>
                        <p className="text-[#0369A1] font-bold text-base leading-none mt-0.5">
                            {hotel.prixMoyenNuit ? `${hotel.prixMoyenNuit}€` : "—"}
                            <span className="text-gray-400 text-xs font-normal">/nuit</span>
                        </p>
                    </div>
                    <Link
                        to={`/hotel/${hotelSlug}`}
                        className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-xs font-semibold px-3.5 py-2 rounded-xl transition-colors duration-200 whitespace-nowrap"
                    >
                        Voir les chambres
                    </Link>
                </div>
            </div>
        </div>
    );
}