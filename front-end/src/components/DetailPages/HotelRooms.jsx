// src/components/DetailPages/HotelRooms.jsx
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Calendar, Users } from "lucide-react";
import { useChambres } from "../../hooks/useChambres";
import RoomCard from "./RoomCard";
import DateRangePicker from "../DateRangePicker";
import { toISO, formatRange } from "../../utils/dateUtils";

/**
 * Liste des chambres d'un hotel avec filtre de disponibilite par dates.
 *
 * Comportement :
 *   - Si aucune date selectionnee : affiche toutes les chambres telles quelles
 *   - Si dates selectionnees : interroge /api/chambres/disponibles pour obtenir
 *     la liste des chambres libres, et marque les autres comme indisponibles
 *
 * Le clic sur "Reserver" navigue vers /hotel/{hotelSlug}/chambre/{id} avec
 * les dates en query string si elles sont selectionnees.
 */
export default function HotelRooms({ hotelId, hotelSlug }) {
    const navigate = useNavigate();
    const { chambres, loading, error } = useChambres(hotelId);

    const [startDate, setStartDate] = useState(null);
    const [endDate, setEndDate] = useState(null);
    const [pickerOpen, setPickerOpen] = useState(false);
    const [voyageurs, setVoyageurs] = useState(1);

    const [chambresDispoIds, setChambresDispoIds] = useState(null); // null = pas de filtre actif
    const [checkingDispo, setCheckingDispo] = useState(false);

    const rangeLabel = formatRange(startDate, endDate);
    const hasDates = startDate && endDate;

    // Quand les dates changent, on interroge l'endpoint disponibilite
    useEffect(() => {
        if (!hasDates) {
            setChambresDispoIds(null);
            return;
        }
        setCheckingDispo(true);
        const url = `${import.meta.env.VITE_API_URL}/api/chambres/disponibles?dateDebut=${toISO(startDate)}&dateFin=${toISO(endDate)}&hotelId=${hotelId}`;
        fetch(url)
            .then((r) => r.json())
            .then((data) => {
                const ids = new Set((data || []).map((c) => c.id));
                setChambresDispoIds(ids);
            })
            .catch(() => setChambresDispoIds(null))
            .finally(() => setCheckingDispo(false));
    }, [startDate, endDate, hotelId, hasDates]);

    const handleReserver = (chambre) => {
        const query = hasDates
            ? `?dateDebut=${toISO(startDate)}&dateFin=${toISO(endDate)}&voyageurs=${voyageurs}`
            : "";
        navigate(`/hotel/${hotelSlug}/chambre/${chambre.id}${query}`);
    };

    if (loading) {
        return (
            <div className="text-center py-10">
                <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin mx-auto" />
                <p className="mt-4 text-gray-500 text-sm">Chargement des chambres...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-red-50 border border-red-200 text-red-600 p-4 rounded-xl text-sm">
                <p className="font-semibold mb-1">Erreur</p>
                <p className="text-red-400">{error}</p>
            </div>
        );
    }

    if (!chambres || chambres.length === 0) {
        return (
            <div className="text-center py-10 bg-white border border-gray-100 rounded-2xl">
                <p className="text-gray-200 text-4xl mb-3">—</p>
                <p className="text-gray-500 text-sm">Aucune chambre disponible pour cet hotel.</p>
            </div>
        );
    }

    return (
        <div className="space-y-4">

            {/* Mini barre de filtre dates + voyageurs */}
            <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm">
                <div className="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-3 items-center">
                    <div className="relative">
                        <div
                            onClick={() => setPickerOpen((v) => !v)}
                            className="flex items-center gap-3 px-4 py-3 border border-gray-200 rounded-xl cursor-pointer hover:border-[#0EA5E9] transition-colors"
                        >
                            <Calendar size={16} className="text-[#0EA5E9] flex-shrink-0" />
                            <div className="flex-1">
                                <p className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-0.5">
                                    Verifier la disponibilite
                                </p>
                                <p className={`text-sm ${rangeLabel ? "text-gray-800" : "text-gray-400"}`}>
                                    {rangeLabel || "Selectionnez vos dates"}
                                </p>
                            </div>
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

                    <div className="flex items-center gap-3 px-4 py-3 border border-gray-200 rounded-xl">
                        <Users size={16} className="text-[#0EA5E9] flex-shrink-0" />
                        <button onClick={() => setVoyageurs((n) => Math.max(1, n - 1))}
                            className="w-6 h-6 rounded-full border border-gray-200 hover:border-[#0EA5E9] text-gray-700 text-sm">-</button>
                        <span className="text-sm font-medium text-gray-800 min-w-[60px] text-center">
                            {voyageurs} {voyageurs > 1 ? "pers." : "pers."}
                        </span>
                        <button onClick={() => setVoyageurs((n) => n + 1)}
                            className="w-6 h-6 rounded-full border border-gray-200 hover:border-[#0EA5E9] text-gray-700 text-sm">+</button>
                    </div>
                </div>

                {/* Etat de filtrage */}
                {hasDates && (
                    <div className="mt-3 pt-3 border-t border-gray-100">
                        {checkingDispo ? (
                            <p className="text-xs text-gray-400">Verification en cours...</p>
                        ) : chambresDispoIds ? (
                            <p className="text-xs text-gray-500">
                                <span className="font-semibold text-emerald-600">{chambresDispoIds.size}</span> chambre{chambresDispoIds.size > 1 ? "s" : ""} disponible{chambresDispoIds.size > 1 ? "s" : ""} sur {chambres.length} pour ces dates
                            </p>
                        ) : null}
                    </div>
                )}
            </div>

            {/* Liste des chambres */}
            <div className="flex flex-col gap-4">
                {chambres.map((chambre) => {
                    // Si pas de dates ou capacite suffisante : chambre disponible par defaut
                    const isDispoDates = !hasDates || (chambresDispoIds && chambresDispoIds.has(chambre.id));
                    const isDispoCapacite = !chambre.capacity || chambre.capacity >= voyageurs;
                    const isDisponible = isDispoDates && isDispoCapacite;

                    return (
                        <RoomCard
                            key={chambre.id}
                            chambre={chambre}
                            onReserver={handleReserver}
                            disponible={isDisponible}
                            raisonIndispo={
                                !isDispoCapacite ? "Capacite insuffisante" :
                                !isDispoDates ? "Indisponible pour ces dates" :
                                null
                            }
                        />
                    );
                })}
            </div>
        </div>
    );
}