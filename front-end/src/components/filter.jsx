import { useState } from "react";

export default function Filter({ onFilterChange, onReset }) {
  const [filters, setFilters] = useState({
    prixMax: 500,
    categories: [],
    equipments: [],
    notationMin: 0,
  });

  const handlePriceChange = (value) => {
    const newFilters = { ...filters, prixMax: parseInt(value) };
    setFilters(newFilters);
  };

  const handleCategoryChange = (category) => {
    const newCategories = filters.categories.includes(category)
      ? filters.categories.filter((c) => c !== category)
      : [...filters.categories, category];

    const newFilters = { ...filters, categories: newCategories };
    setFilters(newFilters);
  };

  const handleEquipmentChange = (equipment) => {
    const newEquipements = filters.equipments.includes(equipment)
      ? filters.equipments.filter((e) => e !== equipment)
      : [...filters.equipments, equipment];

    const newFilters = { ...filters, equipments: newEquipements };
    setFilters(newFilters);
  };

  const handleApplyFilters = () => {
    if (onFilterChange) {
      onFilterChange({
        prixMin: 20,
        prixMax: filters.prixMax,
        categories: filters.categories,
        equipments: filters.equipments,
        notationMin: filters.notationMin,
      });
    }
  };

  const handleReset = () => {
    const resetFilters = {
      prixMax: 500,
      categories: [],
      equipments: [],
      notationMin: 0,
    };
    setFilters(resetFilters);

    // Appeler onReset pour réinitialiser les hôtels affichés
    if (onReset) {
      onReset();
    }
  };

  return (
    <div className="w-72 bg-white shadow-xl rounded-2xl p-6 space-y-6 sticky top-4 h-fit">
      <h2 className="text-2xl font-bold text-gray-800 border-b pb-3">
        Filtres
      </h2>

      {/* Prix */}
      <div className="space-y-3">
        <label className="font-semibold text-gray-700 flex items-center gap-2">
          💰 Prix par nuit
        </label>

        <div className="flex justify-between text-sm text-gray-500">
          <span>20€</span>
          <span>500€</span>
        </div>

        <input
          type="range"
          min="20"
          max="500"
          value={filters.prixMax}
          onChange={(e) => handlePriceChange(e.target.value)}
          className="w-full accent-blue-600 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        />

        <p className="text-center text-blue-600 font-bold text-lg bg-blue-50 py-2 rounded-lg">
          Jusqu'à {filters.prixMax} € / nuit
        </p>
      </div>

      {/* Notation */}
      <div className="space-y-3">
        <label className="font-semibold text-gray-700 flex items-center gap-2">
          ⭐ Notation minimale
        </label>

        <div className="flex gap-2">
          {[0, 3, 4, 4.5].map((rating) => (
            <button
              key={rating}
              onClick={() => setFilters({ ...filters, notationMin: rating })}
              className={`flex-1 py-2 rounded-lg font-medium transition ${
                filters.notationMin === rating
                  ? "bg-yellow-500 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              {rating === 0 ? "Tous" : `${rating}+`}
            </button>
          ))}
        </div>
      </div>

      {/* Catégorie */}
      <div className="space-y-3">
        <label className="font-semibold text-gray-700 flex items-center gap-2">
          🏨 Catégorie
        </label>

        <div className="flex flex-col space-y-2 text-gray-700">
          {[5, 4, 3].map((stars) => (
            <label
              key={stars}
              className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={filters.categories.includes(stars)}
                onChange={() => handleCategoryChange(stars)}
                className="h-5 w-5 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
              />
              <span className="flex items-center gap-1">
                {stars} {"⭐".repeat(stars)}
              </span>
            </label>
          ))}
        </div>
      </div>

      {/* Équipements */}
      <div className="space-y-3">
        <label className="font-semibold text-gray-700 flex items-center gap-2">
          🎯 Équipements
        </label>

        <div className="flex flex-col space-y-2 text-gray-700">
          {[
            { value: "wifi", label: "📶 Wifi" },
            { value: "restaurant", label: "🍽️ Restaurant" },
            { value: "parking", label: "🅿️ Parking" },
            { value: "piscine", label: "🏊 Piscine" },
            { value: "spa", label: "💆 Spa" },
          ].map((equipment) => (
            <label
              key={equipment.value}
              className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={filters.equipments.includes(equipment.value)}
                onChange={() => handleEquipmentChange(equipment.value)}
                className="h-5 w-5 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
              />
              <span>{equipment.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Boutons */}
      <div className="space-y-3 pt-4 border-t">
        <button
          onClick={handleApplyFilters}
          className="w-full bg-blue-600 text-white py-3 rounded-xl font-semibold hover:bg-blue-700 transition shadow-lg hover:shadow-xl"
        >
          ✓ Appliquer les filtres
        </button>

        <button
          onClick={handleReset}
          className="w-full bg-gray-100 py-3 rounded-xl text-gray-600 hover:bg-gray-200 transition font-medium"
        >
          ↺ Réinitialiser
        </button>
      </div>
    </div>
  );
}