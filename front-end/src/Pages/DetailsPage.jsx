// src/Pages/DetailsPage.jsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
  ChevronLeft,
  ChevronRight,
  MapPin,
  Star,
  Wifi,
  Car,
  Waves,
  Dumbbell,
  Utensils,
  Sparkles,
  Phone,
  Mail,
  Map,
  CheckCircle,
  ArrowLeft,
  Gift,
} from "lucide-react";
import HotelRooms from "../components/DetailPages/HotelRooms";

// Icône par nom d'équipement
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

export default function DetailsPage() {
  const { hotelId } = useParams();
  const navigate = useNavigate();
  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentSlide, setCurrentSlide] = useState(0);

  useEffect(() => {
    if (!hotelId) return;
    fetch(`${import.meta.env.VITE_API_URL}/api/hotels/${hotelId}`)
        .then((res) => {
          if (!res.ok) throw new Error("Hôtel non trouvé");
          return res.json();
        })
        .then((data) => { setHotel(data); setLoading(false); })
        .catch(() => setLoading(false));
  }, [hotelId]);

  if (loading) {
    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600" />
        </div>
    );
  }

  if (!hotel) {
    return (
        <div className="flex flex-col items-center justify-center min-h-screen gap-4">
          <p className="text-gray-600 text-lg">Hôtel introuvable</p>
          <button
              onClick={() => navigate("/")}
              className="bg-blue-600 text-white px-6 py-2 rounded-xl"
          >
            Retour à l'accueil
          </button>
        </div>
    );
  }

  // Slides : imageUrl principale + éventuellement d'autres (à étendre quand tu auras plusieurs images)
  const slides = hotel.imageUrl
      ? [hotel.imageUrl]
      : ["https://placehold.co/1200x500/e2e8f0/64748b?text=Hotel"];

  const prevSlide = () =>
      setCurrentSlide((s) => (s - 1 + slides.length) % slides.length);
  const nextSlide = () =>
      setCurrentSlide((s) => (s + 1) % slides.length);

  const renderStars = (count = 0) => {
    return Array.from({ length: 5 }, (_, i) => (
        <Star
            key={i}
            size={16}
            className={
              i < count
                  ? "text-yellow-400 fill-yellow-400"
                  : "text-gray-300 fill-gray-300 opacity-40"
            }
        />
    ));
  };

  return (
      <div className="min-h-screen bg-gray-50">

        {/* ── Bouton retour ── */}
        <div className="max-w-7xl mx-auto px-6 pt-6 pb-2">
          <button
              onClick={() => navigate(-1)}
              className="flex items-center gap-2 text-gray-500 hover:text-gray-800 text-sm font-medium transition-colors"
          >
            <ArrowLeft size={16} />
            Retour aux résultats
          </button>
        </div>

        {/* ── Slider ── */}
        <div className="relative w-full mb-8">
          <div className="relative h-[500px] w-full overflow-hidden bg-gray-200">
            <img
                src={slides[currentSlide]}
                alt={hotel.nom}
                className="w-full h-full object-cover"
            />

            {slides.length > 1 && (
                <>
                  <button
                      onClick={prevSlide}
                      className="absolute left-4 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white rounded-full p-2 shadow transition"
                  >
                    <ChevronLeft size={20} />
                  </button>
                  <button
                      onClick={nextSlide}
                      className="absolute right-4 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white rounded-full p-2 shadow transition"
                  >
                    <ChevronRight size={20} />
                  </button>
                  <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                    {slides.map((_, i) => (
                        <button
                            key={i}
                            onClick={() => setCurrentSlide(i)}
                            className={`w-2 h-2 rounded-full transition-all ${
                                i === currentSlide ? "bg-white w-6" : "bg-white/50"
                            }`}
                        />
                    ))}
                  </div>
                </>
            )}
          </div>
        </div>

        {/* ── Contenu principal ── */}
        <div className="max-w-7xl mx-auto px-6 pb-16 grid grid-cols-1 lg:grid-cols-3 gap-8">

          {/* ── Colonne gauche (2/3) ── */}
          <div className="lg:col-span-2 space-y-8">

            {/* En-tête hôtel */}
            <div>
              {/* Badges */}
              <div className="flex flex-wrap gap-2 mb-3">
                {hotel.categorie && (
                    <span className="bg-yellow-100 text-yellow-800 text-xs font-semibold px-3 py-1 rounded-full">
                  {hotel.categorie}★ {hotel.categorie === 5 ? "Luxe" : hotel.categorie === 4 ? "Premium" : "Confort"}
                </span>
                )}
                {hotel.noteMoyenne >= 4.5 && (
                    <span className="bg-gray-100 text-gray-700 text-xs font-semibold px-3 py-1 rounded-full flex items-center gap-1">
                  Recommandé
                </span>
                )}
              </div>

              <h1 className="text-2xl font-bold text-gray-900 mb-2">{hotel.nom}</h1>

              <div className="flex items-center gap-4 flex-wrap">
                <div className="flex items-center gap-1 text-gray-500 text-sm">
                  <MapPin size={14} />
                  <span>{hotel.ville}, France</span>
                </div>
                {hotel.noteMoyenne && (
                    <div className="flex items-center gap-1.5">
                      <div className="flex">{renderStars(hotel.categorie)}</div>
                      <span className="text-sm font-semibold text-gray-700">
                    {hotel.noteMoyenne}
                  </span>
                      <span className="text-sm text-gray-400">(350 avis)</span>
                    </div>
                )}
              </div>
            </div>

            {/* Équipements & Services */}
            {hotel.equipements && hotel.equipements.length > 0 && (
                <div>
                  <h2 className="text-lg font-bold text-gray-900 mb-4">
                    Équipements & Services
                  </h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {hotel.equipements.map((eq) => (
                        <div
                            key={eq}
                            className="flex items-start gap-3 border border-gray-200 rounded-xl p-4 bg-white"
                        >
                          <div className="text-blue-600 mt-0.5 flex-shrink-0">
                            {equipementIcon(eq)}
                          </div>
                          <div>
                            <p className="font-medium text-gray-800 text-sm">{eq}</p>
                          </div>
                        </div>
                    ))}
                  </div>
                </div>
            )}

            {/* Description */}
            {hotel.description && (
                <div>
                  <h2 className="text-lg font-bold text-gray-900 mb-3">
                    À propos de l'hôtel
                  </h2>
                  <p className="text-gray-600 leading-relaxed text-sm">
                    {hotel.description}
                  </p>
                </div>
            )}

            {/* ── Chambres ── */}
            <div>
              <h2 className="text-lg font-bold text-gray-900 mb-4">
                Chambres disponibles
              </h2>
              <HotelRooms hotelId={parseInt(hotelId)} />
            </div>
          </div>

          {/* ── Colonne droite (1/3) — carte récapitulatif ── */}
          <div className="lg:col-span-1 space-y-4">

            {/* Prix + CTA */}
            <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm sticky top-6">
              <p className="text-sm text-gray-500 mb-1">À partir de</p>
              <p className="text-3xl font-bold text-blue-600 mb-1">
                {hotel.prixMoyenNuit ? `${hotel.prixMoyenNuit}€` : "—"}
                <span className="text-base font-normal text-gray-400">/ nuit</span>
              </p>
              <p className="text-xs text-gray-400 mb-5">Taxes et frais inclus</p>

              <div className="space-y-2 mb-5 text-sm text-gray-600">
                {hotel.noteMoyenne && (
                    <div className="flex items-center gap-2">
                      <Star size={14} className="text-yellow-400 fill-yellow-400" />
                      <span>Noté {hotel.noteMoyenne}/10 (350 avis)</span>
                    </div>
                )}
                <div className="flex items-center gap-2">
                  <CheckCircle size={14} className="text-green-500" />
                  <span>Annulation gratuite</span>
                </div>
                <div className="flex items-center gap-2">
                  <MapPin size={14} className="text-blue-500" />
                  <span>Établissement recommandé</span>
                </div>
              </div>

              {/* Bouton qui nécessite une connexion */}
              <Link
                  to="/login"
                  className="block w-full bg-gray-900 text-white text-center font-semibold py-3 rounded-xl hover:bg-gray-700 transition-colors mb-3"
              >
                Voir les disponibilités
              </Link>
              <p className="text-center text-xs text-gray-400">
                Connexion requise pour réserver
              </p>

              {/* Coordonnées */}
              <div className="mt-5 pt-5 border-t border-gray-100 space-y-2">
                <p className="text-sm font-semibold text-gray-800">Contact</p>
                {hotel.adresse && (
                    <div className="flex items-start gap-2 text-xs text-gray-500">
                      <MapPin size={12} className="mt-0.5 flex-shrink-0" />
                      <span>{hotel.adresse}</span>
                    </div>
                )}
              </div>

              {/* Voir sur la carte */}
              {hotel.latitude && hotel.longitude && (
                  <button
                      onClick={() =>
                          window.open(
                              `https://www.google.com/maps?q=${hotel.latitude},${hotel.longitude}`,
                              "_blank"
                          )
                      }
                      className="mt-4 w-full flex items-center justify-center gap-2 border border-gray-200 text-gray-700 text-sm font-medium py-2.5 rounded-xl hover:bg-gray-50 transition-colors"
                  >
                    <Map size={15} />
                    Voir sur la carte
                  </button>
              )}
            </div>

            {/* Offre spéciale */}
            <div className="bg-blue-50 border border-blue-100 rounded-2xl p-4 flex items-start gap-3">
              <Gift size={18} className="text-blue-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-blue-800 text-sm">Offre spéciale</p>
                <p className="text-blue-600 text-xs mt-0.5">
                  Réservez 3 nuits et obtenez 15% de réduction
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}