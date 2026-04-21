// src/Pages/InscriptionUser.jsx
import { useState } from "react";
import { Link } from "react-router-dom";
import { Mail, Lock, User, Phone, Eye, EyeOff, ArrowLeft, Check } from "lucide-react";

export default function InscriptionUser() {
    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        telephone: "",
        password: "",
        passwordConfirm: "",
        cgu: false,
    });
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);

    const update = (field, value) => setForm((prev) => ({ ...prev, [field]: value }));

    const validate = () => {
        if (!form.firstName.trim() || !form.lastName.trim()) {
            setError("Veuillez renseigner votre prénom et votre nom");
            return false;
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
            setError("Adresse email invalide");
            return false;
        }
        if (form.telephone && !/^[\d\s+().-]{8,}$/.test(form.telephone)) {
            setError("Numéro de téléphone invalide");
            return false;
        }
        if (form.password.length < 6) {
            setError("Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }
        if (form.password !== form.passwordConfirm) {
            setError("Les mots de passe ne correspondent pas");
            return false;
        }
        if (!form.cgu) {
            setError("Vous devez accepter les conditions générales d'utilisation");
            return false;
        }
        return true;
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setError("");
        if (!validate()) return;

        setLoading(true);

        // Note : le back-end ignorera silencieusement 'telephone' tant que Users.java
        // n'aura pas le champ correspondant. À ajouter plus tard côté entité.
        const userData = {
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            email: form.email.trim(),
            telephone: form.telephone.trim(),
            password: form.password,
        };

        try {
            const response = await fetch(`${import.meta.env.VITE_API_URL}/api/v1/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(userData),
            });

            const contentType = response.headers.get("content-type");
            const responseBody =
                contentType && contentType.includes("application/json")
                    ? await response.json()
                    : await response.text();

            if (!response.ok) {
                setError(responseBody?.error || responseBody || "Erreur lors de l'inscription");
                setLoading(false);
                return;
            }

            setSuccess(true);
            setTimeout(() => {
                window.location.href = "/Connexion";
            }, 1500);
        } catch (err) {
            console.error("Erreur fetch :", err);
            setError("Erreur serveur, veuillez réessayer");
            setLoading(false);
        }
    };

    // Écran de succès
    if (success) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center px-4">
                <div className="bg-white border border-gray-100 rounded-2xl p-8 shadow-sm max-w-md w-full text-center">
                    <div className="w-14 h-14 rounded-full bg-[#E0F2FE] flex items-center justify-center mx-auto mb-4">
                        <Check size={28} className="text-[#0EA5E9]" />
                    </div>
                    <h2 className="text-xl font-bold text-gray-900 mb-2">Compte créé !</h2>
                    <p className="text-gray-500 text-sm">Redirection vers la connexion...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC] flex flex-col">

            {/* Header minimal */}
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
                        <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] mb-4 shadow-lg shadow-[#0EA5E9]/30">
                            <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
                                <path d="M3 16C3 16 5 14 7 14C9 14 10 16 12 16C14 16 15 14 17 14C19 14 21 16 21 16" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                <path d="M3 20C3 20 5 18 7 18C9 18 10 20 12 20C14 20 15 18 17 18C19 18 21 20 21 20" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                <circle cx="12" cy="7" r="3" stroke="white" strokeWidth="2" fill="#F59E0B"/>
                            </svg>
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900 mb-1">Rejoignez Séjour</h1>
                        <p className="text-gray-500 text-sm">Créez votre compte en quelques secondes</p>
                    </div>

                    {/* Formulaire */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-7 shadow-sm">
                        <form onSubmit={handleRegister} className="space-y-4">

                            {/* Prénom + Nom */}
                            <div className="grid grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                        Prénom
                                    </label>
                                    <div className="relative">
                                        <User size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                        <input
                                            type="text"
                                            value={form.firstName}
                                            onChange={(e) => update("firstName", e.target.value)}
                                            required
                                            placeholder="Jean"
                                            className="w-full pl-10 pr-3 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                        Nom
                                    </label>
                                    <input
                                        type="text"
                                        value={form.lastName}
                                        onChange={(e) => update("lastName", e.target.value)}
                                        required
                                        placeholder="Dupont"
                                        className="w-full px-3 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                </div>
                            </div>

                            {/* Email */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Email
                                </label>
                                <div className="relative">
                                    <Mail size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                    <input
                                        type="email"
                                        value={form.email}
                                        onChange={(e) => update("email", e.target.value)}
                                        required
                                        placeholder="votre@email.com"
                                        className="w-full pl-10 pr-4 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                </div>
                            </div>

                            {/* Téléphone */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Téléphone
                                </label>
                                <div className="relative">
                                    <Phone size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                    <input
                                        type="tel"
                                        value={form.telephone}
                                        onChange={(e) => update("telephone", e.target.value)}
                                        placeholder="06 12 34 56 78"
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
                                        value={form.password}
                                        onChange={(e) => update("password", e.target.value)}
                                        required
                                        placeholder="Minimum 6 caractères"
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

                            {/* Confirmation */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Confirmation
                                </label>
                                <div className="relative">
                                    <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                    <input
                                        type={showPassword ? "text" : "password"}
                                        value={form.passwordConfirm}
                                        onChange={(e) => update("passwordConfirm", e.target.value)}
                                        required
                                        placeholder="Répétez votre mot de passe"
                                        className="w-full pl-10 pr-4 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                </div>
                            </div>

                            {/* CGU */}
                            <label className="flex items-start gap-3 cursor-pointer pt-1">
                                <div
                                    onClick={() => update("cgu", !form.cgu)}
                                    className={`w-4 h-4 rounded flex items-center justify-center border transition-all duration-150 flex-shrink-0 mt-0.5 ${
                                        form.cgu
                                            ? "bg-[#0EA5E9] border-[#0EA5E9]"
                                            : "bg-white border-gray-300 hover:border-[#0EA5E9]/50"
                                    }`}
                                >
                                    {form.cgu && (
                                        <svg width="9" height="7" viewBox="0 0 9 7" fill="none">
                                            <path d="M1 3.5L3.5 6L8 1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                                        </svg>
                                    )}
                                </div>
                                <span className="text-xs text-gray-600 leading-relaxed">
                  J'accepte les{" "}
                                    <a href="#" className="text-[#0EA5E9] font-semibold hover:underline">
                    conditions générales d'utilisation
                  </a>{" "}
                                    et la{" "}
                                    <a href="#" className="text-[#0EA5E9] font-semibold hover:underline">
                    politique de confidentialité
                  </a>
                </span>
                            </label>

                            {/* Erreur */}
                            {error && (
                                <div className="bg-red-50 border border-red-100 text-red-600 text-xs p-3 rounded-xl">
                                    {error}
                                </div>
                            )}

                            {/* Bouton */}
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-300 text-white py-3 rounded-xl font-semibold text-sm transition-colors shadow-sm mt-2"
                            >
                                {loading ? "Création du compte..." : "Créer un compte"}
                            </button>
                        </form>

                        {/* Séparateur */}
                        <div className="flex items-center gap-3 my-6">
                            <div className="flex-1 h-px bg-gray-100" />
                            <span className="text-xs text-gray-300 uppercase tracking-wider">ou</span>
                            <div className="flex-1 h-px bg-gray-100" />
                        </div>

                        {/* Lien connexion */}
                        <p className="text-center text-sm text-gray-500">
                            Déjà un compte ?{" "}
                            <Link
                                to="/Connexion"
                                className="text-[#0EA5E9] font-semibold hover:text-[#0284C7] transition-colors"
                            >
                                Se connecter
                            </Link>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}