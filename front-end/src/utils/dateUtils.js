export function toISO(date) {
    if (!date) return "";
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
}

export function formatRange(start, end) {
    const fmt = (d) =>
        new Intl.DateTimeFormat("fr-FR", {
            weekday: "short", day: "numeric", month: "short",
        }).format(d);
    if (!start) return "";
    if (!end) return `${fmt(start)} - ?`;
    return `${fmt(start)}  -  ${fmt(end)}`;
}