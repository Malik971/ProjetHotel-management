import { useState } from "react";
import { ChevronDown } from "lucide-react";

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
    categorie: [],       // ← clé unifiée
    equipements: [],     // ← clé unifiée (français, comme la BDD)
    notationMin: 0,
    tri: "",
};

export default function Filter({ onFilterChange, onReset }) {
    const [filters, setFilters] = useState(INITIAL);

    const update = (values) =>
        setFilters((prev) => ({ ...prev, ...values }));

    const toggle = (key, value) =>
        update({
            [key]: filters[key].includes(value)
                ? filters[key].filter((v) => v !== value)
                : [...filters[key], value],
        });

    const apply = () => onFilterChange?.(filters);

    const reset = () => {
        setFilters(INITIAL);
        onReset?.();
    };

    return (
        <aside className="w-72 bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6 sticky top-6 self-start">
            <h2 className="text-base font-semibold text-gray-900 border-b border-gray-100 pb-4">
                Filtres
            </h2>

            {/* ── TRI ── */}
            <section className="space-y-2">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                    Trier par
                </p>
                <div className="relative">
                    <select
                        value={filters.tri}
                        onChange={(e) => update({ tri: e.target.value })}
                        className="w-full appearance-none bg-gray-50 border border-gray-200 text-gray-700 text-sm rounded-xl px-3 py-2.5 pr-8 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
                    >
                        {TRI_OPTIONS.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.label}
                            </option>
                        ))}
                    </select>
                    <ChevronDown
                        size={14}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"
                    />
                </div>
            </section>

            {/* ── PRIX ── */}
            <section className="space-y-3">
                <div className="flex justify-between items-center">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                        Prix par nuit
                    </p>
                    <span className="text-sm font-bold text-blue-600">
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
                    className="w-full accent-blue-600"
                />
                <div className="flex justify-between text-xs text-gray-400">
                    <span>20 €</span>
                    <span>500 €</span>
                </div>
            </section>

            {/* ── CATÉGORIE (étoiles) ── */}
            <section className="space-y-2">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                    Catégorie
                </p>
                <div className="space-y-2">
                    {[5, 4, 3, 2].map((stars) => (
                        <label
                            key={stars}
                            className="flex items-center gap-3 cursor-pointer group"
                        >
                            <input
                                type="checkbox"
                                checked={filters.categorie.includes(stars)}
                                onChange={() => toggle("categorie", stars)}
                                className="accent-blue-600 w-4 h-4 rounded"
                            />
                            <div className="flex items-center gap-1">
                                {Array.from({ length: 5 }, (_, i) => (
                                    <span
                                        key={i}
                                        className={`text-base leading-none ${
                                            i < stars ? "text-amber-400" : "text-gray-200"
                                        }`}
                                    >
                    ★
                  </span>
                                ))}
                                <span className="text-xs text-gray-500 ml-1">
                  {stars === 5 ? "Luxe" : stars === 4 ? "Premium" : stars === 3 ? "Confort" : "Économique"}
                </span>
                            </div>
                        </label>
                    ))}
                </div>
            </section>

            {/* ── ÉQUIPEMENTS ── */}
            <section className="space-y-2">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                    Équipements
                </p>
                <div className="space-y-2">
                    {EQUIPEMENTS_OPTIONS.map((eq) => (
                        <label
                            key={eq.value}
                            className="flex items-center gap-3 cursor-pointer text-sm text-gray-700"
                        >
                            <input
                                type="checkbox"
                                checked={filters.equipements.includes(eq.value)}
                                onChange={() => toggle("equipements", eq.value)}
                                className="accent-blue-600 w-4 h-4 rounded"
                            />
                            {eq.label}
                        </label>
                    ))}
                </div>
            </section>

            {/* ── ACTIONS ── */}
            <div className="pt-4 border-t border-gray-100 space-y-2">
                <button
                    onClick={apply}
                    className="w-full bg-gray-900 text-white py-2.5 rounded-xl text-sm font-semibold hover:bg-gray-700 transition-colors"
                >
                    Appliquer
                </button>
                <button
                    onClick={reset}
                    className="w-full py-2.5 rounded-xl text-sm text-gray-500 hover:bg-gray-100 transition-colors"
                >
                    Réinitialiser
                </button>
            </div>
        </aside>
    );
}