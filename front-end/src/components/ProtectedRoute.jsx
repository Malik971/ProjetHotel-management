import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ children, roleRequired }) {
    const role = localStorage.getItem("role");

    // ❌ Pas connecté
    if (!role) {
        return <Navigate to="/Connexion" replace />;
    }

    // ❌ Mauvais rôle
    if (roleRequired && role !== roleRequired) {
        return <Navigate to="/" replace />;
    }

    // ✅ Autorisé
    return children;
}
