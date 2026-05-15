/**
 * ProtectedRoute.jsx
 * Composant qui protege une route en exigeant une authentification.
 *
 * Reecriture Lot 1 : utilise useAuth() au lieu de lire localStorage en direct.
 *
 * Le nom du prop est `roleRequired` (pas requiredRole) pour rester compatible
 * avec l'usage existant dans App.jsx.
 *
 * Usage dans App.jsx :
 *   <Route path="/admin" element={
 *     <ProtectedRoute roleRequired="ROLE_ADMIN">
 *       <AdminDashboard />
 *     </ProtectedRoute>
 *   } />
 *
 * Sans roleRequired : l'utilisateur doit juste etre connecte.
 * Avec roleRequired : l'utilisateur doit avoir ce role specifique.
 */

import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function ProtectedRoute({ children, roleRequired = null }) {
    const { isAuthenticated, isAdmin, isEmploye, loading } = useAuth();
    const location = useLocation();

    // Pendant la verification initiale du token au demarrage de l'app,
    // on affiche un placeholder. Sans ca, on redirige vers Connexion avant
    // meme d'avoir verifie si l'utilisateur etait connecte.
    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-gray-500">Chargement...</div>
            </div>
        );
    }

    if (!isAuthenticated) {
        // On garde l'URL d'origine pour pouvoir y revenir apres login
        return <Navigate to="/Connexion" replace state={{ from: location }} />;
    }

    if (roleRequired === "ROLE_ADMIN" && !isAdmin) {
        return <Navigate to="/" replace />;
    }

    if (roleRequired === "ROLE_EMPLOYE" && !isEmploye && !isAdmin) {
        return <Navigate to="/" replace />;
    }

    return children;
}