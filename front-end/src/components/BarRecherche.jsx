// src/components/BarRecherche.jsx
import { useState } from "react";
import { Search, MapPin, Calendar, Users } from "lucide-react";

export default function BarRecherche({ onSearch }) {
  const [searchParams, setSearchParams] = useState({
    destination: "",
    dateArrivee: "",
    dateDepart: "",
    nombreVoyageurs: 1,
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setSearchParams((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSearch) onSearch(searchParams);
  };

  return (
      <div className="relative w-full overflow-hidden bg-gradient-to-br from-[#0EA5E9] via-[#0284C7] to-[#0369A1]">
        {/* Ligne déco haut */}
        <div className="absolute top-0 left-0 right-0 h-px bg-white/20" />

        {/* Cercles déco flous */}
        <div className="absolute -top-16 -right-16 w-64 h-64 bg-white/5 rounded-full pointer-events-none" />
        <div className="absolute -bottom-10 -left-10 w-48 h-48 bg-white/5 rounded-full pointer-events-none" />

        <div className="relative z-10 px-6 py-14 md:py-20 flex flex-col items-center text-center">
          {/* Eyebrow */}
          <p className="text-[#BAE6FD] text-xs font-semibold tracking-[0.25em] uppercase mb-4">
            Collection prestige
          </p>

          {/* Heading */}
          <h1 className="text-4xl md:text-5xl font-light text-white leading-tight mb-3"
              style={{ fontFamily: "'Georgia', 'Times New Roman', serif" }}>
            Trouvez votre
            <span className="block font-normal italic text-[#FDF8F0]">
            séjour d'exception
          </span>
          </h1>

          <p className="text-[#BAE6FD] text-sm mb-10 max-w-md leading-relaxed">
            Des établissements sélectionnés avec soin, pour des expériences inoubliables.
          </p>

          {/* Formulaire desktop */}
          <form onSubmit={handleSubmit} className="w-full max-w-4xl">
            <div className="hidden md:flex items-stretch bg-white rounded-2xl overflow-hidden shadow-2xl shadow-[#0369A1]/30">

              {/* Destination */}
              <div className="flex-1 flex items-center gap-3 px-5 py-4 border-r border-gray-100">
                <MapPin size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <div className="flex flex-col w-full">
                  <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                    Destination
                  </label>
                  <input
                      type="text"
                      name="destination"
                      value={searchParams.destination}
                      onChange={handleChange}
                      placeholder="Ville, région..."
                      className="bg-transparent text-gray-800 text-sm placeholder-gray-300 outline-none w-full"
                  />
                </div>
              </div>

              {/* Arrivée */}
              <div className="flex items-center gap-3 px-5 py-4 border-r border-gray-100 min-w-[160px]">
                <Calendar size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <div className="flex flex-col w-full">
                  <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                    Arrivée
                  </label>
                  <input
                      type="date"
                      name="dateArrivee"
                      value={searchParams.dateArrivee}
                      onChange={handleChange}
                      className="bg-transparent text-gray-800 text-sm outline-none w-full"
                  />
                </div>
              </div>

              {/* Départ */}
              <div className="flex items-center gap-3 px-5 py-4 border-r border-gray-100 min-w-[160px]">
                <Calendar size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <div className="flex flex-col w-full">
                  <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                    Départ
                  </label>
                  <input
                      type="date"
                      name="dateDepart"
                      value={searchParams.dateDepart}
                      onChange={handleChange}
                      className="bg-transparent text-gray-800 text-sm outline-none w-full"
                  />
                </div>
              </div>

              {/* Voyageurs */}
              <div className="flex items-center gap-3 px-5 py-4 border-r border-gray-100 min-w-[120px]">
                <Users size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <div className="flex flex-col w-full">
                  <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                    Voyageurs
                  </label>
                  <input
                      type="number"
                      name="nombreVoyageurs"
                      value={searchParams.nombreVoyageurs}
                      onChange={handleChange}
                      min="1"
                      className="bg-transparent text-gray-800 text-sm outline-none w-full"
                  />
                </div>
              </div>

              {/* Bouton */}
              <button
                  type="submit"
                  className="flex items-center gap-2 bg-[#F59E0B] hover:bg-[#D97706] text-white font-semibold text-sm px-7 transition-colors duration-200 whitespace-nowrap"
              >
                <Search size={16} />
                Rechercher
              </button>
            </div>

            {/* Formulaire mobile */}
            <div className="md:hidden flex flex-col gap-3">
              <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-3">
                <MapPin size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <input
                    type="text"
                    name="destination"
                    value={searchParams.destination}
                    onChange={handleChange}
                    placeholder="Destination"
                    className="bg-transparent text-gray-800 text-sm placeholder-gray-300 outline-none w-full"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-2">
                  <Calendar size={14} className="text-[#0EA5E9] flex-shrink-0" />
                  <input
                      type="date"
                      name="dateArrivee"
                      value={searchParams.dateArrivee}
                      onChange={handleChange}
                      className="bg-transparent text-gray-800 text-sm outline-none w-full"
                  />
                </div>
                <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-2">
                  <Calendar size={14} className="text-[#0EA5E9] flex-shrink-0" />
                  <input
                      type="date"
                      name="dateDepart"
                      value={searchParams.dateDepart}
                      onChange={handleChange}
                      className="bg-transparent text-gray-800 text-sm outline-none w-full"
                  />
                </div>
              </div>

              <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-3">
                <Users size={16} className="text-[#0EA5E9] flex-shrink-0" />
                <input
                    type="number"
                    name="nombreVoyageurs"
                    value={searchParams.nombreVoyageurs}
                    onChange={handleChange}
                    min="1"
                    className="bg-transparent text-gray-800 text-sm outline-none w-full"
                />
              </div>

              <button
                  type="submit"
                  className="w-full bg-[#F59E0B] hover:bg-[#D97706] text-white font-semibold text-sm py-4 rounded-xl transition-colors duration-200 flex items-center justify-center gap-2"
              >
                <Search size={16} />
                Rechercher
              </button>
            </div>
          </form>
        </div>

        {/* Vague de transition vers le fond de page */}
        <div className="absolute bottom-0 left-0 right-0 overflow-hidden leading-none">
          <svg viewBox="0 0 1440 32" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none" className="w-full h-8 block">
            <path d="M0,32 C360,0 1080,0 1440,32 L1440,32 L0,32 Z" fill="#F8FAFC" />
          </svg>
        </div>
      </div>
  );
}