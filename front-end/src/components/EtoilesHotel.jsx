// src/components/EtoilesHotel.jsx
// Composant universel d'affichage des étoiles — à utiliser sur TOUT le site

export default function EtoilesHotel({ categorie, size = "sm" }) {
    if (!categorie) return null;

    const sizeClass = {
        xs: "text-xs",
        sm: "text-sm",
        md: "text-base",
        lg: "text-lg",
    }[size] || "text-sm";

    return (
        <div className="flex items-center gap-0.5">
            {Array.from({ length: 5 }, (_, i) => (
                <span
                    key={i}
                    className={`${sizeClass} leading-none ${
                        i < categorie ? "text-[#F59E0B]" : "text-gray-200"
                    }`}
                >
          ★
        </span>
            ))}
        </div>
    );
}