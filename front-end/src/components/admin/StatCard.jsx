// src/components/admin/StatCard.jsx

/**
 * Carte de compteur reutilisable pour le dashboard admin.
 *
 * Props :
 *   - icon      : composant icone lucide
 *   - iconColor : classe Tailwind de couleur de l'icone
 *   - label     : libelle en majuscules (ex: "Dossiers OK")
 *   - value     : valeur numerique
 *   - hint      : texte secondaire en gris
 */
export default function StatCard({ icon: Icon, iconColor, label, value, hint }) {
    return (
        <div className="bg-white border border-gray-200 rounded-2xl p-4 md:p-5 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
                {Icon && <Icon size={16} className={iconColor} />}
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
                    {label}
                </p>
            </div>
            <p className="text-2xl md:text-3xl font-semibold text-gray-900 leading-none">
                {value}
            </p>
            {hint && (
                <p className="text-xs text-gray-400 mt-2">{hint}</p>
            )}
        </div>
    );
}