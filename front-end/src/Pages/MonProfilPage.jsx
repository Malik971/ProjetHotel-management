// src/Pages/MonProfilPage.jsx
import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { User, Mail, Phone, Lock, Eye, EyeOff, ArrowLeft, Save } from "lucide-react";
import { toast } from "sonner";
import { httpClient } from "../api/httpClient";
import { useAuth } from "../hooks/useAuth";

/**
 * Page de gestion du profil utilisateur.
 *
 * Deux blocs distincts :
 *   - Informations personnelles (firstName, lastName, telephone)
 *   - Mot de passe (currentPassword + newPassword + confirmation)
 *
 * Bouton sauvegarde a la fin, qui envoie tout d'un coup. L'utilisateur
 * peut modifier soit seulement ses infos, soit aussi son mot de passe.
 *
 * Apres succes, on rafraichit l'utilisateur courant en local via /api/me
 * pour que la NavBar affiche la bonne initiale si le prenom a change.
 */
export default function MonProfilPage() {
    const { user } = useAuth();

    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        telephone: "",
        currentPassword: "",
        newPassword: "",
        newPasswordConfirm: "",
    });

    const [showPassword, setShowPassword] = useState(false);
    const [showPasswordSection, setShowPasswordSection] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    // Pre-remplissage des champs avec les donnees actuelles
    useEffect(() => {
        if (user) {
            setForm((prev) => ({
                ...prev,
                firstName: user.firstName || "",
                lastName: user.lastName || "",
                telephone: user.telephone || "",
            }));
        }
    }, [user]);

    const update = (field, value) => {
        setForm((prev) => ({ ...prev, [field]: value }));
    };

    const validate = () => {
        if (!form.firstName.trim() || !form.lastName.trim()) {
            setError("Le prenom et le nom sont obligatoires");
            return false;
        }
        if (form.telephone && !/^[\d\s+().-]{8,}$/.test(form.telephone)) {
            setError("Numero de telephone invalide");
            return false;
        }
        // Validation du mot de passe seulement si la section est ouverte
        if (showPasswordSection) {
            if (!form.currentPassword) {
                setError("Veuillez saisir votre mot de passe actuel");
                return false;
            }
            if (form.newPassword.length < 6) {
                setError("Le nouveau mot de passe doit contenir au moins 6 caracteres");
                return false;
            }
            if (form.newPassword !== form.newPasswordConfirm) {
                setError("Les nouveaux mots de passe ne correspondent pas");
                return false;
            }
        }
        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        if (!validate()) return;

        setSubmitting(true);

        const payload = {
            firstName: form.firstName.trim(),
            lastName: form.lastName.trim(),
            telephone: form.telephone.trim() || null,
        };

        if (showPasswordSection) {
            payload.currentPassword = form.currentPassword;
            payload.newPassword = form.newPassword;
        }

        try {
            await httpClient.put("/api/client/profil", payload);
            toast.success("Profil mis a jour");

            // Reset des champs mot de passe
            setForm((prev) => ({
                ...prev,
                currentPassword: "",
                newPassword: "",
                newPasswordConfirm: "",
            }));
            setShowPasswordSection(false);

            // Rechargement page pour rafraichir l'AuthContext (initiale, prenom navbar)
            window.location.reload();
        } catch (err) {
            const msg =
                err.response?.data?.error
                || err.response?.data
                || "Erreur lors de la mise a jour du profil";
            setError(typeof msg === "string" ? msg : "Erreur inattendue");
        } finally {
            setSubmitting(false);
        }
    };

    if (!user) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <p className="text-gray-500">Chargement...</p>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-6 md:py-12">
            <div className="max-w-2xl mx-auto px-4">

                {/* Retour */}
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-6"
                >
                    <ArrowLeft size={16} />
                    Retour
                </Link>

                {/* En-tete avec avatar */}
                <div className="text-center mb-8">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] mb-4 shadow-lg shadow-[#0EA5E9]/30 text-white text-2xl font-bold">
                        {(user.firstName?.charAt(0) || user.email?.charAt(0) || "?").toUpperCase()}
                    </div>
                    <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-1">
                        Mon profil
                    </h1>
                    <p className="text-gray-500 text-sm">
                        Gerez vos informations personnelles
                    </p>
                </div>

                {/* Formulaire */}
                <form onSubmit={handleSubmit} className="space-y-6">

                    {/* Bloc 1 : Informations personnelles */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-6 md:p-7 shadow-sm">
                        <h2 className="text-base font-semibold text-gray-900 mb-5">
                            Informations personnelles
                        </h2>

                        <div className="space-y-4">

                            {/* Email (lecture seule) */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Email
                                </label>
                                <div className="relative">
                                    <Mail size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-300" />
                                    <input
                                        type="email"
                                        value={user.email}
                                        disabled
                                        className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm text-gray-400 cursor-not-allowed"
                                    />
                                </div>
                                <p className="text-xs text-gray-400 mt-1.5">L'email ne peut pas etre modifie</p>
                            </div>

                            {/* Prenom + Nom */}
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                        Prenom
                                    </label>
                                    <div className="relative">
                                        <User size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                        <input
                                            type="text"
                                            value={form.firstName}
                                            onChange={(e) => update("firstName", e.target.value)}
                                            required
                                            className="w-full pl-10 pr-3 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
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
                                        className="w-full px-3 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                    />
                                </div>
                            </div>

                            {/* Telephone */}
                            <div>
                                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                    Telephone
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
                        </div>
                    </div>

                    {/* Bloc 2 : Mot de passe (toggle) */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-6 md:p-7 shadow-sm">
                        <div className="flex items-center justify-between mb-5">
                            <h2 className="text-base font-semibold text-gray-900">
                                Mot de passe
                            </h2>
                            <button
                                type="button"
                                onClick={() => {
                                    setShowPasswordSection(!showPasswordSection);
                                    if (showPasswordSection) {
                                        setForm((prev) => ({
                                            ...prev,
                                            currentPassword: "",
                                            newPassword: "",
                                            newPasswordConfirm: "",
                                        }));
                                    }
                                }}
                                className="text-sm font-medium text-[#0EA5E9] hover:text-[#0284C7] transition-colors"
                            >
                                {showPasswordSection ? "Annuler" : "Modifier"}
                            </button>
                        </div>

                        {!showPasswordSection ? (
                            <p className="text-sm text-gray-500">
                                Votre mot de passe est masque pour des raisons de securite.
                            </p>
                        ) : (
                            <div className="space-y-4">
                                {/* Mot de passe actuel */}
                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                        Mot de passe actuel
                                    </label>
                                    <div className="relative">
                                        <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            value={form.currentPassword}
                                            onChange={(e) => update("currentPassword", e.target.value)}
                                            className="w-full pl-10 pr-11 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
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

                                {/* Nouveau mot de passe */}
                                <div>
                                    <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-2">
                                        Nouveau mot de passe
                                    </label>
                                    <div className="relative">
                                        <Lock size={16} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-[#0EA5E9]" />
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            value={form.newPassword}
                                            onChange={(e) => update("newPassword", e.target.value)}
                                            placeholder="Minimum 6 caracteres"
                                            className="w-full pl-10 pr-4 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                        />
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
                                            value={form.newPasswordConfirm}
                                            onChange={(e) => update("newPasswordConfirm", e.target.value)}
                                            placeholder="Repetez le nouveau mot de passe"
                                            className="w-full pl-10 pr-4 py-3 bg-[#F8FAFC] border border-gray-200 rounded-xl text-sm text-gray-800 placeholder-gray-300 focus:outline-none focus:border-[#0EA5E9] focus:bg-white transition-all"
                                        />
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Message d'erreur */}
                    {error && (
                        <div className="bg-red-50 border border-red-100 text-red-600 text-sm p-3 rounded-xl">
                            {error}
                        </div>
                    )}

                    {/* Bouton sauvegarde */}
                    <button
                        type="submit"
                        disabled={submitting}
                        className="w-full flex items-center justify-center gap-2 bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-300 text-white py-3 rounded-xl font-semibold text-sm transition-colors shadow-sm"
                    >
                        <Save size={16} />
                        {submitting ? "Enregistrement..." : "Enregistrer les modifications"}
                    </button>
                </form>
            </div>
        </div>
    );
}