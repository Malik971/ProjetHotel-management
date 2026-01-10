// front-end/src/components/BarRecherche.jsx (version améliorée)
import { useState } from "react";

export default function BarRecherche({ onSearch }) {
  const [searchParams, setSearchParams] = useState({
    destination: "",
    dateArrivee: "",
    dateDepart: "",
    nombreVoyageurs: 1,
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setSearchParams((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSearch) {
      onSearch(searchParams);
    }
  };

  return (
    <div className="w-full bg-gradient-to-r from-blue-600 to-blue-800 text-white p-10 flex flex-col items-center">
      <h2 className="text-4xl font-bold mb-2">Trouvez votre hôtel idéal</h2>
      <p className="text-lg mb-8">Des milliers d'hôtels au meilleur prix</p>

      <form
        onSubmit={handleSubmit}
        className="bg-white w-[90%] max-w-5xl rounded-xl shadow-2xl p-6 grid grid-cols-4 gap-4 text-black"
      >
        <div className="flex flex-col">
          <label className="text-sm font-medium text-gray-700 mb-1">
            Destination
          </label>
          <input
            type="text"
            name="destination"
            value={searchParams.destination}
            onChange={handleChange}
            placeholder="Ville ou région"
            className="border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex flex-col">
          <label className="text-sm font-medium text-gray-700 mb-1">
            Arrivée
          </label>
          <input
            type="date"
            name="dateArrivee"
            value={searchParams.dateArrivee}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex flex-col">
          <label className="text-sm font-medium text-gray-700 mb-1">
            Départ
          </label>
          <input
            type="date"
            name="dateDepart"
            value={searchParams.dateDepart}
            onChange={handleChange}
            className="border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex flex-col">
          <label className="text-sm font-medium text-gray-700 mb-1">
            Voyageurs
          </label>
          <input
            type="number"
            name="nombreVoyageurs"
            value={searchParams.nombreVoyageurs}
            onChange={handleChange}
            min="1"
            className="border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <button
          type="submit"
          className="col-span-4 bg-black text-white py-3 rounded-lg hover:bg-gray-800 transition-all duration-300 font-semibold text-lg mt-2 shadow-lg hover:shadow-xl"
        >
          🔍 Rechercher
        </button>
      </form>
    </div>
  );
}