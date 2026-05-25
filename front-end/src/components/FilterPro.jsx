// src/components/FilterPro.jsx
import { useState } from "react";
import { ChevronDown } from "lucide-react";
import EtoilesHotel from "./EtoilesHotel";

const EQUIPEMENTS_OPTIONS = [
    { value: "Wifi", label: "WiFi" },
    { value: "Restaurant", label: "Restaurant" },
    { value: "Parking", label: "Parking" },
    { value: "Piscine", label: "Piscine" },
    { value: "Spa", label: "Spa & bien-être" },
    { value: "Bar", label: "Bar" },
    { value: "Climatisation", label: "Climatisation" },
];

const TRI_OPTIONS = [
    { value: "", label: "Pertinence" },
    { value: "prix_asc", label: "Prix croissant" },
    { value: "prix_desc", label: "Prix décroissant" },
    { value: "note_desc", label: "Mieux notés" },
    { value: "nom_asc", label: "Nom A → Z" },
];

const INITIAL = {
    prixMax: 500,
    categorie: [],
    equipements: [],
    notationMin: 0,
    tri: "",
};

/**
 * Composant de filtres.
 *
 * Mode desktop (prop isMobile absent ou false) :
 *   Chaque changement declenche immediatement onFilterChange.
 *   Le bouton "Appliquer" est conserve mais rendu discret car il n'est
 *   plus necessaire : il sert uniquement de confirmation visuelle.
 *
 * Mode mobile (prop isMobile=true, passe par HomePage quand le drawer est ouvert) :
 *   Les changements s'accumulent, l'utilisateur clique "Appliquer" pour confirmer.
 *   Raison : le drawer se ferme apres onFilterChange dans HomePage, donc un filtre
 *   reactif fermerait le drawer a chaque case cochee, ce qui serait perturbant.
 *
 * Pourquoi calculer le nouvel etat avant setFilters ?
 *   setFilters est asynchrone : si on appelle onFilterChange(filters) juste apres
 *   setFilters(newVal), on envoie encore l'ancienne valeur de filters. On calcule
 *   donc newFilters, on le passe a setFilters ET a onFilterChange en meme temps.
 */
export default function Filter({ onFilterChange, onReset, isMobile = false }) {
    const [filters, setFilters] = useState(INITIAL);

    /**
     * Met a jour l'etat et, en mode desktop, notifie immediatement le parent.
     */
    const update = (values) => {
        const newFilters = { ...filters, ...values };
        setFilters(newFilters);
        if (!isMobile) {
            onFilterChange?.(newFilters);
        }
    };

    /**
     * Bascule un element dans un tableau (categorie ou equipements).
     */
    const toggle = (key, value) => {
        const current = filters[key];
        const newVal = current.includes(value)
            ? current.filter((v) => v !== value)
            : [...current, value];
        update({ [key]: newVal });
    };

    /**
     * Bouton "Appliquer" : utile uniquement en mode mobile.
     * En desktop il reste visible mais son role est redondant.
     */
    const apply = () => onFilterChange?.(filters);

    const reset = () => {
        setFilters(INITIAL);
        onReset?.();
    };

    return (
        <aside className="w-72 bg-white border border-gray-100 rounded-2xl p-6 space-y-6 sticky top-6 self-start shadow-sm">

            {/* Header */}
            <div className="border-b border-gray-100 pb-4">
                <h2 className="text-xs font-semibold text-[#0EA5E9] uppercase tracking-[0.2em]">
                    Filtres
                </h2>
            </div>

            {/* Tri */}
            <section className="space-y-2">
                <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest">
                    Trier par
                </p>
                <div className="relative">
                    <select
                        value={filters.tri}
                        onChange={(e) => update({ tri: e.target.value })}
                        className="w-full appearance-none bg-[#F0F9FF] border border-[#BAE6FD] text-gray-700 text-sm rounded-xl px-3 py-2.5 pr-8 focus:outline-none focus:border-[#0EA5E9] transition-colors cursor-pointer"
                    >
                        {TRI_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                    <ChevronDown
                        size={14}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-[#0EA5E9] pointer-events-none"
                    />
                </div>
            </section>

            {/* Prix */}
            <section className="space-y-3">
                <div className="flex justify-between items-center">
                    <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest">
                        Prix par nuit
                    </p>
                    <span className="text-sm font-bold text-[#0369A1]">
                        {filters.prixMax} €
                    </span>
                </div>
                <input
                    type="range"
                    min="20"
                    max="500"
                    step="5"
                    value={filters.prixMax}
                    onChange={(e) => update({ prixMax: Number(e.target.value) })}
                    className="w-full h-1.5 rounded-full appearance-none cursor-pointer"
                    style={{
                        background: `linear-gradient(to right, #0EA5E9 ${((filters.prixMax - 20) / 480) * 100}%, #E0F2FE ${((filters.prixMax - 20) / 480) * 100}%)`,
                        accentColor: "#0EA5E9",
                    }}
                />
                <div className="flex justify-between text-xs text-gray-300">
                    <span>20 €</span>
                    <span>500 €</span>
                </div>
            </section>

            {/* Classement */}
            <section className="space-y-3">
                <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest">
                    Classement
                </p>
                <div className="space-y-2.5">
                    {[5, 4, 3, 2].map((stars) => (
                        <label key={stars} className="flex items-center gap-3 cursor-pointer group">
                            <div
                                onClick={() => toggle("categorie", stars)}
                                className={`w-4 h-4 rounded flex items-center justify-center border transition-all duration-150 flex-shrink-0 ${
                                    filters.categorie.includes(stars)
                                        ? "bg-[#0EA5E9] border-[#0EA5E9]"
                                        : "bg-white border-gray-200 group-hover:border-[#0EA5E9]/50"
                                }`}
                            >
                                {filters.categorie.includes(stars) && (
                                    <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
                                        <path d="M1 3.5L3.5 6L8 1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                                    </svg>
                                )}
                            </div>
                            <div className="flex items-center gap-2">
                                <EtoilesHotel categorie={stars} size="xs" />
                                <span className="text-xs text-gray-500 group-hover:text-gray-800 transition-colors">
                                    {stars === 5 ? "Luxe" : stars === 4 ? "Premium" : stars === 3 ? "Confort" : "Économique"}
                                </span>
                            </div>
                        </label>
                    ))}
                </div>
            </section>

            {/* Équipements */}
            <section className="space-y-3">
                <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-widest">
                    Équipements
                </p>
                <div className="space-y-2.5">
                    {EQUIPEMENTS_OPTIONS.map((eq) => (
                        <label key={eq.value} className="flex items-center gap-3 cursor-pointer group">
                            <div
                                onClick={() => toggle("equipements", eq.value)}
                                className={`w-4 h-4 rounded flex items-center justify-center border transition-all duration-150 flex-shrink-0 ${
                                    filters.equipements.includes(eq.value)
                                        ? "bg-[#0EA5E9] border-[#0EA5E9]"
                                        : "bg-white border-gray-200 group-hover:border-[#0EA5E9]/50"
                                }`}
                            >
                                {filters.equipements.includes(eq.value) && (
                                    <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
                                        <path d="M1 3.5L3.5 6L8 1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                                    </svg>
                                )}
                            </div>
                            <span className="text-sm text-gray-600 group-hover:text-gray-900 transition-colors">
                                {eq.label}
                            </span>
                        </label>
                    ))}
                </div>
            </section>

            {/* Actions */}
            <div className="pt-4 border-t border-gray-100 space-y-2">
                {/* Appliquer : visible et utile sur mobile, discret sur desktop */}
                {isMobile ? (
                    <button
                        onClick={apply}
                        className="w-full bg-[#0EA5E9] hover:bg-[#0284C7] text-white py-2.5 rounded-xl text-sm font-semibold transition-colors duration-200"
                    >
                        Appliquer
                    </button>
                ) : (
                    <p className="text-center text-xs text-gray-300 py-1">
                        Les filtres s'appliquent en temps réel
                    </p>
                )}
                <button
                    onClick={reset}
                    className="w-full py-2.5 rounded-xl text-sm text-gray-400 hover:text-gray-700 hover:bg-gray-50 transition-all duration-200"
                >
                    Réinitialiser
                </button>
            </div>
        </aside>
    );
}