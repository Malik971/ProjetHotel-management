import { useState } from "react";

export default function Filter({ onFilterChange, onReset }) {
    const [filters, setFilters] = useState({
        prixMax: 50,
        categories: [],
        equipments: [],
        notationMin: 0,
    });

    const updateFilters = (values) => {
        setFilters((prev) => ({ ...prev, ...values }));
    };

    const toggleValue = (key, value) => {
        updateFilters({
            [key]: filters[key].includes(value)
                ? filters[key].filter((v) => v !== value)
                : [...filters[key], value],
        });
    };

    const applyFilters = () => {
        onFilterChange?.({
            prixMin: 20,
            ...filters,
        });
    };

    const resetFilters = () => {
        const reset = {
            prixMax: 50,
            categories: [],
            equipments: [],
            notationMin: 0,
        };
        setFilters(reset);
        onReset?.();
    };

    return (
        <aside className="w-80 bg-white rounded-2xl shadow-lg p-6 space-y-7 sticky top-6">
            <h2 className="text-xl font-semibold text-gray-800 border-b pb-4">
                Filtres
            </h2>

            {/* PRIX */}
            <section className="space-y-4">
                <p className="text-sm font-medium text-gray-700">
                    Prix par nuit
                </p>

                <div className="relative pt-8">
                    {/* Valeur au-dessus du curseur */}
                    <div
                        className="absolute -top-1 text-sm font-semibold text-blue-600"
                        style={{
                            left: `calc(${(filters.prixMax - 20) / 4.8}% - 14px)`,
                        }}
                    >
                        {filters.prixMax} €
                    </div>

                    <input
                        type="range"
                        min="20"
                        max="500"
                        value={filters.prixMax}
                        onChange={(e) =>
                            updateFilters({ prixMax: Number(e.target.value) })
                        }
                        className="w-full accent-blue-600"
                    />
                </div>

                <div className="flex justify-between text-xs text-gray-500">
                    <span>20 €</span>
                    <span>500 €</span>
                </div>
            </section>

            {/* NOTATION */}
            <section className="space-y-3">
                <p className="text-sm font-medium text-gray-700">
                    Catégorie
                </p>

                <div className="space-y-2">
                    {[5, 4, 3].map((stars) => (
                        <label
                            key={stars}
                            className="flex items-center gap-3 cursor-pointer text-sm"
                        >
                            <input
                                type="checkbox"
                                checked={filters.categories.includes(stars)}
                                onChange={() => toggleValue("categories", stars)}
                                className="accent-blue-600"
                            />

                            {/* ÉTOILES LUXE */}
                            <div className="flex">
                                {Array.from({ length: 5 }).map((_, i) => (
                                    <span
                                        key={i}
                                        className={`text-lg ${
                                            i < stars
                                                ? "text-amber-500 drop-shadow-sm"
                                                : "text-gray-300"
                                        }`}
                                    >
                    ★
                  </span>
                                ))}
                            </div>
                        </label>
                    ))}
                </div>
            </section>

            {/* ÉQUIPEMENTS */}
            <section className="space-y-3">
                <p className="text-sm font-medium text-gray-700">
                    Équipements
                </p>

                {[
                    { value: "wifi", label: "WiFi" },
                    { value: "restaurant", label: "Restaurant" },
                    { value: "parking", label: "Parking" },
                    { value: "piscine", label: "Piscine" },
                    { value: "spa", label: "Spa & bien-être" },
                ].map((equip) => (
                    <label
                        key={equip.value}
                        className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer"
                    >
                        <input
                            type="checkbox"
                            checked={filters.equipments.includes(equip.value)}
                            onChange={() => toggleValue("equipments", equip.value)}
                            className="accent-blue-600"
                        />
                        {equip.label}
                    </label>
                ))}
            </section>

            {/* ACTIONS */}
            <div className="pt-5 border-t space-y-3">
                <button
                    onClick={applyFilters}
                    className="w-full bg-blue-600 text-white py-3 rounded-xl font-medium hover:bg-blue-700 transition"
                >
                    Appliquer les filtres
                </button>

                <button
                    onClick={resetFilters}
                    className="w-full py-3 rounded-xl text-sm text-gray-600 hover:bg-gray-100 transition"
                >
                    Réinitialiser
                </button>
            </div>
        </aside>
    );
}
