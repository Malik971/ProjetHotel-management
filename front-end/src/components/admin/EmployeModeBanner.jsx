// src/components/admin/EmployeModeBanner.jsx
import { Eye, ShieldAlert } from "lucide-react";
import { useAuth } from "../../hooks/useAuth";

/**
 * Bandeau affiche en haut des pages d'administration lorsque l'utilisateur
 * connecte est un EMPLOYE (et pas un ADMIN).
 *
 * Objectif : rendre visible la separation des pouvoirs. L'employe accede a tout
 * l'espace admin en lecture et modification, mais les suppressions lui sont
 * refusees (boutons desactives cote frontend + refus serveur cote SecurityConfig).
 *
 * Ne rend rien pour :
 *   - un administrateur (isAdmin === true) : il a tous les droits, pas de bandeau,
 *   - un visiteur non staff : il n'atteint pas ces pages (ProtectedRoute).
 */
export default function EmployeModeBanner() {
    const { isEmploye, isAdmin } = useAuth();

    if (!isEmploye || isAdmin) return null;

    return (
        <div
            role="status"
            className="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 mb-6 shadow-sm"
        >
            <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-100">
                <Eye size={16} className="text-amber-700" />
            </span>
            <div className="min-w-0">
                <p className="flex items-center gap-1.5 text-sm font-semibold text-amber-900">
                    <ShieldAlert size={14} className="text-amber-600" />
                    Mode employé - accès en consultation et modification
                </p>
                <p className="text-xs text-amber-800 mt-0.5 leading-relaxed">
                    Vous explorez l'espace d'administration en tant qu'employé. Vous pouvez
                    tout consulter, créer et modifier, mais les <strong>suppressions</strong> sont
                    réservées aux administrateurs. Connectez-vous avec un compte administrateur
                    pour disposer de ces droits.
                </p>
            </div>
        </div>
    );
}
