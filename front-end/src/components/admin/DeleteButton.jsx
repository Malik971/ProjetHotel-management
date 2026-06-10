// src/components/admin/DeleteButton.jsx
import { Trash2, Lock } from "lucide-react";
import { toast } from "sonner";

/**
 * Bouton de suppression conscient de la separation des pouvoirs.
 *
 * - Administrateur (canDelete === true) : bouton rouge actif, declenche onDelete.
 * - Employe (canDelete === false) : le bouton reste VISIBLE mais grise et non
 *   actionnable (cadenas a la place de la corbeille). Un clic n'effectue aucune
 *   suppression : il affiche un message expliquant que l'action est reservee aux
 *   administrateurs. C'est un choix volontaire de demonstration : montrer la
 *   separation des pouvoirs plutot que de masquer le bouton.
 *
 * Defense en profondeur : meme si l'employe forcait l'appel, le backend refuse
 * tout DELETE non-ADMIN (voir SecurityConfig). Ce composant est la couche visuelle.
 *
 * Deux variantes d'affichage pour coller aux tableaux existants :
 *   - "icon"    : petit bouton icone (tableaux hotels et chambres),
 *   - "labeled" : bouton avec libelle "Supprimer" (tableau utilisateurs).
 */
const BLOCKED_MESSAGE =
    "Suppression réservée aux administrateurs. Vous êtes connecté en tant qu'employé : consultation et modification autorisées, pas de suppression.";

export default function DeleteButton({
    canDelete,
    onDelete,
    variant = "icon",
    title = "Supprimer",
}) {
    const handleBlocked = () => toast.warning(BLOCKED_MESSAGE);

    if (variant === "labeled") {
        if (canDelete) {
            return (
                <button
                    onClick={onDelete}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-red-200 text-red-600 hover:bg-red-50 hover:border-red-300 transition"
                >
                    <Trash2 size={12} />
                    Supprimer
                </button>
            );
        }
        return (
            <button
                onClick={handleBlocked}
                aria-disabled="true"
                title="Réservé aux administrateurs"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-gray-50 border border-gray-200 text-gray-400 cursor-not-allowed transition"
            >
                <Lock size={12} />
                Supprimer
            </button>
        );
    }

    // variant "icon"
    if (canDelete) {
        return (
            <button
                onClick={onDelete}
                className="p-1.5 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                title={title}
            >
                <Trash2 size={14} />
            </button>
        );
    }
    return (
        <button
            onClick={handleBlocked}
            aria-disabled="true"
            title="Suppression réservée aux administrateurs"
            className="p-1.5 text-gray-300 bg-gray-50 rounded-lg cursor-not-allowed transition-colors"
        >
            <Lock size={14} />
        </button>
    );
}
