// src/Pages/HotelDetailsPage.jsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
    ChevronLeft,
    ChevronRight,
    MapPin,
    Wifi,
    Car,
    Waves,
    Dumbbell,
    Utensils,
    Sparkles,
    Map as MapIcon,
    CheckCircle,
    ArrowLeft,
    Gift,
} from "lucide-react";
import HotelRooms from "../components/DetailPages/HotelRooms";
import EtoilesHotel from "../components/EtoilesHotel";
import { extractHotelIdFromSlug } from "../utils/slugify";

const FALLBACK_IMAGES = [
    "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1600&q=80",
    "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=1600&q=80",
    "https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=1600&q=80",
    "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=1600&q=80",
    "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1600&q=80",
];

const equipementIcon = (nom) => {
    const n = nom?.toLowerCase() || "";
    if (n.includes("wifi") || n.includes("internet")) return <Wifi size={18} />;
    if (n.includes("parking") || n.includes("voiture")) return <Car size={18} />;
    if (n.includes("piscine") || n.includes("pool")) return <Waves size={18} />;
    if (n.includes("sport") || n.includes("gym") || n.includes("fitness")) return <Dumbbell size={18} />;
    if (n.includes("restaurant") || n.includes("cuisine")) return <Utensils size={18} />;
    if (n.includes("spa") || n.includes("wellness") || n.includes("massage")) return <Sparkles size={18} />;
    return <CheckCircle size={18} />;
};

export default function HotelDetailsPage() {
    const { hotelSlug } = useParams();
    const navigate = useNavigate();
    const [hotel, setHotel] = useState(null);
    const [loading, setLoading] = useState(true);
    const [currentSlide, setCurrentSlide] = useState(0);

    // L'id est encode dans le slug : "hotel-des-arceaux-42" -> 42
    const hotelId = extractHotelIdFromSlug(hotelSlug);

    useEffect(() => {
        if (!hotelId) return;
        fetch(`${import.meta.env.VITE_API_URL}/api/hotels/${hotelId}`)
            .then((res) => {
                if (!res.ok) throw new Error("Hotel non trouve");
                return res.json();
            })
            .then((data) => { setHotel(data); setLoading(false); })
            .catch(() => setLoading(false));
    }, [hotelId]);

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-[#F8FAFC]">
                <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin" />
            </div>
        );
    }

    if (!hotel) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-[#F8FAFC]">
                <p className="text-gray-600 text-lg">Hotel introuvable</p>
                <button
                    onClick={() => navigate("/")}
                    className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white px-6 py-2.5 rounded-xl text-sm font-semibold transition-colors"
                >
                    Retour a l'accueil
                </button>
            </div>
        );
    }

        // imageUrls est desormais la source unique (V7)
    const slides = hotel.imageUrls?.length > 0
        ? hotel.imageUrls
        : FALLBACK_IMAGES;

    const prevSlide = () => setCurrentSlide((s) => (s - 1 + slides.length) % slides.length);
    const nextSlide = () => setCurrentSlide((s) => (s + 1) % slides.length);

    return (
        <div className="min-h-screen bg-[#F8FAFC]">

            {/* Bouton retour */}
            <div className="max-w-7xl mx-auto px-4 md:px-6 pt-6 pb-3">
                <button
                    onClick={() => navigate(-1)}
                    className="flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors"
                >
                    <ArrowLeft size={16} />
                    Retour aux resultats
                </button>
            </div>

            {/* Slider */}
            <div className="relative w-full mb-8">
                <div className="relative h-[340px] md:h-[500px] w-full overflow-hidden bg-gray-200">
                    <img
                        key={slides[currentSlide]}
                        src={slides[currentSlide]}
                        alt={`${hotel.nom} - vue ${currentSlide + 1}`}
                        className="w-full h-full object-cover object-center block"
                        style={{ imageRendering: "auto" }}
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent pointer-events-none" />

                    {slides.length > 1 && (
                        <>
                            <button onClick={prevSlide} className="absolute left-3 md:left-6 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white rounded-full p-2.5 shadow-lg transition">
                                <ChevronLeft size={20} className="text-gray-800" />
                            </button>
                            <button onClick={nextSlide} className="absolute right-3 md:right-6 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white rounded-full p-2.5 shadow-lg transition">
                                <ChevronRight size={20} className="text-gray-800" />
                            </button>
                            <div className="absolute bottom-5 left-1/2 -translate-x-1/2 flex gap-2">
                                {slides.map((_, i) => (
                                    <button key={i} onClick={() => setCurrentSlide(i)}
                                        className={`h-1.5 rounded-full transition-all ${i === currentSlide ? "bg-white w-8" : "bg-white/60 w-1.5 hover:bg-white/80"}`} />
                                ))}
                            </div>
                            <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-sm text-white text-xs font-semibold px-3 py-1.5 rounded-full">
                                {currentSlide + 1} / {slides.length}
                            </div>
                        </>
                    )}
                </div>
            </div>

            {/* Contenu principal */}
            <div className="max-w-7xl mx-auto px-4 md:px-6 pb-16 grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Colonne gauche */}
                <div className="lg:col-span-2 space-y-8">

                    {/* En-tete */}
                    <div>
                        <div className="flex flex-wrap gap-2 mb-3">
                            {hotel.categorie && (
                                <span className="bg-[#E0F2FE] text-[#0369A1] text-xs font-semibold px-3 py-1 rounded-full">
                                    {hotel.categorie === 5 ? "Luxe" : hotel.categorie === 4 ? "Premium" : hotel.categorie === 3 ? "Confort" : "Economique"}
                                </span>
                            )}
                            {hotel.noteMoyenne >= 4.5 && (
                                <span className="bg-[#FEF3C7] text-[#92400E] text-xs font-semibold px-3 py-1 rounded-full">
                                    Recommande
                                </span>
                            )}
                        </div>

                        <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-3">{hotel.nom}</h1>

                        <div className="flex items-center gap-4 flex-wrap">
                            <div className="flex items-center gap-1 text-gray-500 text-sm">
                                <MapPin size={14} className="text-[#0EA5E9]" />
                                <span>{hotel.ville}, France</span>
                            </div>
                            <div className="flex items-center gap-3">
                                <div className="flex flex-col gap-0.5">
                                    <span className="text-[9px] font-semibold text-gray-400 uppercase tracking-wider">Classement</span>
                                    <EtoilesHotel categorie={hotel.categorie} size="sm" />
                                </div>
                                <div className="w-px h-8 bg-gray-200" />
                                <div className="flex flex-col gap-0.5">
                                    <span className="text-[9px] font-semibold text-gray-400 uppercase tracking-wider">Avis clients</span>
                                    <div className="flex items-center gap-1">
                                        {hotel.noteMoyenne ? (
                                            <>
                                                <span className="text-sm font-semibold text-gray-800">{hotel.noteMoyenne}</span>
                                                <span className="text-xs text-gray-400">/5</span>
                                                <span className="text-[10px] text-gray-400">· 350 avis</span>
                                            </>
                                        ) : (
                                            <span className="text-xs text-gray-300">-</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Equipements */}
                    {hotel.equipements && hotel.equipements.length > 0 && (
                        <div>
                            <h2 className="text-lg font-bold text-gray-900 mb-4">Equipements & Services</h2>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {hotel.equipements.map((eq) => (
                                    <div key={eq} className="flex items-center gap-3 border border-gray-100 rounded-xl p-3.5 bg-white hover:border-[#0EA5E9]/30 hover:shadow-sm transition-all">
                                        <div className="w-9 h-9 rounded-lg bg-[#E0F2FE] text-[#0EA5E9] flex items-center justify-center flex-shrink-0">
                                            {equipementIcon(eq)}
                                        </div>
                                        <p className="font-medium text-gray-800 text-sm">{eq}</p>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* A propos */}
                    <div>
                        <h2 className="text-lg font-bold text-gray-900 mb-3">A propos de l'hotel</h2>
                        <p className="text-gray-600 leading-relaxed text-sm">
                            {hotel.description && hotel.description.trim()
                                ? hotel.description
                                : `Cet etablissement vous accueille dans un cadre elegant au coeur de ${hotel.ville || "la ville"}. Description detaillee a venir.`}
                        </p>
                    </div>

                    {/* Chambres - hotelSlug passe pour que HotelRooms construise les URLs */}
                    <div>
                        <h2 className="text-lg font-bold text-gray-900 mb-4">Nos chambres & suites</h2>
                        <HotelRooms hotelId={hotelId} hotelSlug={hotelSlug} />
                    </div>
                </div>

                {/* Colonne droite sticky */}
                <div className="lg:col-span-1">
                    <div className="lg:sticky lg:top-6 space-y-4">

                        <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm">
                            <p className="text-xs text-gray-500 mb-1 uppercase tracking-wider">A partir de</p>
                            <p className="text-3xl font-bold text-[#0369A1] mb-1">
                                {hotel.prixMoyenNuit ? `${hotel.prixMoyenNuit}€` : "-"}
                                <span className="text-base font-normal text-gray-400"> / nuit</span>
                            </p>
                            <p className="text-xs text-gray-400 mb-5">Taxes et frais inclus</p>

                            <div className="space-y-2.5 mb-5 text-sm text-gray-600">
                                {hotel.noteMoyenne && (
                                    <div className="flex items-center gap-2.5">
                                        <div className="w-7 h-7 rounded-lg bg-[#FEF3C7] text-[#92400E] flex items-center justify-center flex-shrink-0">
                                            <span className="text-xs font-bold">★</span>
                                        </div>
                                        <span className="text-xs">Note {hotel.noteMoyenne}/5</span>
                                    </div>
                                )}
                                <div className="flex items-center gap-2.5">
                                    <div className="w-7 h-7 rounded-lg bg-[#E0F2FE] text-[#0EA5E9] flex items-center justify-center flex-shrink-0">
                                        <CheckCircle size={14} />
                                    </div>
                                    <span className="text-xs">Annulation gratuite</span>
                                </div>
                                <div className="flex items-center gap-2.5">
                                    <div className="w-7 h-7 rounded-lg bg-[#E0F2FE] text-[#0EA5E9] flex items-center justify-center flex-shrink-0">
                                        <MapPin size={14} />
                                    </div>
                                    <span className="text-xs">Etablissement recommande</span>
                                </div>
                            </div>

                            <p className="text-center text-xs text-gray-400 mt-3">
                                Choisissez une chambre ci-dessous pour reserver
                            </p>

                            {hotel.adresse && (
                                <div className="mt-5 pt-5 border-t border-gray-100">
                                    <p className="text-xs font-semibold text-gray-800 uppercase tracking-wider mb-2">Contact</p>
                                    <div className="flex items-start gap-2 text-xs text-gray-500">
                                        <MapPin size={12} className="mt-0.5 flex-shrink-0 text-[#0EA5E9]" />
                                        <span>{hotel.adresse}</span>
                                    </div>
                                </div>
                            )}

                            {hotel.latitude && hotel.longitude && (
                                <button
                                    onClick={() => window.open(`https://www.google.com/maps?q=${hotel.latitude},${hotel.longitude}`, "_blank")}
                                    className="mt-4 w-full flex items-center justify-center gap-2 border border-gray-200 text-gray-700 text-sm font-medium py-2.5 rounded-xl hover:bg-gray-50 hover:border-[#0EA5E9] transition-colors"
                                >
                                    <MapIcon size={15} />
                                    Voir sur la carte
                                </button>
                            )}
                        </div>

                        <div className="bg-[#F0F9FF] border border-[#BAE6FD] rounded-2xl p-4 flex items-start gap-3">
                            <div className="w-9 h-9 rounded-lg bg-[#0EA5E9] text-white flex items-center justify-center flex-shrink-0">
                                <Gift size={18} />
                            </div>
                            <div>
                                <p className="font-semibold text-[#0369A1] text-sm">Offre speciale</p>
                                <p className="text-[#0EA5E9] text-xs mt-0.5">Reservez 3 nuits et obtenez 15% de reduction</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}