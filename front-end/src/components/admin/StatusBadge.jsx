// src/components/admin/StatusBadge.jsx

/**
 * Badge de statut reutilisable dans tout l'admin Pastell.
 *
 * Mapping des statuts vers les couleurs (palette Lot 3) :
 *   OK          -> vert (succes)
 *   PENDING     -> bleu (en cours d'orchestration)
 *   EN_RETRY    -> ambre (relance automatique)
 *   EN_ERREUR   -> rouge (intervention requise)
 *   DIVERGENCE  -> rouge (anomalie metier)
 *
 * Les couleurs sont alignees sur ta palette : bleu primaire, ambre Pastell,
 * vert ECFDF5, rouge FEE2E2.
 */
export default function StatusBadge({ status }) {
    const config = getBadgeConfig(status);
    return (
        <span
            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${config.bg} ${config.text}`}
        >
            <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
            {config.label}
        </span>
    );
}

function getBadgeConfig(status) {
    switch (status) {
        case "OK":
            return {
                label: "OK",
                bg: "bg-emerald-50",
                text: "text-emerald-700",
                dot: "bg-emerald-500",
            };
        case "PENDING":
            return {
                label: "PENDING",
                bg: "bg-sky-50",
                text: "text-[#0369A1]",
                dot: "bg-[#0EA5E9]",
            };
        case "EN_RETRY":
            return {
                label: "EN_RETRY",
                bg: "bg-amber-50",
                text: "text-amber-800",
                dot: "bg-[#F59E0B]",
            };
        case "EN_ERREUR":
            return {
                label: "EN_ERREUR",
                bg: "bg-red-50",
                text: "text-red-800",
                dot: "bg-red-500",
            };
        case "DIVERGENCE":
            return {
                label: "DIVERGENCE",
                bg: "bg-red-50",
                text: "text-red-800",
                dot: "bg-red-500",
            };
        default:
            return {
                label: status || "INCONNU",
                bg: "bg-gray-50",
                text: "text-gray-700",
                dot: "bg-gray-400",
            };
    }
}