// src/components/BarRecherche.jsx
import { useState } from "react";
import { Search, MapPin, Calendar, Users } from "lucide-react";
import DateRangePicker from "./DateRangePicker";
import { toISO, formatRange } from "../utils/dateUtils";


export default function BarRecherche({ onSearch }) {
  const [destination, setDestination] = useState("");
  const [nombreVoyageurs, setNombreVoyageurs] = useState(1);
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  const [pickerOpen, setPickerOpen] = useState(false);

  // Champs dates natifs mobile
  const [mobileArrivee, setMobileArrivee] = useState("");
  const [mobileDepart, setMobileDepart] = useState("");

  const handleDateChange = ({ start, end }) => {
    setStartDate(start);
    setEndDate(end);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const isMobile = window.innerWidth < 768;
    if (onSearch) {
      onSearch({
        destination,
        dateArrivee: isMobile ? mobileArrivee : (startDate ? toISO(startDate) : ""),
        dateDepart: isMobile ? mobileDepart : (endDate ? toISO(endDate) : ""),
        nombreVoyageurs,
      });
    }
  };

  const rangeLabel = formatRange(startDate, endDate);

  return (
    <div className="relative w-full bg-gradient-to-br from-[#0EA5E9] via-[#0284C7] to-[#0369A1]">

      {/* Layer decoration : isole les debordements decoratifs sans contraindre le picker */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {/* Ligne deco haut */}
        <div className="absolute top-0 left-0 right-0 h-px bg-white/20" />

        {/* Cercles deco flous */}
        <div className="absolute -top-16 -right-16 w-64 h-64 bg-white/5 rounded-full" />
        <div className="absolute -bottom-10 -left-10 w-48 h-48 bg-white/5 rounded-full" />

        {/* Vague de transition (deplacee ici pour rester clippee) */}
        <div className="absolute bottom-0 left-0 right-0 leading-none">
          <svg viewBox="0 0 1440 32" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none" className="w-full h-8 block">
            <path d="M0,32 C360,0 1080,0 1440,32 L1440,32 L0,32 Z" fill="#F8FAFC" />
          </svg>
        </div>
      </div>

      <div className="relative z-10 px-6 py-14 md:py-20 flex flex-col items-center text-center">

        {/* Eyebrow */}
        <p className="text-[#BAE6FD] text-xs font-semibold tracking-[0.25em] uppercase mb-4">
          Collection prestige
        </p>

        {/* Heading */}
        <h1
          className="text-4xl md:text-5xl font-light text-white leading-tight mb-3"
          style={{ fontFamily: "'Georgia', 'Times New Roman', serif" }}
        >
          Trouvez votre
          <span className="block font-normal italic text-[#FDF8F0]">
            sejour d'exception
          </span>
        </h1>

        <p className="text-[#BAE6FD] text-sm mb-10 max-w-md leading-relaxed">
          Des etablissements selectionnes avec soin, pour des experiences inoubliables.
        </p>

        <form onSubmit={handleSubmit} className="w-full max-w-4xl">

          {/* ── DESKTOP ── */}
          <div className="hidden md:flex items-stretch bg-white rounded-2xl overflow-visible shadow-2xl shadow-[#0369A1]/30 relative">

            {/* Destination */}
            <div className="flex-1 flex items-center gap-3 px-5 py-4 border-r border-gray-100">
              <MapPin size={16} className="text-[#0EA5E9] flex-shrink-0" />
              <div className="flex flex-col w-full">
                <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                  Destination
                </label>
                <input
                  type="text"
                  value={destination}
                  onChange={(e) => setDestination(e.target.value)}
                  placeholder="Ville, region..."
                  className="bg-transparent text-gray-800 text-sm placeholder-gray-300 outline-none w-full"
                />
              </div>
            </div>

            {/* Dates : input declencheur du picker */}
            <div
              className="flex items-center gap-3 px-5 py-4 border-r border-gray-100 min-w-[220px] cursor-pointer select-none"
              onClick={() => setPickerOpen((v) => !v)}
            >
              <Calendar size={16} className={`flex-shrink-0 transition-colors ${pickerOpen ? "text-[#0369A1]" : "text-[#0EA5E9]"}`} />
              <div className="flex flex-col w-full">
                <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1 cursor-pointer">
                  Dates
                </label>
                <span className={`text-sm ${rangeLabel ? "text-gray-800" : "text-gray-300"}`}>
                  {rangeLabel || "Quand partez-vous ?"}
                </span>
              </div>
            </div>

            {/* Picker popover */}
            {pickerOpen && (
              <DateRangePicker
                startDate={startDate}
                endDate={endDate}
                onChange={handleDateChange}
                onClose={() => setPickerOpen(false)}
              />
            )}

            {/* Voyageurs */}
            <div className="flex items-center gap-3 px-5 py-4 border-r border-gray-100 min-w-[120px]">
              <Users size={16} className="text-[#0EA5E9] flex-shrink-0" />
              <div className="flex flex-col w-full">
                <label className="text-[10px] font-semibold text-[#0EA5E9] uppercase tracking-widest mb-1">
                  Voyageurs
                </label>
                <input
                  type="number"
                  value={nombreVoyageurs}
                  onChange={(e) => setNombreVoyageurs(Number(e.target.value))}
                  min="1"
                  className="bg-transparent text-gray-800 text-sm outline-none w-full"
                />
              </div>
            </div>

            {/* Bouton */}
            <button
              type="submit"
              className="flex items-center gap-2 bg-[#F59E0B] hover:bg-[#D97706] text-white font-semibold text-sm px-7 rounded-r-2xl transition-colors duration-200 whitespace-nowrap"
            >
              <Search size={16} />
              Rechercher
            </button>
          </div>

          {/* ── MOBILE : dates natives inchangees ── */}
          <div className="md:hidden flex flex-col gap-3">
            <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-3">
              <MapPin size={16} className="text-[#0EA5E9] flex-shrink-0" />
              <input
                type="text"
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                placeholder="Destination"
                className="bg-transparent text-gray-800 text-sm placeholder-gray-300 outline-none w-full"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-2">
                <Calendar size={14} className="text-[#0EA5E9] flex-shrink-0" />
                <input
                  type="date"
                  value={mobileArrivee}
                  onChange={(e) => setMobileArrivee(e.target.value)}
                  className="bg-transparent text-gray-800 text-sm outline-none w-full"
                />
              </div>
              <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-2">
                <Calendar size={14} className="text-[#0EA5E9] flex-shrink-0" />
                <input
                  type="date"
                  value={mobileDepart}
                  onChange={(e) => setMobileDepart(e.target.value)}
                  className="bg-transparent text-gray-800 text-sm outline-none w-full"
                />
              </div>
            </div>

            <div className="bg-white rounded-xl px-4 py-3 flex items-center gap-3">
              <Users size={16} className="text-[#0EA5E9] flex-shrink-0" />
              <input
                type="number"
                value={nombreVoyageurs}
                onChange={(e) => setNombreVoyageurs(Number(e.target.value))}
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
    </div>
  );
}