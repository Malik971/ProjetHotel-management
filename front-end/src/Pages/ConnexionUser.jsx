// src/Pages/ConnexionUser.jsx
import { useState, useEffect } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Mail, Lock, Eye, EyeOff, ArrowLeft } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

/**
 * Page de connexion.
 *
 * Trois modes de connexion coexistent sur la meme page.
 *
 * Mode 1 : JWT maison (formulaire email + mot de passe).
 *   Appelle login(email, password) depuis AuthContext.
 *
 * Mode 2 : Keycloak PKCE (bouton "Se connecter avec Keycloak").
 *   Appelle loginWithKeycloak() qui redirige vers la page de login Keycloak.
 *   Sur cette page, les fournisseurs externes configures (Google) apparaissent
 *   aussi, l'utilisateur a donc le choix.
 *
 * Mode 3 : Google direct (bouton "Continuer avec Google").
 *   Appelle loginWithKeycloak('openid pastell-admin', 'google').
 *   Le parametre kc_idp_hint=google fait que Keycloak saute sa propre page
 *   de login et redirige immediatement vers Google. Plus court d'un clic.
 *
 * Les trois modes aboutissent au meme etat : un token stocke sous sejour_token,
 * puis /api/me appele par AuthProvider pour recuperer le profil.
 */
export default function ConnexionUser() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [keycloakLoading, setKeycloakLoading] = useState(false);
    const [googleLoading, setGoogleLoading] = useState(false);
    const [error, setError] = useState("");

    const { login, loginWithKeycloak, isAuthenticated, isAdmin, loading: authLoading } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const redirectAfterLogin = location.state?.from?.pathname || "/";

    /**
     * Redirection automatique apres que AuthProvider a traite le callback Keycloak.
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
     * Connexion via la page de login Keycloak (formulaire + fournisseurs externes).
     */
    const handleKeycloakLogin = async () => {
        setKeycloakLoading(true);
        try {
            await loginWithKeycloak('openid pastell-admin');
        } catch {
            setKeycloakLoading(false);
            setError("Impossible de contacter Keycloak. Verifiez que le serveur est demarre.");
        }
    };

    /**
     * Connexion directe via Google (kc_idp_hint=google).
     * Keycloak saute sa page de login et redirige immediatement vers Google.
     */
    const handleGoogleLogin = async () => {
        setGoogleLoading(true);
        try {
            await loginWithKeycloak('openid pastell-admin', 'google');
        } catch {
            setGoogleLoading(false);
            setError("Impossible de contacter Keycloak. Verifiez que le serveur est demarre.");
        }
    };

    const anyLoading = loading || keycloakLoading || googleLoading;

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
                                disabled={anyLoading}
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

                        {/* Bouton Google direct (kc_idp_hint=google) */}
                        <button
                            type="button"
                            onClick={handleGoogleLogin}
                            disabled={anyLoading}
                            className="w-full flex items-center justify-center gap-3 bg-white hover:bg-gray-50 disabled:bg-gray-100 border border-gray-200 hover:border-gray-300 text-gray-700 py-3 rounded-xl font-semibold text-sm transition-all shadow-sm mb-3"
                        >
                            {/* Logo Google officiel quatre couleurs */}
                            <svg width="18" height="18" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                                <path fill="#FFC107" d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8c-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4C12.955 4 4 12.955 4 24s8.955 20 20 20s20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"/>
                                <path fill="#FF3D00" d="M6.306 14.691l6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4C16.318 4 9.656 8.337 6.306 14.691z"/>
                                <path fill="#4CAF50" d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"/>
                                <path fill="#1976D2" d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"/>
                            </svg>
                            {googleLoading ? "Redirection vers Google..." : "Continuer avec Google"}
                        </button>

                        {/* Bouton Keycloak */}
                        <button
                            type="button"
                            onClick={handleKeycloakLogin}
                            disabled={anyLoading}
                            className="w-full flex items-center justify-center gap-3 bg-white hover:bg-gray-50 disabled:bg-gray-100 border border-gray-200 hover:border-[#0EA5E9] text-gray-700 py-3 rounded-xl font-semibold text-sm transition-all shadow-sm"
                        >
                            {/* Icone Keycloak : cadenas, symbole du SSO */}
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M12 2C8.13 2 5 5.13 5 9v1H4c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2h-1V9c0-3.87-3.13-7-7-7zm0 2c2.76 0 5 2.24 5 5v1H7V9c0-2.76 2.24-5 5-5zm0 9c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2z" fill="#0EA5E9"/>
                            </svg>
                            {keycloakLoading ? "Redirection vers Keycloak..." : "Se connecter avec Keycloak"}
                        </button>

                        {/* Note explicative pour les comptes Keycloak */}
                        <p className="text-center text-xs text-gray-400 mt-3">
                            Comptes Keycloak de demo : admin-demo / user-demo
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