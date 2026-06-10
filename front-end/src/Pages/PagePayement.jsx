// src/Pages/PagePayement.jsx
import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
    ArrowLeft, CheckCircle, Calendar, Users, Bed, MapPin,
    Lock, ShieldCheck, CreditCard, Landmark,
} from "lucide-react";
import { creerReservation } from "../services/reservationService";
import { useAuth } from "../hooks/useAuth";
import {
    VisaLogo, MastercardLogo, CbLogo, PaypalLogo, ApplePayLogo, GooglePayLogo,
} from "../components/payment/PaymentBrands";

function formatDateFr(iso) {
    if (!iso) return "—";
    return new Intl.DateTimeFormat("fr-FR", {
        day: "numeric", month: "long", year: "numeric",
    }).format(new Date(iso));
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de formatage carte (demo : aucune donnee n'est transmise nulle part)
// ─────────────────────────────────────────────────────────────────────────────
const onlyDigits = (s) => s.replace(/\D/g, "");

// Groupe les chiffres par paquets de 4 : "4242424242424242" -> "4242 4242 4242 4242"
function formatCardNumber(value) {
    return onlyDigits(value).slice(0, 16).replace(/(.{4})/g, "$1 ").trim();
}

// "1228" -> "12 / 28", insertion automatique du slash
function formatExpiry(value) {
    const d = onlyDigits(value).slice(0, 4);
    if (d.length <= 2) return d;
    return `${d.slice(0, 2)} / ${d.slice(2)}`;
}

// Detection du reseau a partir des premiers chiffres (regles simplifiees)
function detectBrand(value) {
    const d = onlyDigits(value);
    if (!d) return null;
    if (/^4/.test(d)) return "visa";
    if (/^(5[1-5]|2[2-7])/.test(d)) return "mastercard";
    return "cb";
}

// ─────────────────────────────────────────────────────────────────────────────
// Etat de confirmation apres succes
// ─────────────────────────────────────────────────────────────────────────────
function ConfirmationScreen({ code, chambre, hotel, dateDebut, dateFin, total }) {
    const navigate = useNavigate();
    return (
        <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center px-4">
            <div className="bg-white rounded-2xl shadow-lg border border-gray-100 max-w-lg w-full p-8 text-center">
                <div className="w-16 h-16 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-4">
                    <CheckCircle size={32} className="text-emerald-500" />
                </div>
                <h1 className="text-2xl font-bold text-gray-900 mb-2">Reservation confirmee</h1>
                <p className="text-gray-500 text-sm mb-6">
                    Votre paiement a ete accepte et votre sejour est reserve. Un email de
                    confirmation vous a ete envoye.
                </p>

                <div className="bg-[#F0F9FF] border border-[#BAE6FD] rounded-xl p-4 text-left mb-6 space-y-2">
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Code de confirmation</span>
                        <span className="font-bold text-[#0369A1] font-mono tracking-wider">{code}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Hotel</span>
                        <span className="font-medium text-gray-800">{hotel?.nom}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Chambre</span>
                        <span className="font-medium text-gray-800">{chambre?.nom}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Arrivee</span>
                        <span className="font-medium text-gray-800">{formatDateFr(dateDebut)}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                        <span className="text-gray-500">Depart</span>
                        <span className="font-medium text-gray-800">{formatDateFr(dateFin)}</span>
                    </div>
                    <div className="border-t border-[#BAE6FD] pt-2 flex justify-between font-semibold">
                        <span className="text-gray-700">Total paye</span>
                        <span className="text-[#0369A1]">{total?.toFixed(2)}€</span>
                    </div>
                </div>

                <div className="flex gap-3">
                    <button
                        onClick={() => navigate("/mes-reservations")}
                        className="flex-1 bg-[#0EA5E9] hover:bg-[#0284C7] text-white font-semibold py-3 rounded-xl text-sm transition-colors"
                    >
                        Mes reservations
                    </button>
                    <button
                        onClick={() => navigate("/")}
                        className="flex-1 border border-gray-200 text-gray-700 font-semibold py-3 rounded-xl text-sm hover:bg-gray-50 transition-colors"
                    >
                        Accueil
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Onglet de selection d'un moyen de paiement
// ─────────────────────────────────────────────────────────────────────────────
function MethodTab({ active, onClick, label, children }) {
    return (
        <button
            type="button"
            onClick={onClick}
            aria-pressed={active}
            className={`flex flex-col items-center justify-center gap-1.5 px-2 py-3 rounded-xl border text-xs font-medium transition-all ${
                active
                    ? "border-[#0EA5E9] bg-sky-50 ring-2 ring-[#0EA5E9]/15 text-gray-900"
                    : "border-gray-200 bg-white text-gray-600 hover:border-gray-300"
            }`}
        >
            <span className="h-5 flex items-center gap-1">{children}</span>
            <span>{label}</span>
        </button>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Bandeau de reassurance (badges + logos)
// ─────────────────────────────────────────────────────────────────────────────
function TrustBadges() {
    return (
        <div className="rounded-xl border border-gray-100 bg-[#F8FAFC] p-3.5">
            <div className="flex items-center gap-2 text-xs font-semibold text-gray-700">
                <Lock size={13} className="text-emerald-600" />
                Paiement 100% securise
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[11px] text-gray-500">
                <span className="inline-flex items-center gap-1">
                    <ShieldCheck size={12} className="text-emerald-500" /> Chiffrement SSL
                </span>
                <span className="inline-flex items-center gap-1">
                    <ShieldCheck size={12} className="text-emerald-500" /> 3-D Secure
                </span>
                <span className="ml-auto flex items-center gap-2">
                    <VisaLogo className="h-4" />
                    <MastercardLogo className="h-4" />
                    <CbLogo className="h-4" />
                    <PaypalLogo className="h-4" />
                </span>
            </div>
        </div>
    );
}

export default function PagePayement() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [confirmation, setConfirmation] = useState(null);
    const [method, setMethod] = useState("card");

    // Donnees transmises par ChambreDetailsPage via navigate state
    const { chambre, hotel, hotelSlug, dateDebut, dateFin, nombrePersonnes, nuits, total } = state || {};

    // Pre-remplissage depuis le profil utilisateur
    const [form, setForm] = useState({
        nomClient: user ? `${user.firstName || ""} ${user.lastName || ""}`.trim() : "",
        emailClient: user?.email || "",
        telephoneClient: user?.telephone || "",
        cardNumber: "",
        cardExpiry: "",
        cardCvc: "",
        cardName: "",
    });

    // Redirection si on arrive sur cette page sans donnees
    if (!chambre || !hotel) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex flex-col items-center justify-center gap-4">
                <p className="text-gray-500">Aucune reservation en cours.</p>
                <button onClick={() => navigate("/")}
                    className="bg-[#0EA5E9] text-white px-6 py-2.5 rounded-xl text-sm font-semibold">
                    Retour a l'accueil
                </button>
            </div>
        );
    }

    if (confirmation) {
        return (
            <ConfirmationScreen
                code={confirmation}
                chambre={chambre}
                hotel={hotel}
                dateDebut={dateDebut}
                dateFin={dateFin}
                total={total}
            />
        );
    }

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    };

    // Champs carte avec formatage en direct
    const handleCardNumber = (e) =>
        setForm((prev) => ({ ...prev, cardNumber: formatCardNumber(e.target.value) }));
    const handleExpiry = (e) =>
        setForm((prev) => ({ ...prev, cardExpiry: formatExpiry(e.target.value) }));
    const handleCvc = (e) =>
        setForm((prev) => ({ ...prev, cardCvc: onlyDigits(e.target.value).slice(0, 4) }));

    const brand = detectBrand(form.cardNumber);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        try {
            // Demo : aucune donnee bancaire n'est transmise. Le "paiement" est
            // simule, seule la reservation est reellement creee cote backend.
            const response = await creerReservation({
                chambreId: chambre.id,
                dateDebut,
                dateFin,
                nombrePersonnes,
                nomClient: form.nomClient,
                emailClient: form.emailClient,
                telephoneClient: form.telephoneClient,
            });
            setConfirmation(response.codeConfirmation);
        } catch (err) {
            const msg = err.response?.data?.message
                || err.response?.data?.error
                || "Une erreur est survenue. Veuillez reessayer.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    const submitLabel = loading
        ? "Paiement en cours..."
        : method === "paypal"
            ? `Payer ${total?.toFixed(2)}€ avec PayPal`
            : method === "transfer"
                ? "Valider le virement"
                : `Payer ${total?.toFixed(2)}€`;

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-8">
            <div className="max-w-4xl mx-auto px-4 md:px-6">

                {/* Retour */}
                <button
                    onClick={() =>
                        navigate(hotelSlug ? `/hotel/${hotelSlug}/chambre/${chambre.id}` : -1)
                    }
                    className="flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-6"
                >
                    <ArrowLeft size={16} />
                    Retour a la chambre
                </button>

                <h1 className="text-2xl font-bold text-gray-900 mb-6">Paiement securise</h1>

                <div className="grid grid-cols-1 lg:grid-cols-[1fr_380px] gap-6">

                    {/* Colonne gauche : informations + paiement */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm">

                        {error && (
                            <div className="bg-red-50 border border-red-200 text-red-700 text-sm p-4 rounded-xl mb-4">
                                {error}
                            </div>
                        )}

                        <form onSubmit={handleSubmit} className="space-y-6">

                            {/* --- Informations client --- */}
                            <section>
                                <h2 className="text-base font-bold text-gray-900 mb-4">Vos informations</h2>
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                            Nom complet
                                        </label>
                                        <input
                                            type="text" name="nomClient" value={form.nomClient}
                                            onChange={handleChange} required placeholder="Jean Dupont"
                                            className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                        />
                                    </div>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                Adresse email
                                            </label>
                                            <input
                                                type="email" name="emailClient" value={form.emailClient}
                                                onChange={handleChange} required placeholder="jean.dupont@email.com"
                                                className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                Telephone
                                            </label>
                                            <input
                                                type="tel" name="telephoneClient" value={form.telephoneClient}
                                                onChange={handleChange} required placeholder="06 12 34 56 78"
                                                className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </section>

                            {/* --- Mode de paiement --- */}
                            <section>
                                <h2 className="text-base font-bold text-gray-900 mb-4">Mode de paiement</h2>

                                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-4">
                                    <MethodTab active={method === "card"} onClick={() => setMethod("card")} label="Carte">
                                        <VisaLogo className="h-4" />
                                        <MastercardLogo className="h-4" />
                                    </MethodTab>
                                    <MethodTab active={method === "paypal"} onClick={() => setMethod("paypal")} label="PayPal">
                                        <PaypalLogo className="h-4" />
                                    </MethodTab>
                                    <MethodTab active={method === "wallet"} onClick={() => setMethod("wallet")} label="Wallet">
                                        <ApplePayLogo className="h-4" />
                                        <GooglePayLogo className="h-4" />
                                    </MethodTab>
                                    <MethodTab active={method === "transfer"} onClick={() => setMethod("transfer")} label="Virement">
                                        <Landmark size={16} className="text-gray-500" />
                                    </MethodTab>
                                </div>

                                {/* Panneau Carte bancaire */}
                                {method === "card" && (
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                Numero de carte
                                            </label>
                                            <div className="relative">
                                                <input
                                                    type="text" inputMode="numeric" name="cardNumber"
                                                    value={form.cardNumber} onChange={handleCardNumber} required
                                                    placeholder="4242 4242 4242 4242"
                                                    className="w-full border border-gray-200 rounded-xl pl-4 pr-16 py-3 text-sm font-mono tracking-wider outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                                />
                                                <span className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center">
                                                    {brand === "visa" && <VisaLogo className="h-5" />}
                                                    {brand === "mastercard" && <MastercardLogo className="h-5" />}
                                                    {brand === "cb" && <CbLogo className="h-5" />}
                                                    {!brand && <CreditCard size={18} className="text-gray-300" />}
                                                </span>
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-2 gap-4">
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                    Expiration
                                                </label>
                                                <input
                                                    type="text" inputMode="numeric" name="cardExpiry"
                                                    value={form.cardExpiry} onChange={handleExpiry} required
                                                    placeholder="MM / AA"
                                                    className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm font-mono outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                    CVC
                                                </label>
                                                <input
                                                    type="text" inputMode="numeric" name="cardCvc"
                                                    value={form.cardCvc} onChange={handleCvc} required
                                                    placeholder="123"
                                                    className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm font-mono outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                                />
                                            </div>
                                        </div>

                                        <div>
                                            <label className="block text-xs font-semibold text-gray-700 mb-1.5 uppercase tracking-wider">
                                                Titulaire de la carte
                                            </label>
                                            <input
                                                type="text" name="cardName" value={form.cardName}
                                                onChange={handleChange} required placeholder="JEAN DUPONT"
                                                className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm uppercase outline-none focus:border-[#0EA5E9] focus:ring-2 focus:ring-[#0EA5E9]/10 transition-all"
                                            />
                                        </div>
                                    </div>
                                )}

                                {/* Panneau PayPal */}
                                {method === "paypal" && (
                                    <div className="rounded-xl border border-gray-200 bg-[#F8FAFC] p-5 text-center">
                                        <PaypalLogo className="h-6 mx-auto" />
                                        <p className="text-sm text-gray-600 mt-3">
                                            Vous serez redirige vers PayPal pour valider votre paiement en toute securite.
                                        </p>
                                    </div>
                                )}

                                {/* Panneau Wallet (Apple Pay / Google Pay) */}
                                {method === "wallet" && (
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                        <div className="flex items-center justify-center rounded-xl border border-gray-900 bg-black py-3.5">
                                            <ApplePayLogo className="h-6 [&_*]:fill-white [&_text]:fill-white" />
                                        </div>
                                        <div className="flex items-center justify-center rounded-xl border border-gray-200 bg-white py-3.5">
                                            <GooglePayLogo className="h-6" />
                                        </div>
                                    </div>
                                )}

                                {/* Panneau Virement */}
                                {method === "transfer" && (
                                    <div className="rounded-xl border border-gray-200 bg-[#F8FAFC] p-5 space-y-2">
                                        <p className="text-sm text-gray-600">
                                            Effectuez un virement vers l'IBAN ci-dessous en indiquant votre nom en reference.
                                            Votre reservation sera confirmee a reception.
                                        </p>
                                        <div className="flex items-center gap-2 rounded-lg bg-white border border-gray-200 px-3 py-2">
                                            <Landmark size={15} className="text-[#0EA5E9]" />
                                            <span className="font-mono text-sm text-gray-800 tracking-wide">
                                                FR76 3000 4000 0312 3456 7890 143
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </section>

                            {/* --- Reassurance --- */}
                            <TrustBadges />

                            {/* Mention demo */}
                            <div className="bg-[#FEF3C7] border border-[#FDE68A] rounded-xl p-4 text-xs text-[#92400E]">
                                Demonstration : aucun paiement reel n'est effectue et aucune donnee bancaire
                                n'est transmise ni stockee.
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full flex items-center justify-center gap-2 bg-[#0EA5E9] hover:bg-[#0284C7] disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-semibold py-3.5 rounded-xl transition-colors text-sm"
                            >
                                <Lock size={15} />
                                {submitLabel}
                            </button>
                        </form>
                    </div>

                    {/* Recap reservation */}
                    <div className="space-y-4">
                        <div className="bg-white border border-gray-100 rounded-2xl p-5 shadow-sm">
                            <h2 className="text-sm font-bold text-gray-900 mb-4">Recapitulatif</h2>

                            {/* Image chambre */}
                            {(chambre.imageUrls?.[0] || chambre.imageUrl) && (
                                <img
                                    src={resolveImage(chambre.imageUrls?.[0] || chambre.imageUrl)}
                                    alt={chambre.nom}
                                    className="w-full h-36 object-cover rounded-xl mb-4"
                                    onError={(e) => { e.target.style.display = "none"; }}
                                />
                            )}

                            <div className="space-y-3">
                                <div>
                                    <p className="font-semibold text-gray-900 text-sm">{chambre.nom}</p>
                                    <div className="flex items-center gap-1 text-xs text-gray-500 mt-0.5">
                                        <MapPin size={11} className="text-[#0EA5E9]" />
                                        <span>{hotel.nom}, {hotel.ville}</span>
                                    </div>
                                </div>

                                <div className="border-t border-gray-100 pt-3 space-y-2 text-sm">
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <Calendar size={14} className="text-[#0EA5E9]" />
                                        <span>{formatDateFr(dateDebut)} - {formatDateFr(dateFin)}</span>
                                    </div>
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <Users size={14} className="text-[#0EA5E9]" />
                                        <span>{nombrePersonnes} {nombrePersonnes > 1 ? "personnes" : "personne"}</span>
                                    </div>
                                    <div className="flex items-center gap-2 text-gray-600">
                                        <Bed size={14} className="text-[#0EA5E9]" />
                                        <span>{nuits} nuit{nuits > 1 ? "s" : ""}</span>
                                    </div>
                                </div>

                                <div className="border-t border-gray-100 pt-3 space-y-1.5 text-sm">
                                    <div className="flex justify-between text-gray-600">
                                        <span>{parseFloat(chambre.prixParNuit).toFixed(2)}€ × {nuits} nuit{nuits > 1 ? "s" : ""}</span>
                                        <span>{total?.toFixed(2)}€</span>
                                    </div>
                                    <div className="flex justify-between font-bold text-gray-900 text-base">
                                        <span>Total</span>
                                        <span className="text-[#0369A1]">{total?.toFixed(2)}€</span>
                                    </div>
                                    <p className="text-xs text-gray-400">Taxes et frais inclus</p>
                                </div>
                            </div>
                        </div>

                        {/* Annulation gratuite */}
                        <div className="flex items-start gap-3 bg-emerald-50 border border-emerald-100 rounded-xl p-4">
                            <CheckCircle size={16} className="text-emerald-500 shrink-0 mt-0.5" />
                            <div>
                                <p className="text-xs font-semibold text-emerald-700">Annulation gratuite</p>
                                <p className="text-xs text-emerald-600 mt-0.5">Annulez sans frais avant la date d'arrivee</p>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
}

// Fonction utilitaire locale pour resoudre les URLs d'images
function resolveImage(url) {
    if (!url) return null;
    if (url.startsWith("http")) return url;
    return `${import.meta.env.VITE_API_URL}${url}`;
}
