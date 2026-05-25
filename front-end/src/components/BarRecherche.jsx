// src/components/BarRecherche.jsx
import { useState, useEffect, useRef, useCallback } from "react";
import { Search, MapPin, Calendar, Users, ChevronLeft, ChevronRight } from "lucide-react";

// ─────────────────────────────────────────────────────────────────────────────
// Utilitaires date (pas de dependance externe, tout en vanilla JS)
// ─────────────────────────────────────────────────────────────────────────────

const JOURS = ["Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"];

const MOIS_FR = [
  "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
  "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre",
];

function toISO(date) {
  if (!date) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function isSameDay(a, b) {
  if (!a || !b) return false;
  return toISO(a) === toISO(b);
}

function isBetween(date, start, end) {
  if (!date || !start || !end) return false;
  const d = toISO(date);
  return d > toISO(start) && d < toISO(end);
}

function isBeforeToday(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date < today;
}

function addMonths(date, n) {
  const d = new Date(date);
  d.setDate(1);
  d.setMonth(d.getMonth() + n);
  return d;
}

function getDaysInMonth(year, month) {
  return new Date(year, month + 1, 0).getDate();
}

// Premier jour de la semaine du mois (0=Di → on convertit en lundi=0)
function getFirstDayOfWeek(year, month) {
  const day = new Date(year, month, 1).getDay();
  return day === 0 ? 6 : day - 1;
}

function formatRange(start, end) {
  const fmt = (d) =>
    new Intl.DateTimeFormat("fr-FR", {
      weekday: "short",
      day: "numeric",
      month: "short",
    }).format(d);
  if (!start) return "";
  if (!end) return `${fmt(start)} - ?`;
  return `${fmt(start)}  —  ${fmt(end)}`;
}

// ─────────────────────────────────────────────────────────────────────────────
// Composant grille d'un mois
// ─────────────────────────────────────────────────────────────────────────────

function MonthGrid({ year, month, startDate, endDate, hoverDate, onDayClick, onDayHover }) {
  const firstDow = getFirstDayOfWeek(year, month);
  const daysInMonth = getDaysInMonth(year, month);
  const cells = [];

  // Cases vides avant le 1er
  for (let i = 0; i < firstDow; i++) {
    cells.push(null);
  }
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push(new Date(year, month, d));
  }

  // Plage effective pour le surlignage (utilise hoverDate si depart pas encore choisi)
  const rangeEnd = endDate || hoverDate;

  return (
    <div className="flex-1 min-w-0">
      <p className="text-sm font-semibold text-gray-800 text-center mb-3">
        {MOIS_FR[month]} {year}
      </p>

      {/* Entetes jours */}
      <div className="grid grid-cols-7 mb-1">
        {JOURS.map((j) => (
          <div key={j} className="text-center text-[10px] font-semibold text-gray-400 uppercase py-1">
            {j}
          </div>
        ))}
      </div>

      {/* Grille */}
      <div className="grid grid-cols-7">
        {cells.map((date, idx) => {
          if (!date) {
            return <div key={`empty-${idx}`} />;
          }

          const isPast = isBeforeToday(date);
          const isStart = isSameDay(date, startDate);
          const isEnd = isSameDay(date, rangeEnd) && endDate;
          const isHoverEnd = isSameDay(date, hoverDate) && !endDate && startDate;
          const inRange = startDate && rangeEnd && isBetween(date, startDate, rangeEnd);
          const isToday = isSameDay(date, new Date());

          // Fond de trainee : couvre toute la cellule sauf les bords gauche/droit
          const trailBg =
            inRange
              ? "bg-[#E0F2FE]"
              : isStart && (endDate || (hoverDate && !isSameDay(hoverDate, startDate)))
              ? "bg-[#E0F2FE] rounded-l-full"
              : "";

          // Arrondi gauche ou droit de la trainee aux bornes
          const trailRound =
            inRange && isStart ? "rounded-l-full" :
            inRange && (isEnd || isHoverEnd) ? "rounded-r-full" : "";

          return (
            <div
              key={toISO(date)}
              className={`relative flex items-center justify-center h-9 ${trailBg} ${trailRound} transition-colors duration-100`}
              onMouseEnter={() => !isPast && onDayHover(date)}
              onMouseLeave={() => onDayHover(null)}
              onClick={() => !isPast && onDayClick(date)}
            >
              <span
                className={[
                  "relative z-10 w-8 h-8 flex items-center justify-center rounded-full text-sm transition-all duration-150 select-none",
                  isPast
                    ? "text-gray-300 cursor-not-allowed"
                    : "cursor-pointer",
                  isStart || isEnd
                    ? "bg-[#0EA5E9] text-white font-semibold shadow-md shadow-[#0EA5E9]/30"
                    : isHoverEnd
                    ? "bg-[#0EA5E9]/70 text-white font-medium"
                    : inRange
                    ? "text-[#0369A1] font-medium"
                    : isToday
                    ? "text-[#0EA5E9] font-semibold underline decoration-[#0EA5E9] underline-offset-2"
                    : !isPast
                    ? "text-gray-700 hover:bg-gray-100"
                    : "",
                ].join(" ")}
              >
                {date.getDate()}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Composant DateRangePicker (popover)
// ─────────────────────────────────────────────────────────────────────────────

function DateRangePicker({ startDate, endDate, onChange, onClose }) {
  const today = new Date();
  const [leftMonth, setLeftMonth] = useState(new Date(today.getFullYear(), today.getMonth(), 1));
  const [hoverDate, setHoverDate] = useState(null);
  const ref = useRef(null);

  const rightMonth = addMonths(leftMonth, 1);

  // Fermeture au clic exterieur
  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [onClose]);

  const handleDayClick = useCallback(
    (date) => {
      if (!startDate || (startDate && endDate)) {
        // Pas de selection ou selection complete : on repart
        onChange({ start: date, end: null });
      } else {
        // startDate choisie, pas encore endDate
        if (toISO(date) < toISO(startDate)) {
          // Clic avant le debut : le nouveau clic devient le debut
          onChange({ start: date, end: null });
        } else if (isSameDay(date, startDate)) {
          // Meme jour : on reset
          onChange({ start: null, end: null });
        } else {
          onChange({ start: startDate, end: date });
          onClose();
        }
      }
    },
    [startDate, endDate, onChange, onClose]
  );

  const prevMonth = () => setLeftMonth((m) => addMonths(m, -1));
  const nextMonth = () => setLeftMonth((m) => addMonths(m, 1));

  // Desactiver la fleche gauche si mois courant = mois actuel
  const canGoPrev =
    leftMonth.getFullYear() > today.getFullYear() ||
    leftMonth.getMonth() > today.getMonth();

  return (
    <div
      ref={ref}
      className="absolute top-full left-1/2 -translate-x-1/2 mt-3 z-50
                 bg-white rounded-2xl shadow-2xl shadow-gray-300/50 border border-gray-100
                 p-5 w-[640px] max-w-[95vw]"
      style={{ animation: "fadeInDown 0.15s ease" }}
    >
      <style>{`
        @keyframes fadeInDown {
          from { opacity: 0; transform: translateX(-50%) translateY(-8px); }
          to   { opacity: 1; transform: translateX(-50%) translateY(0); }
        }
      `}</style>

      {/* Navigation */}
      <div className="flex items-center justify-between mb-4">
        <button
          type="button"
          onClick={prevMonth}
          disabled={!canGoPrev}
          className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <ChevronLeft size={16} className="text-gray-600" />
        </button>

        <button
          type="button"
          onClick={nextMonth}
          className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors"
        >
          <ChevronRight size={16} className="text-gray-600" />
        </button>
      </div>

      {/* Deux mois cote a cote */}
      <div className="flex gap-6">
        <MonthGrid
          year={leftMonth.getFullYear()}
          month={leftMonth.getMonth()}
          startDate={startDate}
          endDate={endDate}
          hoverDate={hoverDate}
          onDayClick={handleDayClick}
          onDayHover={setHoverDate}
        />
        <div className="w-px bg-gray-100 self-stretch" />
        <MonthGrid
          year={rightMonth.getFullYear()}
          month={rightMonth.getMonth()}
          startDate={startDate}
          endDate={endDate}
          hoverDate={hoverDate}
          onDayClick={handleDayClick}
          onDayHover={setHoverDate}
        />
      </div>

      {/* Indication contextuelle en bas */}
      <p className="text-xs text-gray-400 text-center mt-4">
        {!startDate
          ? "Selectionnez votre date d'arrivee"
          : !endDate
          ? "Selectionnez votre date de depart"
          : `${Math.round((endDate - startDate) / 86400000)} nuit${Math.round((endDate - startDate) / 86400000) > 1 ? "s" : ""} selectionnee${Math.round((endDate - startDate) / 86400000) > 1 ? "s" : ""}`}
      </p>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Composant principal BarRecherche
// ─────────────────────────────────────────────────────────────────────────────

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