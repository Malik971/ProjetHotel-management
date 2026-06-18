// src/Pages/admin/AdminSignaturePage.jsx

/**
 * Page admin /admin/reservations/:id/signer.
 *
 * Affiche le recapitulatif du dossier et le canvas de signature HTML5.
 * L'agent saisit son nom, trace sa signature, puis confirme.
 * Le backend genere le PDF, passe le dossier a CONFIRMEE et notifie le client.
 *
 * Flux :
 *   1. Montage : GET /api/admin/reservations/{id} (recupere le dossier)
 *   2. Montage : POST /api/admin/reservations/{id}/initier-signature
 *      -> passe le statut a SIGNATURE_EN_COURS
 *   3. Admin trace sa signature et saisit son nom
 *   4. Clic "Confirmer" : POST /api/admin/reservations/{id}/signer
 *      -> genere PDF, CONFIRMEE, emails envoyes
 *   5. Redirection vers /admin/reservations/en-attente avec toast succes
 *
 * Point de migration niveau 3 :
 *   L'etape 4 appellera le mock parapheur au lieu de generer le PDF localement.
 *   Ce composant ne change pas : seul signatureService.js est modifie.
 */

import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Loader2, AlertCircle, ArrowLeft, FileCheck, Download } from "lucide-react";
import SignatureCanvas from "../../components/admin/SignatureCanvas";
import {
    initierSignature,
    signerReservation,
    getPdfReservation,
} from "../../services/signatureService";
import { httpClient } from "../../api/httpClient";

export default function AdminSignaturePage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [reservation, setReservation] = useState(null);
    const [loading, setLoading]         = useState(true);
    const [error, setError]             = useState(null);
    const [nomSignataire, setNomSignataire] = useState("");
    const [enCours, setEnCours]         = useState(false);
    const [signe, setSigne]             = useState(false);
    const [pdfDisponible, setPdfDisponible] = useState(false);

    // --- Chargement du dossier + initiation de la signature ---

    useEffect(() => {
        if (!id) return;

        async function init() {
            setLoading(true);
            setError(null);
            try {
                // Recuperer le dossier
                const { data: reservationData } = await httpClient.get(`/api/admin/reservations/${id}`);
                setReservation(reservationData);

                // Signaler au backend que la page est ouverte (SIGNATURE_EN_COURS)
                if (reservationData.statut === "EN_ATTENTE") {
                    await initierSignature(Number(id)).catch((e) => {
                        // Non bloquant : le dossier peut deja etre en SIGNATURE_EN_COURS
                        console.warn("initierSignature :", e?.message);
                    });
                }
            } catch (e) {
                console.error("Chargement dossier :", e);
                setError("Impossible de charger ce dossier. Revenez a la liste.");
            } finally {
                setLoading(false);
            }
        }

        init();
    }, [id]);

    // --- Soumission de la signature ---

    async function handleSignature(signatureBase64) {
        if (!nomSignataire.trim()) {
            toast.error("Veuillez saisir votre nom avant de confirmer la signature.");
            return;
        }

        setEnCours(true);
        try {
            const result = await signerReservation(
                Number(id),
                signatureBase64,
                nomSignataire.trim()
            );
            setSigne(true);
            setPdfDisponible(result.pdfDisponible ?? false);
            toast.success("Dossier confirme. Le client a ete notifie.");
        } catch (e) {
            console.error("Echec signature :", e);
            const msg = e?.response?.data?.error ?? "Echec de la signature. Reessayez.";
            toast.error(msg);
        } finally {
            setEnCours(false);
        }
    }

    // --- Telechargement du PDF ---

    async function handleDownloadPdf() {
        try {
            const data = await getPdfReservation(Number(id));
            const link = document.createElement("a");
            link.href = `data:application/pdf;base64,${data.pdfBase64}`;
            link.download = `recepisse-${reservation?.codeConfirmation ?? id}.pdf`;
            link.click();
        } catch {
            toast.error("Impossible de telecharger le PDF.");
        }
    }

    // --- Retour liste ---

    function retourListe() {
        navigate("/admin/reservations/en-attente");
    }

    // --- Affichage etats ---

    if (loading) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <Loader2 className="animate-spin text-[#0EA5E9]" size={32} />
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex flex-col items-center justify-center gap-4">
                <AlertCircle size={32} className="text-red-400" />
                <p className="text-sm text-gray-600">{error}</p>
                <button
                    onClick={retourListe}
                    className="text-sm text-[#0369A1] hover:text-[#0EA5E9] font-medium"
                >
                    Retour a la liste
                </button>
            </div>
        );
    }

    const r = reservation;

    // --- Ecran post-signature ---

    if (signe) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center px-4">
                <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-8 max-w-md w-full text-center">
                    <div className="w-14 h-14 rounded-full bg-green-50 flex items-center justify-center mx-auto mb-4">
                        <FileCheck size={28} className="text-green-500" />
                    </div>
                    <h2 className="text-xl font-semibold text-gray-900 mb-1">Dossier confirme</h2>
                    <p className="text-sm text-gray-600 mb-1">
                        La reservation <span className="font-mono font-semibold">{r?.codeConfirmation}</span> est
                        desormais <span className="font-semibold text-green-600">CONFIRMEE</span>.
                    </p>
                    <p className="text-xs text-gray-500 mb-6">
                        Le client a ete notifie par email.
                    </p>
                    <div className="flex flex-col sm:flex-row gap-3 justify-center">
                        {pdfDisponible && (
                            <button
                                onClick={handleDownloadPdf}
                                className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium
                                           border border-[#0EA5E9] text-[#0369A1] hover:bg-sky-50 transition"
                            >
                                <Download size={14} />
                                Telecharger le recepisse PDF
                            </button>
                        )}
                        <button
                            onClick={retourListe}
                            className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold
                                       bg-[#0EA5E9] text-white hover:bg-[#0369A1] transition"
                        >
                            Retour a la liste
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // --- Ecran principal de signature ---

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-3xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* Fil d'ariane */}
                <button
                    onClick={retourListe}
                    className="inline-flex items-center gap-1.5 text-sm text-[#0369A1]
                               hover:text-[#0EA5E9] font-medium mb-6 transition"
                >
                    <ArrowLeft size={16} />
                    Dossiers a valider
                </button>

                {/* En-tete */}
                <div className="mb-6">
                    <h1 className="text-2xl font-semibold text-gray-900">
                        Validation du dossier
                    </h1>
                    <p className="text-sm text-gray-600 mt-1">
                        Verifiez les informations, saisissez votre nom, puis apposez votre signature.
                    </p>
                </div>

                {/* Recapitulatif du dossier */}
                <section className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6 mb-6">
                    <h2 className="text-sm font-semibold text-[#0369A1] uppercase tracking-wider mb-4">
                        Recapitulatif de la reservation
                    </h2>
                    <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
                        <InfoLine label="Reference"   value={r?.codeConfirmation} mono />
                        <InfoLine label="Client"      value={r?.nomClient} />
                        <InfoLine label="Email"       value={r?.emailClient} />
                        <InfoLine label="Telephone"   value={r?.telephoneClient} />
                        <InfoLine label="Hotel"       value={r?.hotelNom} />
                        <InfoLine label="Chambre"     value={r?.chambreNom} />
                        <InfoLine
                            label="Arrivee"
                            value={r?.dateDebut ? new Date(r.dateDebut).toLocaleDateString("fr-FR") : "-"}
                        />
                        <InfoLine
                            label="Depart"
                            value={r?.dateFin ? new Date(r.dateFin).toLocaleDateString("fr-FR") : "-"}
                        />
                        <InfoLine label="Personnes"   value={r?.nombrePersonnes} />
                        <InfoLine
                            label="Prix total"
                            value={r?.prixTotal != null
                                ? Number(r.prixTotal).toLocaleString("fr-FR", { style: "currency", currency: "EUR" })
                                : "-"}
                        />
                        <InfoLine label="Statut actuel" value={r?.statut?.replace(/_/g, " ")} />
                    </dl>
                </section>

                {/* Zone de signature */}
                <section className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6">
                    <h2 className="text-sm font-semibold text-[#0369A1] uppercase tracking-wider mb-4">
                        Visa de validation
                    </h2>

                    {/* Nom du signataire */}
                    <div className="mb-5">
                        <label
                            htmlFor="nomSignataire"
                            className="block text-sm font-medium text-gray-700 mb-1"
                        >
                            Nom et prenom du signataire
                            <span className="text-red-500 ml-1" aria-hidden="true">*</span>
                        </label>
                        <input
                            id="nomSignataire"
                            type="text"
                            value={nomSignataire}
                            onChange={(e) => setNomSignataire(e.target.value)}
                            placeholder="ex. Marie Dupont"
                            disabled={enCours}
                            className="w-full max-w-sm px-3 py-2 text-sm rounded-lg border border-gray-300
                                       focus:border-[#0EA5E9] focus:ring-2 focus:ring-sky-100
                                       disabled:opacity-50 outline-none transition"
                        />
                        <p className="text-xs text-gray-500 mt-1">
                            Ce nom apparaitra sur le recepisse PDF et est immutable apres validation.
                        </p>
                    </div>

                    {/* Canvas */}
                    <div className="mb-2">
                        <p className="text-sm font-medium text-gray-700 mb-2">
                            Signature manuscrite
                            <span className="text-red-500 ml-1" aria-hidden="true">*</span>
                        </p>
                        <SignatureCanvas
                            onSignature={handleSignature}
                            disabled={enCours}
                        />
                    </div>

                    {enCours && (
                        <div className="flex items-center gap-2 text-sm text-[#0369A1] mt-4">
                            <Loader2 size={16} className="animate-spin" />
                            Generation du PDF et envoi de la confirmation en cours...
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}

// --- Sous-composant ---

function InfoLine({ label, value, mono = false }) {
    return (
        <>
            <dt className="text-gray-500 font-medium">{label}</dt>
            <dd className={mono ? "font-mono text-gray-900" : "text-gray-900"}>
                {value ?? "-"}
            </dd>
        </>
    );
}