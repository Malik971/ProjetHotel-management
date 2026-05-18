// src/components/admin/OrchestrationDiagram.jsx

/**
 * Schema SVG de l'architecture d'orchestration : Reservation -> Pastell ->
 * Parapheur / SAE / Notification.
 *
 * Affiche en permanence sur le dashboard admin pour communiquer en 3 secondes
 * que Pastell est positionne comme un BUS, pas comme un produit final isole.
 * Les destinations "futures" sont en gris pour montrer l'extensibilite.
 *
 * Mention "Inspire du bus applicatif Pastell de Libriciel SCOP" en bas pour
 * positionner clairement la reference metier.
 */
export default function OrchestrationDiagram() {
    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900">
                    Architecture d'orchestration
                </h3>
                <span className="text-xs text-gray-400">Bus Pastell</span>
            </div>

            <svg viewBox="0 0 360 180" className="w-full h-auto" role="img" aria-label="Schema du bus Pastell">
                {/* Source : Reservation Sejour */}
                <rect x="20" y="75" width="80" height="30" rx="6" fill="#E0F2FE" stroke="#0EA5E9" strokeWidth="1" />
                <text x="60" y="95" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="11" fill="#0369A1" fontWeight="500">
                    Reservation
                </text>

                {/* Fleche vers Pastell */}
                <line x1="100" y1="90" x2="135" y2="90" stroke="#94A3B8" strokeWidth="1" strokeDasharray="3,3" />
                <polygon points="135,87 141,90 135,93" fill="#94A3B8" />

                {/* Pastell : le bus, en ambre */}
                <rect x="141" y="65" width="78" height="50" rx="6" fill="#F59E0B" stroke="#B45309" strokeWidth="1" />
                <text x="180" y="85" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="11" fill="white" fontWeight="600">
                    Pastell
                </text>
                <text x="180" y="100" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="9" fill="#FEF3C7">
                    bus d'orchestration
                </text>

                {/* Fleche vers destinations */}
                <line x1="219" y1="90" x2="255" y2="90" stroke="#94A3B8" strokeWidth="1" strokeDasharray="3,3" />
                <polygon points="255,87 261,90 255,93" fill="#94A3B8" />

                {/* Destination 1 : Parapheur */}
                <rect x="261" y="30" width="80" height="28" rx="6" fill="#F1F5F9" stroke="#94A3B8" strokeWidth="1" />
                <text x="301" y="48" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="10" fill="#475569">
                    Parapheur
                </text>

                {/* Destination 2 : SAE */}
                <rect x="261" y="76" width="80" height="28" rx="6" fill="#F1F5F9" stroke="#94A3B8" strokeWidth="1" />
                <text x="301" y="94" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="10" fill="#475569">
                    Archivage SAE
                </text>

                {/* Destination 3 : Notification */}
                <rect x="261" y="122" width="80" height="28" rx="6" fill="#F1F5F9" stroke="#94A3B8" strokeWidth="1" />
                <text x="301" y="140" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="10" fill="#475569">
                    Notification
                </text>

                {/* Mention reference */}
                <text x="180" y="170" textAnchor="middle" fontFamily="system-ui, sans-serif" fontSize="9" fill="#94A3B8">
                    Inspire du bus applicatif Pastell de Libriciel SCOP
                </text>
            </svg>
        </div>
    );
}