// src/Pages/SuiviReservationPage.jsx
import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import {
    ArrowLeft, Calendar, MapPin, Hotel, BedDouble, Hash,
    RefreshCw, CheckCircle2, Clock, Circle, AlertCircle,
    FileCheck, ChevronDown, ChevronUp, Shield
} from "lucide-react";
import { toast } from "sonner";
import { httpClient } from "../api/httpClient";
import { useReservationTimeline } from "../hooks/useReservationTimeline";

/**
 * Page de suivi d'une reservation : URL /mes-reservations/:id
 *
 * Trois blocs distincts :
 *   - Recap du sejour (informations factuelles)
 *   - Suivi de votre sejour (timeline experience voyageur en 4 etapes)
 *   - Suivi administratif (bloc Pastell, toujours visible, collapsible)
 *
 * Polling toutes les 30 secondes via useReservationTimeline.
 */
export default function SuiviReservationPage() {
    const { id } = useParams();
    const [reservation, setReservation] = useState(null);
    const [loadingReservation, setLoadingReservation] = useState(true);

    const { timeline, loading: loadingTimeline, error, refresh } =
        useReservationTimeline(id);

    useEffect(() => {
        if (!id) return;
        setLoadingReservation(true);
        httpClient
            .get(`/api/client/reservations/${id}`)
            .then((res) => setReservation(res.data))
            .catch(() => {
                toast.error("Impossible de charger la reservation");
            })
            .finally(() => setLoadingReservation(false));
    }, [id]);

    if (loadingReservation && loadingTimeline) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <p className="text-gray-500">Chargement de votre reservation...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] py-12 px-4">
                <div className="max-w-2xl mx-auto text-center">
                    <Link
                        to="/mes-reservations"
                        className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-8"
                    >
                        <ArrowLeft size={16} />
                        Retour aux reservations
                    </Link>
                    <div className="bg-white border border-red-100 rounded-2xl p-8 shadow-sm">
                        <AlertCircle className="mx-auto text-red-500 mb-3" size={36} />
                        <p className="text-gray-700 font-medium">{error}</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-6 md:py-12">
            <div className="max-w-5xl mx-auto px-4">

                {/* Retour */}
                <Link
                    to="/mes-reservations"
                    className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-6"
                >
                    <ArrowLeft size={16} />
                    Retour aux reservations
                </Link>

                {/* Titre + rafraichir */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-2xl md:text-3xl font-bold text-gray-900">
                            Suivi de votre reservation
                        </h1>
                        <p className="text-gray-500 text-sm mt-1">
                            Mise a jour automatique toutes les 30 secondes
                        </p>
                    </div>
                    <button
                        onClick={refresh}
                        className="hidden md:flex items-center gap-2 px-3 py-2 rounded-xl border border-gray-200 text-gray-600 text-sm font-medium hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-all"
                    >
                        <RefreshCw size={14} />
                        Rafraichir
                    </button>
                </div>

                {/* Bandeau d'alerte si suivi administratif en erreur */}
                {timeline?.suiviAdministratif?.enErreur && (
                    <div className="mb-6 bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3">
                        <AlertCircle className="text-amber-600 flex-shrink-0 mt-0.5" size={20} />
                        <div className="flex-1">
                            <p className="text-sm font-semibold text-amber-900">
                                Information importante
                            </p>
                            <p className="text-sm text-amber-800 mt-0.5">
                                {timeline.suiviAdministratif.message}
                            </p>
                        </div>
                    </div>
                )}

                {/* Grid principale : recap + timeline */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">

                    {/* Recap reservation */}
                    {reservation && (
                        <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm h-fit">
                            <h2 className="text-base font-semibold text-gray-900 mb-5">
                                Votre sejour
                            </h2>

                            <div className="space-y-4 text-sm">

                                <InfoLine
                                    icon={<Hotel size={18} className="text-[#0EA5E9]" />}
                                    label="Hotel"
                                    value={reservation.hotelNom}
                                />

                                {reservation.hotelVille && (
                                    <InfoLine
                                        icon={<MapPin size={18} className="text-[#0EA5E9]" />}
                                        label="Ville"
                                        value={reservation.hotelVille}
                                    />
                                )}

                                <InfoLine
                                    icon={<BedDouble size={18} className="text-[#0EA5E9]" />}
                                    label="Chambre"
                                    value={reservation.chambreNom}
                                />

                                <InfoLine
                                    icon={<Calendar size={18} className="text-[#0EA5E9]" />}
                                    label="Dates"
                                    value={`Du ${reservation.dateDebut} au ${reservation.dateFin}`}
                                />

                                {reservation.codeConfirmation && (
                                    <InfoLine
                                        icon={<Hash size={18} className="text-[#0EA5E9]" />}
                                        label="Code de confirmation"
                                        value={
                                            <span className="font-mono text-[#0369A1]">
                                                {reservation.codeConfirmation}
                                            </span>
                                        }
                                    />
                                )}
                            </div>

                            <div className="mt-6 pt-6 border-t border-gray-100 flex items-center justify-between">
                                <span className="text-sm text-gray-500">Prix total</span>
                                <span className="text-xl font-bold text-[#0369A1]">
                                    {reservation.prixTotal} €
                                </span>
                            </div>
                        </div>
                    )}

                    {/* Timeline experience voyageur */}
                    <div className="bg-white border border-gray-100 rounded-2xl p-6 shadow-sm">
                        <h2 className="text-base font-semibold text-gray-900 mb-5">
                            Suivi de votre sejour
                        </h2>

                        {timeline && timeline.etapesSejour && (
                            <ol className="relative">
                                {timeline.etapesSejour.map((etape, idx) => (
                                    <TimelineEtape
                                        key={etape.ordre}
                                        etape={etape}
                                        isLast={idx === timeline.etapesSejour.length - 1}
                                    />
                                ))}
                            </ol>
                        )}
                    </div>
                </div>

                {/* Bloc suivi administratif Pastell, toujours visible, collapsible */}
                {timeline?.suiviAdministratif && (
                    <SuiviAdministratifBlock data={timeline.suiviAdministratif} />
                )}
            </div>
        </div>
    );
}

/**
 * Bloc decrivant l'etat du dossier administratif Pastell.
 *
 * Visible en permanence, collapsible. Ferme par defaut pour ne pas distraire
 * le voyageur de son suivi de sejour, mais accessible aux curieux et aux
 * professionnels qui veulent verifier ou en est leur dossier.
 */
function SuiviAdministratifBlock({ data }) {
    const [open, setOpen] = useState(false);

    return (
        <div className="bg-white border border-gray-100 rounded-2xl shadow-sm overflow-hidden">

            {/* En-tete cliquable */}
            <button
                onClick={() => setOpen(!open)}
                className="w-full px-6 py-5 flex items-center justify-between hover:bg-gray-50 transition-colors"
            >
                <div className="flex items-center gap-3 text-left">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                        data.enErreur
                            ? "bg-amber-100"
                            : "bg-[#F8FAFC] border border-gray-200"
                    }`}>
                        <Shield size={20} className={data.enErreur ? "text-amber-600" : "text-[#0EA5E9]"} />
                    </div>
                    <div>
                        <h3 className="text-base font-semibold text-gray-900">
                            Suivi administratif
                        </h3>
                        <p className="text-xs text-gray-500 mt-0.5">
                            {data.message}
                        </p>
                    </div>
                </div>
                {open
                    ? <ChevronUp size={20} className="text-gray-400" />
                    : <ChevronDown size={20} className="text-gray-400" />
                }
            </button>

            {/* Contenu deroulant */}
            {open && (
                <div className="px-6 pb-6 pt-2 border-t border-gray-100">

                    <p className="text-sm text-gray-600 leading-relaxed mb-4">
                        Votre reservation est instruite via un parapheur electronique conforme
                        aux exigences du secteur public francais. Ce systeme assure la tracabilite
                        et l'integrite de chaque etape de votre dossier.
                    </p>

                    <div className="bg-[#F8FAFC] rounded-xl p-4 space-y-3 text-sm">

                        <div className="flex items-start gap-3">
                            <FileCheck size={16} className="text-[#0EA5E9] mt-0.5 flex-shrink-0" />
                            <div>
                                <p className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">
                                    Etat administratif
                                </p>
                                <p className="font-mono text-xs text-gray-700">
                                    {data.statutPastell}
                                </p>
                            </div>
                        </div>

                        {data.derniereSynchro && (
                            <div className="flex items-start gap-3">
                                <Clock size={16} className="text-[#0EA5E9] mt-0.5 flex-shrink-0" />
                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">
                                        Derniere mise a jour
                                    </p>
                                    <p className="text-gray-700">
                                        {formatDate(data.derniereSynchro)}
                                    </p>
                                </div>
                            </div>
                        )}
                    </div>

                    <p className="text-xs text-gray-400 mt-4">
                        En cas de question sur votre dossier, contactez notre service client.
                    </p>
                </div>
            )}
        </div>
    );
}

/**
 * Une ligne d'info dans le recap.
 */
function InfoLine({ icon, label, value }) {
    return (
        <div className="flex items-start gap-3">
            <div className="mt-0.5">{icon}</div>
            <div className="flex-1">
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">
                    {label}
                </p>
                <p className="text-gray-800 font-medium">{value}</p>
            </div>
        </div>
    );
}

/**
 * Une etape de la timeline experience voyageur.
 */
function TimelineEtape({ etape, isLast }) {
    const config = getEtapeConfig(etape.statut);
    const dateLabel = etape.date ? formatDate(etape.date) : null;

    return (
        <li className="flex gap-4 pb-6 last:pb-0 relative">
            {!isLast && (
                <span
                    className={`absolute left-[14px] top-8 bottom-0 w-0.5 ${config.lineColor}`}
                    aria-hidden="true"
                />
            )}

            <div className={`flex-shrink-0 w-7 h-7 rounded-full flex items-center justify-center ${config.circleBg} ${config.circleBorder} ${config.animate}`}>
                {config.icon}
            </div>

            <div className="flex-1 pt-0.5">
                <p className={`text-sm font-semibold ${config.labelColor}`}>
                    {etape.label}
                </p>
                {dateLabel && (
                    <p className="text-xs text-gray-400 mt-1">{dateLabel}</p>
                )}
                {etape.statut === "CURRENT" && (
                    <p className="text-xs text-[#0369A1] mt-1">En cours</p>
                )}
            </div>
        </li>
    );
}

function getEtapeConfig(statut) {
    switch (statut) {
        case "DONE":
            return {
                circleBg: "bg-[#0EA5E9]",
                circleBorder: "border-2 border-[#0EA5E9]",
                lineColor: "bg-[#0EA5E9]",
                labelColor: "text-gray-900",
                animate: "",
                icon: <CheckCircle2 size={16} className="text-white" />,
            };
        case "CURRENT":
            return {
                circleBg: "bg-[#F59E0B]",
                circleBorder: "border-2 border-[#F59E0B]",
                lineColor: "bg-gray-200",
                labelColor: "text-gray-900",
                animate: "animate-pulse",
                icon: <Clock size={16} className="text-white" />,
            };
        case "ERROR":
            return {
                circleBg: "bg-red-500",
                circleBorder: "border-2 border-red-500",
                lineColor: "bg-gray-200",
                labelColor: "text-gray-900",
                animate: "",
                icon: <AlertCircle size={16} className="text-white" />,
            };
        case "PENDING":
        default:
            return {
                circleBg: "bg-white",
                circleBorder: "border-2 border-gray-200",
                lineColor: "bg-gray-200",
                labelColor: "text-gray-400",
                animate: "",
                icon: <Circle size={10} className="text-gray-300 fill-gray-300" />,
            };
    }
}

function formatDate(isoDate) {
    try {
        const d = new Date(isoDate);
        return d.toLocaleDateString("fr-FR", {
            day: "numeric",
            month: "long",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    } catch {
        return isoDate;
    }
}