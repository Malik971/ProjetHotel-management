// src/components/DateRangePicker.jsx
import { useState, useEffect, useRef, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { toISO } from "../utils/dateUtils";

// ─── Utilitaires date (no-deps) ────────────────────────────────────────────

const JOURS = ["Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di"];
const MOIS_FR = [
    "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre",
];

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

function getFirstDayOfWeek(year, month) {
    const day = new Date(year, month, 1).getDay();
    return day === 0 ? 6 : day - 1;
}

// ─── Grille d'un mois ─────────────────────────────────────────────────────

function MonthGrid({ year, month, startDate, endDate, hoverDate, onDayClick, onDayHover }) {
    const firstDow = getFirstDayOfWeek(year, month);
    const daysInMonth = getDaysInMonth(year, month);
    const cells = [];
    for (let i = 0; i < firstDow; i++) cells.push(null);
    for (let d = 1; d <= daysInMonth; d++) cells.push(new Date(year, month, d));

    const rangeEnd = endDate || hoverDate;

    return (
        <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-gray-800 text-center mb-3">
                {MOIS_FR[month]} {year}
            </p>
            <div className="grid grid-cols-7 mb-1">
                {JOURS.map((j) => (
                    <div key={j} className="text-center text-[10px] font-semibold text-gray-400 uppercase py-1">{j}</div>
                ))}
            </div>
            <div className="grid grid-cols-7">
                {cells.map((date, idx) => {
                    if (!date) return <div key={`empty-${idx}`} />;
                    const isPast = isBeforeToday(date);
                    const isStart = isSameDay(date, startDate);
                    const isEnd = isSameDay(date, rangeEnd) && endDate;
                    const isHoverEnd = isSameDay(date, hoverDate) && !endDate && startDate;
                    const inRange = startDate && rangeEnd && isBetween(date, startDate, rangeEnd);
                    const isToday = isSameDay(date, new Date());

                    const trailBg = inRange ? "bg-[#E0F2FE]" :
                        isStart && (endDate || (hoverDate && !isSameDay(hoverDate, startDate))) ? "bg-[#E0F2FE] rounded-l-full" : "";
                    const trailRound = inRange && isStart ? "rounded-l-full" :
                        inRange && (isEnd || isHoverEnd) ? "rounded-r-full" : "";

                    return (
                        <div key={toISO(date)}
                            className={`relative flex items-center justify-center h-9 ${trailBg} ${trailRound} transition-colors duration-100`}
                            onMouseEnter={() => !isPast && onDayHover(date)}
                            onMouseLeave={() => onDayHover(null)}
                            onClick={() => !isPast && onDayClick(date)}>
                            <span className={[
                                "relative z-10 w-8 h-8 flex items-center justify-center rounded-full text-sm transition-all duration-150 select-none",
                                isPast ? "text-gray-300 cursor-not-allowed" : "cursor-pointer",
                                isStart || isEnd ? "bg-[#0EA5E9] text-white font-semibold shadow-md shadow-[#0EA5E9]/30" :
                                isHoverEnd ? "bg-[#0EA5E9]/70 text-white font-medium" :
                                inRange ? "text-[#0369A1] font-medium" :
                                isToday ? "text-[#0EA5E9] font-semibold underline decoration-[#0EA5E9] underline-offset-2" :
                                !isPast ? "text-gray-700 hover:bg-gray-100" : "",
                            ].join(" ")}>
                                {date.getDate()}
                            </span>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

// ─── DateRangePicker (popover positionne par le parent) ───────────────────

export default function DateRangePicker({ startDate, endDate, onChange, onClose, position = "center" }) {
    const today = new Date();
    const [leftMonth, setLeftMonth] = useState(new Date(today.getFullYear(), today.getMonth(), 1));
    const [hoverDate, setHoverDate] = useState(null);
    const ref = useRef(null);
    const rightMonth = addMonths(leftMonth, 1);

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) onClose();
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, [onClose]);

    const handleDayClick = useCallback((date) => {
        if (!startDate || (startDate && endDate)) {
            onChange({ start: date, end: null });
        } else if (toISO(date) < toISO(startDate)) {
            onChange({ start: date, end: null });
        } else if (isSameDay(date, startDate)) {
            onChange({ start: null, end: null });
        } else {
            onChange({ start: startDate, end: date });
            onClose();
        }
    }, [startDate, endDate, onChange, onClose]);

    const prevMonth = () => setLeftMonth((m) => addMonths(m, -1));
    const nextMonth = () => setLeftMonth((m) => addMonths(m, 1));
    const canGoPrev = leftMonth.getFullYear() > today.getFullYear() || leftMonth.getMonth() > today.getMonth();

    // Positionnement : "center" pour la barre de recherche, "left" pour les cards
    const positionClass = position === "left"
        ? "left-0"
        : "left-1/2 -translate-x-1/2";

    return (
        <div ref={ref}
            className={`absolute top-full mt-3 z-50 ${positionClass} bg-white rounded-2xl shadow-2xl shadow-gray-300/50 border border-gray-100 p-5 w-[640px] max-w-[95vw]`}
            style={{ animation: "fadeInDown 0.15s ease" }}>
            <style>{`
                @keyframes fadeInDown {
                    from { opacity: 0; transform: ${position === "left" ? "translateY(-8px)" : "translateX(-50%) translateY(-8px)"}; }
                    to   { opacity: 1; transform: ${position === "left" ? "translateY(0)" : "translateX(-50%) translateY(0)"}; }
                }
            `}</style>

            <div className="flex items-center justify-between mb-4">
                <button type="button" onClick={prevMonth} disabled={!canGoPrev}
                    className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                    <ChevronLeft size={16} className="text-gray-600" />
                </button>
                <button type="button" onClick={nextMonth}
                    className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 transition-colors">
                    <ChevronRight size={16} className="text-gray-600" />
                </button>
            </div>

            <div className="flex gap-6">
                <MonthGrid year={leftMonth.getFullYear()} month={leftMonth.getMonth()}
                    startDate={startDate} endDate={endDate} hoverDate={hoverDate}
                    onDayClick={handleDayClick} onDayHover={setHoverDate} />
                <div className="w-px bg-gray-100 self-stretch" />
                <MonthGrid year={rightMonth.getFullYear()} month={rightMonth.getMonth()}
                    startDate={startDate} endDate={endDate} hoverDate={hoverDate}
                    onDayClick={handleDayClick} onDayHover={setHoverDate} />
            </div>

            <p className="text-xs text-gray-400 text-center mt-4">
                {!startDate ? "Selectionnez votre date d'arrivee" :
                 !endDate ? "Selectionnez votre date de depart" :
                 `${Math.round((endDate - startDate) / 86400000)} nuit${Math.round((endDate - startDate) / 86400000) > 1 ? "s" : ""} selectionnee${Math.round((endDate - startDate) / 86400000) > 1 ? "s" : ""}`}
            </p>
        </div>
    );
}