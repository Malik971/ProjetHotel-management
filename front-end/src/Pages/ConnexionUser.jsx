// src/Pages/ConnexionUser.jsx
import { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { Mail, Lock, Eye, EyeOff, ArrowLeft } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

export default function ConnexionUser() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // Lot 1 : login() remplace le fetch direct + localStorage manuel.
    // Le toast succes est gere dans AuthContext.login(), pas ici.
    const { login, isAdmin } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Si l'user etait redirige vers Connexion depuis une page protegee,
    // on tente de le ramener sur cette page apres login.
    const redirectAfterLogin = location.state?.from?.pathname || "/";

    const handleLogin = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        const success = await login(email, password);

        if (success) {
            // isAdmin est mis a jour par AuthContext apres le login.
            // On relit le role depuis le localStorage pour la redirection
            // car isAdmin du hook peut ne pas encore refleter le nouveau state
            // au moment ou on execute cette ligne.
            const roles = JSON.parse(localStorage.getItem("sejour_roles") || "[]");
            const userIsAdmin = roles.includes("ROLE_ADMIN");

            const target =
                redirectAfterLogin === "/" && userIsAdmin
                    ? "/admin"
                    : redirectAfterLogin;

            navigate(target, { replace: true });
        } else {
            // login() a deja affiche un toast.error, mais on garde aussi
            // le message inline pour les utilisateurs qui auraient desactive
            // les notifications.
            setError("Identifiants incorrects");
        }

        setLoading(false);
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

                    {/* Formulaire */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-7 shadow-sm">
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

                            {/* Bouton */}
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-300 text-white py-3 rounded-xl font-semibold text-sm transition-colors shadow-sm"
                            >
                                {loading ? "Connexion..." : "Se connecter"}
                            </button>
                        </form>

                        {/* Séparateur */}
                        <div className="flex items-center gap-3 my-6">
                            <div className="flex-1 h-px bg-gray-100" />
                            <span className="text-xs text-gray-300 uppercase tracking-wider">ou</span>
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