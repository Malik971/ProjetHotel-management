// src/Pages/ChambreDetailsPage.jsx
import { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
    ArrowLeft, Users, Maximize2, Bed, CheckCircle, Wifi, Car,
    Waves, Dumbbell, Utensils, Sparkles, ChevronLeft, ChevronRight,
    Calendar, Star
} from "lucide-react";
import { getChambreById } from "../services/chambreService";
import { creerReservation } from "../services/reservationService";
import { extractHotelIdFromSlug } from "../utils/slugify";
import { useAuth } from "../hooks/useAuth";
import DateRangePicker, { toISO, formatRange } from "../components/DateRangePicker";

// Images de fallback si la chambre n'a pas de photos
const FALLBACK_ROOM_IMAGES = [
    "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=1200&q=80",
    "https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=1200&q=80",
    "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=1200&q=80",
    "https://images.unsplash.com/photo-1566195992011-5f6b21e539aa?w=1200&q=80",
    "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=1200&q=80",
];

const equipementIcon = (nom) => {
    const n = nom?.toLowerCase() || "";
    if (n.includes("wifi") || n.includes("internet")) return <Wifi size={16} />;
    if (n.includes("parking") || n.includes("voiture")) return <Car size={16} />;
    if (n.includes("piscine") || n.includes("pool")) return <Waves size={16} />;
    if (n.includes("sport") || n.includes("gym") || n.includes("fitness")) return <Dumbbell size={16} />;
    if (n.includes("restaurant") || n.includes("cuisine") || n.includes("minibar")) return <Utensils size={16} />;
    if (n.includes("spa") || n.includes("wellness") || n.includes("massage")) return <Sparkles size={16} />;
    return <CheckCircle size={16} />;
};

function resolveImage(url) {
    if (!url) return null;
    if (url.startsWith("http")) return url;
    return `${import.meta.env.VITE_API_URL}${url}`;
}

// Calcule le nombre de nuits entre deux dates ISO
function calculerNuits(dateDebut, dateFin) {
    if (!dateDebut || !dateFin) return 0;
    const d1 = new Date(dateDebut);
    const d2 = new Date(dateFin);
    const diff = Math.round((d2 - d1) / 86400000);
    return diff > 0 ? diff : 0;
}

function formatDateFr(iso) {
    if (!iso) return "";
    return new Intl.DateTimeFormat("fr-FR", {
        day: "numeric", month: "long", year: "numeric",
    }).format(new Date(iso));
}

// ─────────────────────────────────────────────────────────────────────────────
// Galerie style Booking
// ─────────────────────────────────────────────────────────────────────────────

function ChambreGallery({ images }) {
    const [activeIdx, setActiveIdx] = useState(0);
    const [lightbox, setLightbox] = useState(false);

    const prev = () => setActiveIdx((i) => (i - 1 + images.length) % images.length);
    const next = () => setActiveIdx((i) => (i + 1) % images.length);

    const main = images[activeIdx];
    const secondary = images.filter((_, i) => i !== activeIdx).slice(0, 2);

    return (
        <div className="mb-8">
            {/* Zone principale : grande photo + 2 empilees */}
            <div className="grid grid-cols-1 md:grid-cols-[1fr_0.55fr] gap-2 rounded-2xl overflow-hidden h-[260px] md:h-[420px]">

                {/* Grande photo */}
                <div className="relative group cursor-pointer" onClick={() => setLightbox(true)}>
                    <img
                        src={main}
                        alt="Chambre - photo principale"
                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />

                    {/* Fleches de navigation sur la grande photo */}
                    {images.length > 1 && (
                        <>
                            <button onClick={(e) => { e.stopPropagation(); prev(); }}
                                className="absolute left-3 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white rounded-full p-2 shadow transition opacity-0 group-hover:opacity-100">
                                <ChevronLeft size={18} className="text-gray-800" />
                            </button>
                            <button onClick={(e) => { e.stopPropagation(); next(); }}
                                className="absolute right-3 top-1/2 -translate-y-1/2 bg-white/90 hover:bg-white rounded-full p-2 shadow transition opacity-0 group-hover:opacity-100">
                                <ChevronRight size={18} className="text-gray-800" />
                            </button>
                        </>
                    )}

                    {/* Compteur */}
                    <div className="absolute bottom-3 right-3 bg-black/60 text-white text-xs font-semibold px-2.5 py-1 rounded-full">
                        {activeIdx + 1} / {images.length}
                    </div>
                </div>

                {/* 2 photos secondaires empilees (masquees sur mobile) */}
                <div className="hidden md:flex flex-col gap-2">
                    {secondary.map((img, i) => {
                        const realIdx = images.findIndex((x) => x === img && images.indexOf(x) !== activeIdx);
                        return (
                            <div key={i} className="flex-1 relative group cursor-pointer overflow-hidden"
                                onClick={() => setActiveIdx(images.indexOf(img))}>
                                <img src={img} alt={`Photo ${i + 2}`}
                                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
                                <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />
                                {/* Badge "X autres photos" sur la derniere vignette */}
                                {i === 1 && images.length > 3 && (
                                    <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                                        <span className="text-white font-semibold text-sm">
                                            +{images.length - 3} photos
                                        </span>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Bande de miniatures */}
            {images.length > 1 && (
                <div className="flex gap-2 mt-2 overflow-x-auto pb-1">
                    {images.map((img, i) => (
                        <button key={i} onClick={() => setActiveIdx(i)}
                            className={`flex-shrink-0 w-16 h-12 rounded-lg overflow-hidden border-2 transition-all ${
                                i === activeIdx
                                    ? "border-[#0EA5E9] shadow-md shadow-[#0EA5E9]/20"
                                    : "border-transparent opacity-60 hover:opacity-100"
                            }`}>
                            <img src={img} alt={`Miniature ${i + 1}`} className="w-full h-full object-cover" />
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Card reservation sticky (colonne droite)
// ─────────────────────────────────────────────────────────────────────────────

function ChambreReservationCard({ chambre, hotel, hotelSlug }) {
    const navigate = useNavigate();
    const { user, isAuthenticated } = useAuth();

    const [startDate, setStartDate] = useState(null);
    const [endDate, setEndDate] = useState(null);
    const [pickerOpen, setPickerOpen] = useState(false);
    const [nombrePersonnes, setNombrePersonnes] = useState(1);

    const dateDebut = startDate ? toISO(startDate) : "";
    const dateFin = endDate ? toISO(endDate) : "";
    const rangeLabel = formatRange(startDate, endDate);

    const nuits = calculerNuits(dateDebut, dateFin);
    const prix = parseFloat(chambre.prixParNuit || 0);
    const total = nuits * prix;

    const handleReserver = () => {
        if (!isAuthenticated) {
            navigate("/Connexion");
            return;
        }
        navigate("/paiement", {
            state: {
                chambre,
                hotel,
                hotelSlug,
                dateDebut,
                dateFin,
                nombrePersonnes,
                nuits,
                total,
            },
        });
    };

    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">

            {/* Prix */}
            <div className="mb-4">
                <p className="text-3xl font-bold text-[#0369A1]">
                    {prix}€
                    <span className="text-base font-normal text-gray-400"> / nuit</span>
                </p>
                <p className="text-xs text-gray-400 mt-0.5">Taxes et frais inclus</p>
            </div>

            {/* Dates : declencheur du DateRangePicker */}
            <div className="relative mb-3">
                <div
                    onClick={() => setPickerOpen((v) => !v)}
                    className="border border-gray-200 rounded-xl p-3 hover:border-[#0EA5E9] transition-colors cursor-pointer"
                >
                    <p className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                        Dates du sejour
                    </p>
                    <p className={`text-sm ${rangeLabel ? "text-gray-800" : "text-gray-400"}`}>
                        {rangeLabel || "Choisissez vos dates"}
                    </p>
                </div>
                {pickerOpen && (
                    <DateRangePicker
                        startDate={startDate}
                        endDate={endDate}
                        onChange={({ start, end }) => {
                            setStartDate(start);
                            setEndDate(end);
                        }}
                        onClose={() => setPickerOpen(false)}
                        position="left"
                    />
                )}
            </div>

            {/* Voyageurs */}
            <div className="border border-gray-200 rounded-xl p-3 mb-4 hover:border-[#0EA5E9] transition-colors">
                <p className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">Voyageurs</p>
                <div className="flex items-center justify-between">
                    <button onClick={() => setNombrePersonnes((n) => Math.max(1, n - 1))}
                        className="w-7 h-7 rounded-full border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-colors flex items-center justify-center text-lg font-light">
                        -
                    </button>
                    <span className="text-sm font-semibold text-gray-800">
                        {nombrePersonnes} {nombrePersonnes > 1 ? "personnes" : "personne"}
                    </span>
                    <button onClick={() => setNombrePersonnes((n) => Math.min(chambre.capacity || 10, n + 1))}
                        className="w-7 h-7 rounded-full border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-colors flex items-center justify-center text-lg font-light">
                        +
                    </button>
                </div>
            </div>

            {/* Recapitulatif prix */}
            {nuits > 0 && (
                <div className="bg-[#F0F9FF] rounded-xl p-3 mb-4 space-y-1.5">
                    <div className="flex justify-between text-sm text-gray-600">
                        <span>{prix}€ × {nuits} nuit{nuits > 1 ? "s" : ""}</span>
                        <span>{total.toFixed(2)}€</span>
                    </div>
                    <div className="border-t border-[#BAE6FD] pt-1.5 flex justify-between font-semibold text-gray-900">
                        <span>Total</span>
                        <span className="text-[#0369A1]">{total.toFixed(2)}€</span>
                    </div>
                </div>
            )}

            {/* Bouton */}
            <button
                onClick={handleReserver}
                disabled={!dateDebut || !dateFin || nuits <= 0}
                className="w-full bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-200 disabled:text-gray-400 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-xl transition-colors text-sm"
            >
                {!isAuthenticated
                    ? "Se connecter pour reserver"
                    : !dateDebut || !dateFin
                    ? "Choisissez vos dates"
                    : "Continuer vers la reservation"}
            </button>

            {!isAuthenticated && (
                <p className="text-center text-xs text-gray-400 mt-2">
                    Connexion requise pour reserver
                </p>
            )}
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Page principale
// ─────────────────────────────────────────────────────────────────────────────

export default function ChambreDetailsPage() {
    const { hotelSlug, chambreId } = useParams();
    const navigate = useNavigate();

    const [chambre, setChambre] = useState(null);
    const [hotel, setHotel] = useState(null);
    const [loading, setLoading] = useState(true);

    const hotelId = extractHotelIdFromSlug(hotelSlug);

    useEffect(() => {
        if (!hotelId || !chambreId) return;

        Promise.all([
            getChambreById(chambreId),
            fetch(`${import.meta.env.VITE_API_URL}/api/hotels/${hotelId}`).then((r) => r.json()),
        ])
            .then(([chambreData, hotelData]) => {
                setChambre(chambreData);
                setHotel(hotelData);
            })
            .catch(() => {})
            .finally(() => setLoading(false));
    }, [hotelId, chambreId]);

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-[#F8FAFC]">
                <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin" />
            </div>
        );
    }

    if (!chambre || !hotel) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-[#F8FAFC]">
                <p className="text-gray-600">Chambre introuvable</p>
                <button onClick={() => navigate(-1)}
                    className="bg-[#0EA5E9] text-white px-6 py-2.5 rounded-xl text-sm font-semibold">
                    Retour
                </button>
            </div>
        );
    }

    // Images : imageUrls en priorite, sinon fallback
    const rawImages = chambre.imageUrls?.length > 0
        ? chambre.imageUrls
        : FALLBACK_ROOM_IMAGES;

    const images = rawImages.map((img) => resolveImage(img)).filter(Boolean);

    const equipements = chambre.equipment || [];

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-7xl mx-auto px-4 md:px-6 pt-6 pb-16">

                {/* Breadcrumb */}
                <nav className="flex items-center gap-2 text-sm text-gray-400 mb-6 flex-wrap">
                    <Link to="/" className="hover:text-[#0EA5E9] transition-colors">Accueil</Link>
                    <span>/</span>
                    <Link to={`/hotel/${hotelSlug}`} className="hover:text-[#0EA5E9] transition-colors max-w-[180px] truncate">
                        {hotel.nom}
                    </Link>
                    <span>/</span>
                    <span className="text-gray-700 font-medium truncate max-w-[200px]">{chambre.nom}</span>
                </nav>

                {/* Galerie */}
                <ChambreGallery images={images} />

                {/* Layout 2 colonnes */}
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                    {/* Colonne gauche (2/3) */}
                    <div className="lg:col-span-2 space-y-8">

                        {/* Titre + caracteristiques */}
                        <div>
                            <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
                                {chambre.nom}
                            </h1>
                            <p className="text-sm text-gray-500 mb-4">{hotel.nom} - {hotel.ville}</p>

                            {/* Caracteristiques en pills */}
                            <div className="flex flex-wrap gap-3">
                                {chambre.capacity && (
                                    <div className="flex items-center gap-2 bg-white border border-gray-100 rounded-xl px-3 py-2 shadow-sm">
                                        <Users size={14} className="text-[#0EA5E9]" />
                                        <span className="text-xs font-medium text-gray-700">{chambre.capacity} personne{chambre.capacity > 1 ? "s" : ""}</span>
                                    </div>
                                )}
                                {chambre.superficie && (
                                    <div className="flex items-center gap-2 bg-white border border-gray-100 rounded-xl px-3 py-2 shadow-sm">
                                        <Maximize2 size={14} className="text-[#0EA5E9]" />
                                        <span className="text-xs font-medium text-gray-700">{chambre.superficie} m²</span>
                                    </div>
                                )}
                                {chambre.typeLit && (
                                    <div className="flex items-center gap-2 bg-white border border-gray-100 rounded-xl px-3 py-2 shadow-sm">
                                        <Bed size={14} className="text-[#0EA5E9]" />
                                        <span className="text-xs font-medium text-gray-700">{chambre.typeLit}</span>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Description */}
                        {chambre.description && (
                            <div>
                                <h2 className="text-lg font-bold text-gray-900 mb-3">Description</h2>
                                <p className="text-gray-600 leading-relaxed text-sm">{chambre.description}</p>
                            </div>
                        )}

                        {/* Equipements complets */}
                        {equipements.length > 0 && (
                            <div>
                                <h2 className="text-lg font-bold text-gray-900 mb-4">
                                    Ce que comprend cette chambre
                                </h2>
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                    {equipements.map((eq) => (
                                        <div key={eq} className="flex items-center gap-3 bg-white border border-gray-100 rounded-xl p-3.5 hover:border-[#0EA5E9]/30 hover:shadow-sm transition-all">
                                            <div className="w-8 h-8 rounded-lg bg-[#E0F2FE] text-[#0EA5E9] flex items-center justify-center flex-shrink-0">
                                                {equipementIcon(eq)}
                                            </div>
                                            <span className="text-sm font-medium text-gray-800">{eq}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Conditions */}
                        <div>
                            <h2 className="text-lg font-bold text-gray-900 mb-4">Conditions</h2>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                {[
                                    { icon: <CheckCircle size={15} />, label: "Annulation gratuite" },
                                    { icon: <Calendar size={15} />, label: "Check-in : a partir de 14h" },
                                    { icon: <Calendar size={15} />, label: "Check-out : avant 11h" },
                                    { icon: <Star size={15} />, label: "Confirmation immediate" },
                                ].map(({ icon, label }) => (
                                    <div key={label} className="flex items-center gap-3 text-sm text-gray-600">
                                        <span className="text-[#0EA5E9]">{icon}</span>
                                        {label}
                                    </div>
                                ))}
                            </div>
                        </div>

                    </div>

                    {/* Colonne droite : card sticky */}
                    <div className="lg:col-span-1">
                        <div className="lg:sticky lg:top-6">
                            <ChambreReservationCard
                                chambre={chambre}
                                hotel={hotel}
                                hotelSlug={hotelSlug}
                            />
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}