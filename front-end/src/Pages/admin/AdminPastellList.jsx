// src/Pages/admin/AdminPastellList.jsx

/**
 * Page admin /admin/pastell.
 *
 * Vue d'ensemble des dossiers transmis au bus d'orchestration Pastell.
 * Permet de filtrer par etape circuit et de relancer un dossier en anomalie.
 *
 * Source de donnees :
 *   GET  /api/admin/pastell-sync         (page paginee de dossiers)
 *   GET  /api/admin/pastell/status       (compteurs par statut)
 *   POST /api/admin/pastell-sync/{id}/retry (relance manuelle)
 *
 * Vocabulaire Libriciel : "dossier", "etape circuit", "anomalie", "Relancer".
 *
 * Sous-lot 3.3 : l'ID dossier est devenu un lien vers /admin/pastell/:reservationId.
 *
 * Iteration UX : la ligne entiere est cliquable et accessible au clavier.
 *   - clic n'importe ou sur la ligne : navigation vers le detail
 *   - Ctrl+clic ou clic-droit sur l'ID : ouvrir dans un nouvel onglet (Link)
 *   - bouton Relancer : action isolee, ne declenche pas la navigation
 *   - chevron a droite visible au survol pour signaler l'affordance
 *   - tabIndex et onKeyDown pour la navigation clavier
 */

import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
    RefreshCw,
    ChevronLeft,
    ChevronRight,
    Loader2,
    AlertCircle,
    Inbox,
} from "lucide-react";

import {
    listSyncs,
    getPastellStatus,
    retrySync,
} from "../../services/adminPastellService";
import StatusBadge from "../../components/admin/StatusBadge";

const PAGE_SIZE = 20;

/**
 * Definition des filtres. Le `value` null signifie "tous les statuts"
 * (le parametre est alors omis dans la requete). `countKey` indique
 * d'ou lire le compteur dans la reponse de /api/admin/pastell/status.
 */
const FILTERS = [
    { key: "ALL", label: "Tous", value: null, countKey: "__total__" },
    { key: "PENDING", label: "En attente", value: "PENDING", countKey: "syncCountPending" },
    { key: "OK", label: "OK", value: "OK", countKey: "syncCountOk" },
    { key: "EN_RETRY", label: "En relance", value: "EN_RETRY", countKey: "syncCountEnRetry" },
    { key: "EN_ERREUR", label: "En anomalie", value: "EN_ERREUR", countKey: "syncCountEnErreur" },
    { key: "DIVERGENCE", label: "Divergence", value: "DIVERGENCE", countKey: "syncCountDivergence" },
];

export default function AdminPastellList() {
    const navigate = useNavigate();

    const [activeFilter, setActiveFilter] = useState("ALL");
    const [currentPage, setCurrentPage] = useState(0);

    const [pageData, setPageData] = useState({
        content: [],
        page: 0,
        size: PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
    });

    const [counts, setCounts] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [retryingId, setRetryingId] = useState(null);

    const refreshCounts = useCallback(async () => {
        try {
            const status = await getPastellStatus();
            const total =
                (status.syncCountPending || 0) +
                (status.syncCountOk || 0) +
                (status.syncCountEnRetry || 0) +
                (status.syncCountEnErreur || 0) +
                (status.syncCountDivergence || 0);
            setCounts({ ...status, __total__: total });
        } catch (e) {
            console.warn("Echec chargement compteurs Pastell:", e);
        }
    }, []);

    const refreshList = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const filter = FILTERS.find((f) => f.key === activeFilter);
            const data = await listSyncs({
                status: filter?.value || undefined,
                page: currentPage,
                size: PAGE_SIZE,
            });
            setPageData(data);
        } catch (e) {
            console.error("Echec chargement dossiers Pastell:", e);
            setError("Impossible de charger les dossiers. Reessayez dans un instant.");
            setPageData({
                content: [],
                page: 0,
                size: PAGE_SIZE,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
            });
        } finally {
            setLoading(false);
        }
    }, [activeFilter, currentPage]);

    useEffect(() => {
        refreshCounts();
    }, [refreshCounts]);

    useEffect(() => {
        refreshList();
    }, [refreshList]);

    function handleFilterClick(filterKey) {
        if (filterKey === activeFilter) return;
        setActiveFilter(filterKey);
        setCurrentPage(0);
    }

    function handlePrev() {
        if (!pageData.first) setCurrentPage((p) => Math.max(0, p - 1));
    }

    function handleNext() {
        if (!pageData.last) setCurrentPage((p) => p + 1);
    }

    async function handleRetry(syncId) {
        setRetryingId(syncId);
        try {
            const result = await retrySync(syncId);
            if (result.triggered) {
                toast.success(`Dossier #${syncId} relance avec succes`);
            } else {
                toast.success(
                    `Dossier #${syncId} marque EN_RETRY, le scheduler reprendra automatiquement`
                );
            }
            await Promise.all([refreshList(), refreshCounts()]);
        } catch (e) {
            console.error("Echec relance:", e);
            const message =
                e.response?.status === 403
                    ? "Acces refuse : X-Demo-Token manquant ou invalide."
                    : "Echec de la relance. Le scheduler reprendra automatiquement.";
            toast.error(message);
        } finally {
            setRetryingId(null);
        }
    }

    function handleRowOpen(reservationId) {
        navigate(`/admin/pastell/${reservationId}`);
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-7xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* En-tete */}
                <div className="mb-6 md:mb-8">
                    <div className="flex items-center justify-between gap-4 flex-wrap">
                        <div>
                            <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                                Dossiers Pastell
                            </h1>
                            <p className="text-sm text-gray-600 mt-1 max-w-2xl">
                                Vue d'ensemble des dossiers transmis au bus d'orchestration
                                Pastell. Cliquez sur une ligne pour ouvrir le detail, filtrez
                                par etape circuit, relancez les dossiers en anomalie.
                            </p>
                        </div>
                        <button
                            onClick={() => navigate("/admin")}
                            className="text-sm text-[#0369A1] hover:text-[#0EA5E9] font-medium"
                        >
                            Retour au tableau de bord
                        </button>
                    </div>
                </div>

                {/* Boutons de filtre par statut */}
                <div className="flex flex-wrap gap-2 mb-6">
                    {FILTERS.map((f) => {
                        const isActive = f.key === activeFilter;
                        const count = counts ? counts[f.countKey] : null;
                        return (
                            <button
                                key={f.key}
                                onClick={() => handleFilterClick(f.key)}
                                className={
                                    isActive
                                        ? "inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-[#0EA5E9] text-white shadow-sm transition"
                                        : "inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-white border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0369A1] transition"
                                }
                            >
                                <span>{f.label}</span>
                                {count !== null && count !== undefined && (
                                    <span
                                        className={
                                            isActive
                                                ? "inline-flex items-center justify-center min-w-6 px-1.5 py-0.5 rounded-full text-xs font-semibold bg-white/20"
                                                : "inline-flex items-center justify-center min-w-6 px-1.5 py-0.5 rounded-full text-xs font-semibold bg-gray-100 text-gray-600"
                                        }
                                    >
                                        {count}
                                    </span>
                                )}
                            </button>
                        );
                    })}
                </div>

                {/* Conteneur tableau */}
                <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">

                    {loading && pageData.content.length === 0 && (
                        <div className="flex items-center justify-center py-20 text-gray-500">
                            <Loader2 className="animate-spin mr-2" size={18} />
                            <span className="text-sm">Chargement des dossiers...</span>
                        </div>
                    )}

                    {error && (
                        <div className="flex items-center justify-center py-20 text-red-600 gap-2">
                            <AlertCircle size={18} />
                            <span className="text-sm">{error}</span>
                        </div>
                    )}

                    {!loading && !error && pageData.content.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 text-gray-500 gap-2">
                            <Inbox size={32} className="text-gray-300" />
                            <p className="text-sm">Aucun dossier pour ce filtre.</p>
                        </div>
                    )}

                    {pageData.content.length > 0 && (
                        <>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead className="bg-[#F8FAFC] border-b border-gray-200">
                                        <tr>
                                            <Th>ID</Th>
                                            <Th>Client</Th>
                                            <Th>Hotel</Th>
                                            <Th>Etape circuit</Th>
                                            <Th>Statut</Th>
                                            <Th>Derniere synchro</Th>
                                            <Th className="text-center">Tentatives</Th>
                                            <Th className="text-right">Actions</Th>
                                            <th className="w-10" aria-hidden="true" />
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {pageData.content.map((sync) => (
                                            <SyncRow
                                                key={sync.syncId}
                                                sync={sync}
                                                onRetry={handleRetry}
                                                onOpen={handleRowOpen}
                                                isRetrying={retryingId === sync.syncId}
                                            />
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            <div className="flex items-center justify-between px-4 md:px-6 py-3 border-t border-gray-200 bg-[#F8FAFC]">
                                <p className="text-xs text-gray-600">
                                    Page {pageData.page + 1} sur {Math.max(1, pageData.totalPages)} ({pageData.totalElements} dossier{pageData.totalElements > 1 ? "s" : ""} au total)
                                </p>
                                <div className="flex gap-2">
                                    <button
                                        onClick={handlePrev}
                                        disabled={pageData.first || loading}
                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0369A1] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-gray-200 disabled:hover:text-gray-700 transition"
                                    >
                                        <ChevronLeft size={14} />
                                        Precedent
                                    </button>
                                    <button
                                        onClick={handleNext}
                                        disabled={pageData.last || loading}
                                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-gray-200 text-gray-700 hover:border-[#0EA5E9] hover:text-[#0369A1] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-gray-200 disabled:hover:text-gray-700 transition"
                                    >
                                        Suivant
                                        <ChevronRight size={14} />
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

/**
 * En-tete de colonne du tableau.
 */
function Th({ children, className = "" }) {
    return (
        <th
            className={`px-4 md:px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider whitespace-nowrap ${className}`}
        >
            {children}
        </th>
    );
}

/**
 * Cellule de ligne.
 */
function Td({ children, className = "" }) {
    return (
        <td className={`px-4 md:px-6 py-3 whitespace-nowrap text-gray-700 ${className}`}>
            {children}
        </td>
    );
}

/**
 * Une ligne du tableau.
 *
 * La ligne entiere est cliquable et accessible au clavier. Les elements
 * interactifs imbriques (Link sur l'ID, bouton Relancer) utilisent
 * stopPropagation pour ne pas declencher la navigation parente.
 *
 * L'affordance visuelle est triple :
 *   - cursor-pointer au survol
 *   - fond legerement teinte au hover (bleu pale)
 *   - chevron a droite qui apparait au survol (group-hover)
 */
function SyncRow({ sync, onRetry, onOpen, isRetrying }) {
    const canRetry =
        sync.syncStatus === "EN_RETRY" || sync.syncStatus === "EN_ERREUR";

    function handleRowClick() {
        onOpen(sync.reservationId);
    }

    function handleRowKeyDown(e) {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onOpen(sync.reservationId);
        }
    }

    function stopBubble(e) {
        e.stopPropagation();
    }

    return (
        <tr
            onClick={handleRowClick}
            onKeyDown={handleRowKeyDown}
            tabIndex={0}
            role="link"
            aria-label={`Ouvrir le dossier ${sync.reservationId}`}
            className="group cursor-pointer hover:bg-sky-50/60 focus:bg-sky-50/60 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-[#0EA5E9] transition"
        >
            <Td>
                <Link
                    to={`/admin/pastell/${sync.reservationId}`}
                    onClick={stopBubble}
                    className="font-mono text-xs text-[#0369A1] hover:text-[#0EA5E9] hover:underline"
                >
                    #{sync.syncId}
                </Link>
            </Td>
            <Td>
                <div className="flex flex-col">
                    <span className="font-medium text-gray-900">
                        {sync.clientNom || "Client inconnu"}
                    </span>
                    {sync.clientEmail && (
                        <span className="text-xs text-gray-500">{sync.clientEmail}</span>
                    )}
                </div>
            </Td>
            <Td>{sync.hotelNom || "Hotel inconnu"}</Td>
            <Td>
                {sync.etapeCircuit ? (
                    <span className="font-mono text-xs text-gray-600">
                        {sync.etapeCircuit}
                    </span>
                ) : (
                    <span className="text-xs text-gray-400 italic">non transmise</span>
                )}
            </Td>
            <Td>
                <StatusBadge status={sync.syncStatus} />
            </Td>
            <Td>
                <span className="text-xs text-gray-600" title={sync.derniereSynchro || ""}>
                    {formatRelative(sync.derniereSynchro)}
                </span>
            </Td>
            <Td className="text-center">
                <span
                    className={
                        sync.retryCount > 0
                            ? "inline-flex items-center justify-center min-w-6 px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800"
                            : "text-xs text-gray-400"
                    }
                >
                    {sync.retryCount ?? 0}
                </span>
            </Td>
            <Td className="text-right">
                {canRetry ? (
                    <button
                        onClick={(e) => {
                            stopBubble(e);
                            onRetry(sync.syncId);
                        }}
                        disabled={isRetrying}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-white border border-[#F59E0B] text-[#F59E0B] hover:bg-[#F59E0B] hover:text-white disabled:opacity-50 disabled:cursor-not-allowed transition"
                    >
                        {isRetrying ? (
                            <Loader2 size={12} className="animate-spin" />
                        ) : (
                            <RefreshCw size={12} />
                        )}
                        Relancer
                    </button>
                ) : (
                    <span className="text-xs text-gray-300">-</span>
                )}
            </Td>
            {/* Cellule de chevron : affordance visuelle "cette ligne mene quelque part" */}
            <td className="w-10 pr-4 text-right">
                <ChevronRight
                    size={16}
                    className="text-gray-300 opacity-0 group-hover:opacity-100 group-focus:opacity-100 transition-opacity inline-block"
                    aria-hidden="true"
                />
            </td>
        </tr>
    );
}

/**
 * Formate une date ISO en texte relatif lisible.
 */
function formatRelative(iso) {
    if (!iso) return "jamais";
    const date = new Date(iso);
    if (isNaN(date.getTime())) return "jamais";

    const diffMs = Date.now() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 10) return "a l'instant";
    if (diffSec < 60) return `il y a ${diffSec} s`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `il y a ${diffH} h`;
    const diffDays = Math.floor(diffH / 24);
    if (diffDays < 7) return `il y a ${diffDays} j`;

    return date.toLocaleDateString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    });
}