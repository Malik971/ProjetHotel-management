// src/Pages/admin/AdminPastellDetail.jsx

/**
 * Page admin /admin/pastell/:reservationId.
 *
 * Vue detaillee d'un dossier Pastell. Trois sections :
 *   - Colonne gauche : informations de la reservation
 *   - Colonne droite : informations du dossier Pastell (sync)
 *   - En bas (largeur complete) : journal d'orchestration en frise verticale
 *
 * Actions disponibles dans l'en-tete :
 *   - Rafraichir : recharge reservation, sync et journal
 *   - Forcer un poll : declenche un cycle de polling global du bus
 *   - Voir dans le mock : ouvre le document brut dans le mock Pastell
 *   - Relancer ce dossier : disponible si statut EN_RETRY ou EN_ERREUR
 *
 * Source de donnees :
 *   GET  /api/admin/reservations/{id}
 *   GET  /api/admin/pastell-sync/reservation/{reservationId}
 *   GET  /api/admin/pastell-sync/{syncId}/journal
 *   POST /api/admin/pastell-sync/{syncId}/retry
 *   POST /api/admin/pastell/poll
 *
 * L'URL du mock Pastell est lue depuis VITE_PASTELL_MOCK_URL pour le lien
 * "Voir dans le mock". Si la variable n'est pas definie, le bouton est masque.
 */

import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";
import {
    ArrowLeft,
    RefreshCw,
    RotateCw,
    PlayCircle,
    ExternalLink,
    AlertCircle,
    Loader2,
    User,
    Mail,
    Phone,
    Hotel as HotelIcon,
    BedDouble,
    Calendar,
    Users as UsersIcon,
    Euro,
    Hash,
    Database,
    Activity,
    Clock,
    FileWarning,
    Inbox,
    CheckCircle2,
    Circle,
} from "lucide-react";

import {
    getReservation,
    getSyncByReservation,
    getSyncJournal,
    retrySync,
    forceGlobalPoll,
} from "../../services/adminPastellService";
import StatusBadge from "../../components/admin/StatusBadge";

const MOCK_URL = import.meta.env.VITE_PASTELL_MOCK_URL || "";

export default function AdminPastellDetail() {
    const { reservationId } = useParams();

    const [reservation, setReservation] = useState(null);
    const [sync, setSync] = useState(null);
    const [journal, setJournal] = useState([]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    /**
     * actionInProgress vaut null quand aucune action n'est en cours, sinon
     * 'retry' | 'poll' | 'refresh'. Centralise dans un seul state pour
     * eviter qu'on declenche deux actions en parallele.
     */
    const [actionInProgress, setActionInProgress] = useState(null);

    /**
     * Charge la reservation, le sync, puis le journal en cascade.
     * Le sync peut etre absent (404) : la reservation n'a pas encore declenche
     * de dossier Pastell. Dans ce cas le journal est vide.
     */
    const loadAll = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            // Reservation et sync en parallele : ils sont independants
            const reservationPromise = getReservation(reservationId);
            const syncPromise = getSyncByReservation(reservationId).catch((e) => {
                // Si pas de sync, on continue avec null
                if (e.response?.status === 404) return null;
                throw e;
            });

            const [reservationData, syncData] = await Promise.all([
                reservationPromise,
                syncPromise,
            ]);

            setReservation(reservationData);
            setSync(syncData);

            // Journal ne peut etre charge que si on a un syncId
            if (syncData?.id) {
                const journalData = await getSyncJournal(syncData.id);
                setJournal(journalData || []);
            } else {
                setJournal([]);
            }
        } catch (e) {
            console.error("Echec chargement detail dossier:", e);
            setError(
                "Impossible de charger ce dossier. Verifiez que la reservation existe."
            );
        } finally {
            setLoading(false);
        }
    }, [reservationId]);

    useEffect(() => {
        loadAll();
    }, [loadAll]);

    async function handleRefresh() {
        setActionInProgress("refresh");
        try {
            await loadAll();
            toast.success("Donnees rafraichies");
        } finally {
            setActionInProgress(null);
        }
    }

    async function handleForcePoll() {
        setActionInProgress("poll");
        try {
            const result = await forceGlobalPoll();
            toast.success(
                `Poll force : ${result.processed ?? 0} entree(s) traitee(s)`
            );
            await loadAll();
        } catch (e) {
            const status = e.response?.status;
            if (status === 403) {
                toast.error("Acces refuse : X-Demo-Token manquant ou invalide.");
            } else if (status === 503) {
                toast.error("Pastell est desactive cote serveur.");
            } else {
                toast.error("Echec du poll force.");
            }
        } finally {
            setActionInProgress(null);
        }
    }

    async function handleRetry() {
        if (!sync?.id) return;
        setActionInProgress("retry");
        try {
            const result = await retrySync(sync.id);
            if (result.triggered) {
                toast.success(`Dossier #${sync.id} relance avec succes`);
            } else {
                toast.success(
                    `Dossier #${sync.id} marque EN_RETRY, le scheduler reprendra automatiquement`
                );
            }
            await loadAll();
        } catch (e) {
            const status = e.response?.status;
            if (status === 403) {
                toast.error("Acces refuse : X-Demo-Token manquant ou invalide.");
            } else {
                toast.error("Echec de la relance.");
            }
        } finally {
            setActionInProgress(null);
        }
    }

    if (loading && !reservation) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <div className="flex items-center gap-2 text-gray-500">
                    <Loader2 className="animate-spin" size={18} />
                    <span className="text-sm">Chargement du dossier...</span>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] py-12 px-4">
                <div className="max-w-2xl mx-auto text-center">
                    <Link
                        to="/admin/pastell"
                        className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium mb-8"
                    >
                        <ArrowLeft size={16} />
                        Retour a la liste
                    </Link>
                    <div className="bg-white border border-red-100 rounded-2xl p-8 shadow-sm">
                        <AlertCircle className="mx-auto text-red-500 mb-3" size={36} />
                        <p className="text-gray-700 font-medium">{error}</p>
                    </div>
                </div>
            </div>
        );
    }

    const canRetry =
        sync && (sync.syncStatus === "EN_RETRY" || sync.syncStatus === "EN_ERREUR");
    const canSeeInMock = MOCK_URL && sync?.pastellDocumentId;
    const mockHref = canSeeInMock
        ? `${MOCK_URL}/api/v2/document/${sync.pastellDocumentId}`
        : null;

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-7xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* En-tete */}
                <div className="mb-6">
                    <Link
                        to="/admin/pastell"
                        className="inline-flex items-center gap-2 text-sm text-gray-500 hover:text-[#0EA5E9] mb-4 transition-colors"
                    >
                        <ArrowLeft size={16} />
                        Retour a la liste des dossiers
                    </Link>

                    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                        <div>
                            <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                                Dossier #{reservationId}
                            </h1>
                            <p className="text-sm text-gray-600 mt-1">
                                Detail technique du dossier transmis au bus d'orchestration Pastell.
                            </p>
                        </div>

                        {/* Barre d'actions */}
                        <div className="flex flex-wrap gap-2">
                            <ActionButton
                                onClick={handleRefresh}
                                inProgress={actionInProgress === "refresh"}
                                disabled={actionInProgress !== null}
                                icon={<RefreshCw size={14} />}
                                label="Rafraichir"
                            />
                            <ActionButton
                                onClick={handleForcePoll}
                                inProgress={actionInProgress === "poll"}
                                disabled={actionInProgress !== null}
                                icon={<PlayCircle size={14} />}
                                label="Forcer un poll"
                            />
                            {canRetry && (
                                <ActionButton
                                    onClick={handleRetry}
                                    inProgress={actionInProgress === "retry"}
                                    disabled={actionInProgress !== null}
                                    icon={<RotateCw size={14} />}
                                    label="Relancer ce dossier"
                                    variant="amber"
                                />
                            )}
                            {canSeeInMock && (
                                <a
                                    href={mockHref}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0369A1] transition"
                                >
                                    <ExternalLink size={14} />
                                    Voir dans le mock
                                </a>
                            )}
                        </div>
                    </div>
                </div>

                {/* Bandeau d'alerte si dossier en anomalie */}
                {sync &&
                    (sync.syncStatus === "EN_ERREUR" ||
                        sync.syncStatus === "DIVERGENCE") && (
                        <div className="mb-6 bg-red-50 border border-red-200 rounded-2xl p-4 flex items-start gap-3">
                            <FileWarning
                                className="text-red-600 flex-shrink-0 mt-0.5"
                                size={20}
                            />
                            <div className="flex-1">
                                <p className="text-sm font-semibold text-red-900">
                                    Dossier en anomalie
                                </p>
                                <p className="text-sm text-red-800 mt-0.5">
                                    {sync.derniereErreur ||
                                        "Une intervention manuelle est requise pour debloquer ce dossier."}
                                </p>
                            </div>
                        </div>
                    )}

                {/* Grid 2 colonnes : reservation a gauche, sync a droite */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">

                    {/* Colonne gauche : reservation */}
                    <Panel title="Reservation" icon={<HotelIcon size={18} className="text-[#0EA5E9]" />}>
                        {reservation ? (
                            <div className="space-y-3 text-sm">
                                <Field icon={<Hash size={14} />} label="ID reservation">
                                    <span className="font-mono text-gray-700">
                                        #{reservation.id}
                                    </span>
                                </Field>
                                <Field icon={<User size={14} />} label="Client">
                                    {reservation.nomClient}
                                </Field>
                                <Field icon={<Mail size={14} />} label="Email">
                                    <span className="text-gray-700">{reservation.emailClient}</span>
                                </Field>
                                {reservation.telephoneClient && (
                                    <Field icon={<Phone size={14} />} label="Telephone">
                                        <span className="text-gray-700">
                                            {reservation.telephoneClient}
                                        </span>
                                    </Field>
                                )}
                                <Field icon={<HotelIcon size={14} />} label="Hotel">
                                    {reservation.hotelNom}
                                    {reservation.hotelVille && (
                                        <span className="text-xs text-gray-500 ml-1">
                                            ({reservation.hotelVille})
                                        </span>
                                    )}
                                </Field>
                                <Field icon={<BedDouble size={14} />} label="Chambre">
                                    {reservation.chambreNom}
                                </Field>
                                <Field icon={<Calendar size={14} />} label="Dates">
                                    Du {reservation.dateDebut} au {reservation.dateFin}
                                </Field>
                                <Field icon={<UsersIcon size={14} />} label="Personnes">
                                    {reservation.nombrePersonnes}
                                </Field>
                                <Field icon={<Euro size={14} />} label="Prix total">
                                    <span className="font-semibold text-[#0369A1]">
                                        {reservation.prixTotal} €
                                    </span>
                                </Field>
                                <Field icon={<Activity size={14} />} label="Statut">
                                    <ReservationStatutBadge statut={reservation.statut} />
                                </Field>
                                {reservation.codeConfirmation && (
                                    <Field icon={<Hash size={14} />} label="Code de confirmation">
                                        <span className="font-mono text-xs text-[#0369A1]">
                                            {reservation.codeConfirmation}
                                        </span>
                                    </Field>
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-gray-500 italic">
                                Reservation introuvable.
                            </p>
                        )}
                    </Panel>

                    {/* Colonne droite : dossier Pastell (sync) */}
                    <Panel
                        title="Dossier Pastell"
                        icon={<Database size={18} className="text-[#0EA5E9]" />}
                    >
                        {sync ? (
                            <div className="space-y-3 text-sm">
                                <Field icon={<Hash size={14} />} label="Sync ID">
                                    <span className="font-mono text-gray-700">#{sync.id}</span>
                                </Field>
                                <Field icon={<Database size={14} />} label="Pastell document ID">
                                    {sync.pastellDocumentId ? (
                                        <span className="font-mono text-xs text-gray-700">
                                            {sync.pastellDocumentId}
                                        </span>
                                    ) : (
                                        <span className="text-xs text-gray-400 italic">
                                            non emis
                                        </span>
                                    )}
                                </Field>
                                <Field icon={<Activity size={14} />} label="Statut sync">
                                    <StatusBadge status={sync.syncStatus} />
                                </Field>
                                <Field icon={<Activity size={14} />} label="Etape circuit">
                                    {sync.pastellEtatDernierConnu ? (
                                        <span className="font-mono text-xs text-gray-700">
                                            {sync.pastellEtatDernierConnu}
                                        </span>
                                    ) : (
                                        <span className="text-xs text-gray-400 italic">
                                            non transmise
                                        </span>
                                    )}
                                </Field>
                                <Field icon={<Clock size={14} />} label="Derniere synchro">
                                    <span className="text-gray-700">
                                        {formatDateTime(sync.derniereSynchro)}
                                    </span>
                                </Field>
                                <Field icon={<RotateCw size={14} />} label="Tentatives">
                                    <span
                                        className={
                                            sync.tentatives > 0
                                                ? "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800"
                                                : "text-gray-600"
                                        }
                                    >
                                        {sync.tentatives ?? 0}
                                    </span>
                                </Field>
                                {sync.derniereErreur && (
                                    <Field icon={<FileWarning size={14} />} label="Derniere erreur">
                                        <span className="text-xs text-red-700 font-mono break-all">
                                            {sync.derniereErreur}
                                        </span>
                                    </Field>
                                )}
                                <Field icon={<Clock size={14} />} label="Cree le">
                                    <span className="text-xs text-gray-500">
                                        {formatDateTime(sync.dateCreation)}
                                    </span>
                                </Field>
                                {sync.dateModification && (
                                    <Field icon={<Clock size={14} />} label="Modifie le">
                                        <span className="text-xs text-gray-500">
                                            {formatDateTime(sync.dateModification)}
                                        </span>
                                    </Field>
                                )}
                            </div>
                        ) : (
                            <div className="flex flex-col items-center justify-center py-8 text-center">
                                <Inbox size={28} className="text-gray-300 mb-2" />
                                <p className="text-sm text-gray-600 font-medium">
                                    Aucun dossier Pastell pour cette reservation
                                </p>
                                <p className="text-xs text-gray-400 mt-1">
                                    La synchronisation montante n'a pas encore ete declenchee
                                    ou Pastell est desactive cote serveur.
                                </p>
                            </div>
                        )}
                    </Panel>
                </div>

                {/* Journal d'orchestration */}
                <Panel
                    title="Journal d'orchestration"
                    icon={<Activity size={18} className="text-[#0EA5E9]" />}
                    subtitle={
                        journal.length > 0
                            ? `${journal.length} evenement${journal.length > 1 ? "s" : ""} enregistre${journal.length > 1 ? "s" : ""}`
                            : "Aucun evenement enregistre"
                    }
                >
                    {journal.length > 0 ? (
                        <JournalTimeline entries={journal} />
                    ) : (
                        <div className="flex flex-col items-center justify-center py-8 text-center">
                            <Inbox size={28} className="text-gray-300 mb-2" />
                            <p className="text-sm text-gray-500">
                                Le journal du bus Pastell ne contient aucun evenement pour ce dossier.
                            </p>
                        </div>
                    )}
                </Panel>
            </div>
        </div>
    );
}

/**
 * Conteneur reutilisable pour les blocs de la page.
 */
function Panel({ title, subtitle, icon, children }) {
    return (
        <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
            <div className="px-5 md:px-6 py-4 border-b border-gray-100 flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-[#F8FAFC] border border-gray-200 flex items-center justify-center">
                    {icon}
                </div>
                <div className="flex-1">
                    <h2 className="text-base font-semibold text-gray-900">{title}</h2>
                    {subtitle && (
                        <p className="text-xs text-gray-500 mt-0.5">{subtitle}</p>
                    )}
                </div>
            </div>
            <div className="px-5 md:px-6 py-5">{children}</div>
        </div>
    );
}

/**
 * Bouton d'action standardise. Variant 'amber' pour les actions de relance,
 * defaut sinon.
 */
function ActionButton({ onClick, inProgress, disabled, icon, label, variant }) {
    const base =
        "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition disabled:opacity-50 disabled:cursor-not-allowed";
    const styles =
        variant === "amber"
            ? "bg-white border border-[#F59E0B] text-[#F59E0B] hover:bg-[#F59E0B] hover:text-white"
            : "bg-white border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0369A1]";
    return (
        <button onClick={onClick} disabled={disabled} className={`${base} ${styles}`}>
            {inProgress ? <Loader2 size={14} className="animate-spin" /> : icon}
            {label}
        </button>
    );
}

/**
 * Ligne info : icone, libelle, valeur. Aligne verticalement.
 */
function Field({ icon, label, children }) {
    return (
        <div className="flex items-start gap-2">
            <div className="text-gray-400 mt-1 flex-shrink-0">{icon}</div>
            <div className="flex-1 min-w-0">
                <p className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">
                    {label}
                </p>
                <div className="text-sm text-gray-900 font-medium">{children}</div>
            </div>
        </div>
    );
}

/**
 * Badge specifique aux statuts metier de la reservation (distinct des
 * statuts techniques du sync : on reste sur le vocabulaire client).
 */
function ReservationStatutBadge({ statut }) {
    const config = {
        EN_ATTENTE: { bg: "bg-amber-50", text: "text-amber-800", label: "En attente" },
        CONFIRMEE: { bg: "bg-emerald-50", text: "text-emerald-700", label: "Confirmee" },
        TERMINEE: { bg: "bg-gray-50", text: "text-gray-600", label: "Terminee" },
        ANNULEE: { bg: "bg-red-50", text: "text-red-700", label: "Annulee" },
    }[statut] || {
        bg: "bg-gray-50",
        text: "text-gray-600",
        label: statut || "Inconnu",
    };
    return (
        <span
            className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold ${config.bg} ${config.text}`}
        >
            {config.label}
        </span>
    );
}

/**
 * Frise verticale du journal d'orchestration. Chaque entree affiche son
 * horodatage, son action, sa severite et son message.
 */
function JournalTimeline({ entries }) {
    return (
        <ol className="relative space-y-0">
            {entries.map((entry, idx) => (
                <JournalEntry
                    key={entry.id}
                    entry={entry}
                    isLast={idx === entries.length - 1}
                />
            ))}
        </ol>
    );
}

function JournalEntry({ entry, isLast }) {
    const severityConfig = getSeverityConfig(entry.severity);
    return (
        <li className="flex gap-4 pb-5 last:pb-0 relative">
            {!isLast && (
                <span
                    className="absolute left-[15px] top-8 bottom-0 w-0.5 bg-gray-200"
                    aria-hidden="true"
                />
            )}
            <div
                className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center border-2 ${severityConfig.circle}`}
            >
                {severityConfig.icon}
            </div>
            <div className="flex-1 min-w-0 pt-0.5">
                <div className="flex items-baseline gap-2 flex-wrap">
                    <span className="font-mono text-xs text-gray-700">
                        {entry.action || "evenement"}
                    </span>
                    {entry.severity && (
                        <span
                            className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider ${severityConfig.badge}`}
                        >
                            {entry.severity}
                        </span>
                    )}
                    <span className="text-xs text-gray-400 ml-auto">
                        {formatDateTime(entry.occurredAt)}
                    </span>
                </div>
                {entry.message && (
                    <p className="text-sm text-gray-700 mt-1">{entry.message}</p>
                )}
                {entry.idJ != null && (
                    <p className="text-xs text-gray-400 mt-1 font-mono">
                        id_j: {entry.idJ}
                    </p>
                )}
            </div>
        </li>
    );
}

function getSeverityConfig(severity) {
    const upper = (severity || "").toUpperCase();
    switch (upper) {
        case "ERROR":
            return {
                circle: "bg-red-50 border-red-300",
                badge: "bg-red-100 text-red-800",
                icon: <AlertCircle size={14} className="text-red-600" />,
            };
        case "WARN":
        case "WARNING":
            return {
                circle: "bg-amber-50 border-amber-300",
                badge: "bg-amber-100 text-amber-800",
                icon: <Activity size={14} className="text-amber-600" />,
            };
        case "INFO":
            return {
                circle: "bg-sky-50 border-sky-300",
                badge: "bg-sky-100 text-[#0369A1]",
                icon: <CheckCircle2 size={14} className="text-[#0EA5E9]" />,
            };
        default:
            return {
                circle: "bg-gray-50 border-gray-300",
                badge: "bg-gray-100 text-gray-700",
                icon: <Circle size={10} className="text-gray-400 fill-gray-400" />,
            };
    }
}

/**
 * Formate une date ISO en JJ/MM/AAAA HH:MM. Tolere null/invalide.
 */
function formatDateTime(iso) {
    if (!iso) return "jamais";
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "date invalide";
    return d.toLocaleString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}