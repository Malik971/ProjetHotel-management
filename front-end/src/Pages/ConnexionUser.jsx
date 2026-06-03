// src/Pages/ConnexionUser.jsx
import { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Mail, Lock, Eye, EyeOff, ArrowLeft } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

/**
 * Page de connexion.
 *
 * Evolution Lot K4 : deux modes de connexion coexistent sur la meme page.
 *
 * Mode 1 : JWT maison (formulaire email + mot de passe).
 *   Appelle login(email, password) depuis AuthContext.
 *   Identique au comportement pre-Keycloak.
 *
 * Mode 2 : Keycloak PKCE (bouton "Se connecter avec Keycloak").
 *   Appelle loginWithKeycloak() depuis AuthContext.
 *   Redirige le navigateur vers Keycloak, qui redirige en retour vers cette
 *   meme page avec ?code=... dans l'URL.
 *   Le useEffect dans AuthProvider detecte ce code et l'echange contre un token.
 *   Une fois le token stocke, AuthProvider appelle /api/me et pose le user
 *   dans le context. Ce composant detecte alors isAuthenticated et redirige.
 *
 * Le bouton Keycloak est place apres le separateur existant, a la place
 * du lien "Pas encore de compte ?" qui est deplace en dessous.
 */
export default function ConnexionUser() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [keycloakLoading, setKeycloakLoading] = useState(false);
    const [error, setError] = useState("");

    const { login, loginWithKeycloak, isAuthenticated, isAdmin, loading: authLoading } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const redirectAfterLogin = location.state?.from?.pathname || "/";

    /**
     * Redirection automatique apres que AuthProvider a traite le callback Keycloak.
     * Quand AuthProvider pose le user (isAuthenticated passe a true), on redirige.
     * Sans ce useEffect, l'utilisateur resterait sur /Connexion apres le retour Keycloak.
     */
    useEffect(() => {
        if (!authLoading && isAuthenticated) {
            const roles = JSON.parse(localStorage.getItem("sejour_roles") || "[]");
            const userIsAdmin = isAdmin || roles.includes("ROLE_ADMIN");
            const target =
                redirectAfterLogin === "/" && userIsAdmin ? "/admin" : redirectAfterLogin;
            navigate(target, { replace: true });
        }
    }, [isAuthenticated, authLoading, isAdmin, navigate, redirectAfterLogin]);

    /**
     * Soumission du formulaire JWT maison.
     * Inchange par rapport au Lot 1.
     */
    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        const success = await login(email, password);

        if (success) {
            const roles = JSON.parse(localStorage.getItem("sejour_roles") || "[]");
            const userIsAdmin = roles.includes("ROLE_ADMIN");
            const target =
                redirectAfterLogin === "/" && userIsAdmin ? "/admin" : redirectAfterLogin;
            navigate(target, { replace: true });
        } else {
            setError("Identifiants incorrects");
        }

        setLoading(false);
    };

    /**
     * Connexion via Keycloak PKCE.
     * Appelle loginWithKeycloak() qui redirige le navigateur.
     * Le loading reste actif jusqu'a la redirection (pas de retour possible).
     */
    const handleKeycloakLogin = async () => {
        setKeycloakLoading(true);
        // La fonction redirige le navigateur, setKeycloakLoading(false) ne s'execute pas.
        // On le garde quand meme pour le cas ou la redirection echoue.
        try {
            await loginWithKeycloak('openid pastell-admin');
        } catch {
            setKeycloakLoading(false);
            setError("Impossible de contacter Keycloak. Verifiez que le serveur est demarre.");
        }
    };

    return (
        <div className="min-h-screen bg-[#F8FAFC] flex flex-col">

            {/* Header minimal avec retour */}
            <div className="w-full px-4 md:px-8 py-5">
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors"
                >
                    <ArrowLeft size={16} />
                    Retour à l'accueil
                </Link>
            </div>

            {/* Contenu centré */}
            <div className="flex-1 flex items-center justify-center px-4 py-8">
                <div className="w-full max-w-md">

                    {/* Logo + titre */}
                    <div className="text-center mb-8">
                        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-linear-to-br from-[#0EA5E9] to-[#0369A1] mb-4 shadow-lg shadow-[#0EA5E9]/30">
                            <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
                                <path d="M3 16C3 16 5 14 7 14C9 14 10 16 12 16C14 16 15 14 17 14C19 14 21 16 21 16" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                <path d="M3 20C3 20 5 18 7 18C9 18 10 20 12 20C14 20 15 18 17 18C19 18 21 20 21 20" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                <circle cx="12" cy="7" r="3" stroke="white" strokeWidth="2" fill="#F59E0B"/>
                            </svg>
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900 mb-1">Bon retour parmi nous</h1>
                        <p className="text-gray-500 text-sm">Connectez-vous pour gérer vos réservations</p>
                    </div>

                    {/* Carte principale */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-7 shadow-sm">

                        {/* Formulaire JWT maison */}
                        <form onSubmit={handleLogin} className="space-y-5">

                            {/* Email */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Email
                                </label>
                                <div className="relative">
                                    <Mail size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                        placeholder="votre@email.com"
                                        className="w-full pl-10 pr-4 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                </div>
                            </div>

                            {/* Mot de passe */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Mot de passe
                                </label>
                                <div className="relative">
                                    <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                    <input
                                        type={showPassword ? "text" : "password"}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                        placeholder="••••••••"
                                        className="w-full pl-10 pr-11 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#0EA5E9]"
                                    >
                                        {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                                    </button>
                                </div>
                            </div>

                            {/* Message d'erreur */}
                            {error && (
                                <div className="bg-red-50 border border-red-100 text-red-600 text-xs p-3 rounded-xl">
                                    {error}
                                </div>
                            )}

                            {/* Bouton JWT maison */}
                            <button
                                type="submit"
                                disabled={loading || keycloakLoading}
                                className="w-full bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-300 text-white py-3 rounded-xl font-semibold text-sm transition-colors shadow-sm"
                            >
                                {loading ? "Connexion..." : "Se connecter"}
                            </button>
                        </form>

                        {/* Separateur */}
                        <div className="flex items-center gap-3 my-6">
                            <div className="flex-1 h-px bg-gray-100" />
                            <span className="text-xs text-gray-300 uppercase tracking-wider">ou</span>
                            <div className="flex-1 h-px bg-gray-100" />
                        </div>

                        {/* Bouton Keycloak */}
                        <button
                            type="button"
                            onClick={handleKeycloakLogin}
                            disabled={loading || keycloakLoading}
                            className="w-full flex items-center justify-center gap-3 bg-white hover:bg-gray-50 disabled:bg-gray-100 border border-gray-200 hover:border-[#0EA5E9] text-gray-700 py-3 rounded-xl font-semibold text-sm transition-all shadow-sm"
                        >
                            {/* Icone Keycloak : cadenas ouvert, symbole du SSO */}
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M12 2C8.13 2 5 5.13 5 9v1H4c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2h-1V9c0-3.87-3.13-7-7-7zm0 2c2.76 0 5 2.24 5 5v1H7V9c0-2.76 2.24-5 5-5zm0 9c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2z" fill="#0EA5E9"/>
                            </svg>
                            {keycloakLoading ? "Redirection vers Keycloak..." : "Se connecter avec Keycloak"}
                        </button>

                        {/* Note explicative pour les comptes Keycloak */}
                        <p className="text-center text-xs text-gray-400 mt-3">
                            Comptes Keycloak locaux : admin-demo / user-demo
                        </p>

                        {/* Separateur inscription */}
                        <div className="flex items-center gap-3 my-4">
                            <div className="flex-1 h-px bg-gray-100" />
                        </div>

                        {/* Lien inscription */}
                        <p className="text-center text-sm text-gray-500">
                            Pas encore de compte ?{" "}
                            <Link
                                to="/Inscription"
                                className="text-[#0EA5E9] font-semibold hover:text-[#0284C7] transition-colors"
                            >
                                Créer un compte
                            </Link>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}