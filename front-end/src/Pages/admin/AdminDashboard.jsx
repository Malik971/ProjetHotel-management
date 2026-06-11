// src/Pages/admin/AdminDashboard.jsx
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import {
    CheckCircle2, Clock, RefreshCw, AlertTriangle, ArrowRight,
    List, BookOpen, ExternalLink, ArrowUp, Activity, ArrowLeft,
    UserPlus, Building2, BedDouble, BarChart3, PlayCircle, X
} from "lucide-react";

import StatusBadge from "../../components/admin/StatusBadge";
import StatCard from "../../components/admin/StatCard";
import OrchestrationDiagram from "../../components/admin/OrchestrationDiagram";
import EmployeModeBanner from "../../components/admin/EmployeModeBanner";
import PastellLifecycleDemo from "../../components/admin/PastellLifecycleDemo";
import {
    getPastellStatus,
    getRecentActivity,
    forceGlobalPoll,
} from "../../services/adminPastellService";

/**
 * Espace administrateur, page "Tour de controle".
 *
 * Quatre zones :
 *   1. En-tete avec retour accueil + indicateur "Bus en service" + 4 compteurs
 *   2. Schema d'orchestration + actions rapides
 *   3. Activite recente du bus
 *   4. Gestion du site (cartes utilisateur, hotel, chambre, statistiques)
 *
 * Polling toutes les 10 secondes pour rafraichir les compteurs et l'activite.
 *
 * Lot K5 : loadAll passe a Promise.allSettled pour isoler les echecs.
 * Si getPastellStatus() echoue (ex : token sans SCOPE_pastell-admin),
 * getRecentActivity() est quand meme traite et l'activite s'affiche.
 */
export default function AdminDashboard() {
    const navigate = useNavigate();
    const [status, setStatus] = useState(null);
    const [activity, setActivity] = useState([]);
    const [loading, setLoading] = useState(true);
    const [forcing, setForcing] = useState(false);
    const [demoOpen, setDemoOpen] = useState(false);

    const loadAll = async () => {
        try {
            // Promise.allSettled : chaque promesse est resolue independamment.
            // Un echec sur getPastellStatus (ex : 403 sans scope pastell-admin)
            // ne bloque plus le chargement de l'activite recente.
            const [statusResult, activityResult] = await Promise.allSettled([
                getPastellStatus(),
                getRecentActivity(10),
            ]);

            if (statusResult.status === "fulfilled") {
                setStatus(statusResult.value);
            } else {
                console.warn("getPastellStatus echoue :", statusResult.reason);
            }

            if (activityResult.status === "fulfilled") {
                setActivity(activityResult.value);
            } else {
                console.warn("getRecentActivity echoue :", activityResult.reason);
            }

        } catch (e) {
            console.error("Erreur inattendue chargement dashboard admin", e);
            toast.error("Impossible de charger le tableau de bord");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadAll();
        const intervalId = setInterval(loadAll, 10_000);
        return () => clearInterval(intervalId);
    }, []);

    const handleForcePoll = async () => {
        setForcing(true);
        try {
            const result = await forceGlobalPoll();
            toast.success(
                `Synchronisation forcee : ${result.processed} entrees traitees`
            );
            await loadAll();
        } catch (e) {
            const msg = e.response?.data?.hint || "Echec du poll force";
            toast.error(msg);
        } finally {
            setForcing(false);
        }
    };

    const busEnService = status?.pastellEnabled && status?.mockHealth?.reachable;
    const enTraitement = status?.syncCountPending ?? 0;
    const enRetry = status?.syncCountEnRetry ?? 0;
    const anomalies = (status?.syncCountEnErreur ?? 0) + (status?.syncCountDivergence ?? 0);

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-6 md:py-10">
            <div className="max-w-7xl mx-auto px-4 md:px-8">

                {/* Bouton retour accueil */}
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-4"
                >
                    <ArrowLeft size={16} />
                    Retour a l'accueil
                </Link>

                {/* Header */}
                <div className="flex items-start justify-between pb-4 mb-6 border-b border-gray-200">
                    <div>
                        <p className="text-xs font-semibold text-[#0EA5E9] uppercase tracking-wider mb-1">
                            Espace administrateur
                        </p>
                        <h1 className="text-xl md:text-2xl font-semibold text-gray-900">
                            Tour de controle
                        </h1>
                        <p className="text-sm text-gray-500 mt-1">
                            Supervision du bus d'orchestration Pastell
                        </p>
                    </div>
                    <span
                        className={`hidden md:inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-semibold ${
                            busEnService
                                ? "bg-emerald-50 text-emerald-800"
                                : "bg-red-50 text-red-800"
                        }`}
                    >
                        <span
                            className={`w-1.5 h-1.5 rounded-full ${
                                busEnService ? "bg-emerald-500" : "bg-red-500"
                            }`}
                        />
                        {busEnService ? "Bus en service" : "Bus indisponible"}
                    </span>
                </div>

                <EmployeModeBanner />

                {/* 4 compteurs Pastell */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                    <StatCard
                        icon={CheckCircle2}
                        iconColor="text-emerald-500"
                        label="Dossiers OK"
                        value={status?.syncCountOk ?? "..."}
                        hint={loading ? "" : "Synchronises avec succes"}
                    />
                    <StatCard
                        icon={Clock}
                        iconColor="text-[#0EA5E9]"
                        label="En traitement"
                        value={enTraitement}
                        hint="En attente de validation"
                    />
                    <StatCard
                        icon={RefreshCw}
                        iconColor="text-[#F59E0B]"
                        label="Relances"
                        value={enRetry}
                        hint="Action automatique"
                    />
                    <StatCard
                        icon={AlertTriangle}
                        iconColor="text-red-500"
                        label="Anomalies"
                        value={anomalies}
                        hint={anomalies > 0 ? "Intervention requise" : "Aucune anomalie"}
                    />
                </div>

                {/* Schema + actions rapides */}
                <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-3 mb-6">

                    <OrchestrationDiagram />

                    <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm">
                        <h3 className="text-sm font-semibold text-gray-900 mb-4">
                            Actions rapides
                        </h3>

                        <div className="flex flex-col gap-2">
                            <button
                                onClick={() => setDemoOpen(true)}
                                className="flex items-center justify-between gap-2 px-3 py-2.5 bg-sky-50 border border-[#0EA5E9]/30 rounded-xl text-gray-900 text-sm font-medium hover:border-[#0EA5E9] hover:bg-sky-100 transition-all"
                            >
                                <span className="flex items-center gap-2">
                                    <PlayCircle size={16} className="text-[#0EA5E9]" />
                                    Comment ca marche ? (demo guidee)
                                </span>
                                <ArrowRight size={14} className="text-gray-400" />
                            </button>

                            <button
                                onClick={handleForcePoll}
                                disabled={forcing}
                                className="flex items-center justify-between gap-2 px-3 py-2.5 bg-[#F8FAFC] border border-gray-200 rounded-xl text-gray-900 text-sm font-medium hover:border-[#0EA5E9] hover:bg-sky-50 transition-all disabled:opacity-50"
                            >
                                <span className="flex items-center gap-2">
                                    <RefreshCw size={16} className={`text-[#0EA5E9] ${forcing ? "animate-spin" : ""}`} />
                                    {forcing ? "Synchronisation..." : "Forcer une synchronisation"}
                                </span>
                                <ArrowRight size={14} className="text-gray-400" />
                            </button>

                            <Link
                                to="/admin/pastell"
                                className="flex items-center justify-between gap-2 px-3 py-2.5 bg-[#F8FAFC] border border-gray-200 rounded-xl text-gray-900 text-sm font-medium hover:border-[#0EA5E9] hover:bg-sky-50 transition-all"
                            >
                                <span className="flex items-center gap-2">
                                    <List size={16} className="text-[#0EA5E9]" />
                                    Voir tous les dossiers
                                </span>
                                <ArrowRight size={14} className="text-gray-400" />
                            </Link>

                            <Link
                                to="/admin/docs"
                                className="flex items-center justify-between gap-2 px-3 py-2.5 bg-[#F8FAFC] border border-gray-200 rounded-xl text-gray-900 text-sm font-medium hover:border-[#0EA5E9] hover:bg-sky-50 transition-all"
                            >
                                <span className="flex items-center gap-2">
                                    <BookOpen size={16} className="text-[#0EA5E9]" />
                                    Documentation API
                                </span>
                                <ArrowRight size={14} className="text-gray-400" />
                            </Link>

                            <a
                                href="https://pastell-demo.netlify.app"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center justify-between gap-2 px-3 py-2.5 bg-[#F8FAFC] border border-gray-200 rounded-xl text-gray-900 text-sm font-medium hover:border-[#0EA5E9] hover:bg-sky-50 transition-all"
                            >
                                <span className="flex items-center gap-2">
                                    <ExternalLink size={16} className="text-[#0EA5E9]" />
                                    Dashboard demo Pastell
                                </span>
                                <ArrowRight size={14} className="text-gray-400" />
                            </a>
                        </div>
                    </div>

                </div>

                {/* Activite recente */}
                <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-sm mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-2">
                            <Activity size={16} className="text-[#0EA5E9]" />
                            <h3 className="text-sm font-semibold text-gray-900">
                                Activite recente du bus
                            </h3>
                        </div>
                        <Link
                            to="/admin/pastell"
                            className="text-xs font-semibold text-[#0EA5E9] hover:text-[#0369A1]"
                        >
                            Tout voir &rarr;
                        </Link>
                    </div>

                    {loading ? (
                        <p className="text-sm text-gray-400 py-4 text-center">
                            Chargement de l'activite...
                        </p>
                    ) : activity.length === 0 ? (
                        <p className="text-sm text-gray-400 py-6 text-center">
                            Aucune activite enregistree pour le moment
                        </p>
                    ) : (
                        <ul className="divide-y divide-gray-100">
                            {activity.map((item, idx) => (
                                <ActivityRow key={idx} item={item} onClick={() => {
                                    if (item.reservationId) {
                                        navigate(`/admin/pastell/${item.reservationId}`);
                                    }
                                }} />
                            ))}
                        </ul>
                    )}
                </div>

                {/* Gestion du site */}
                <div className="mb-6">
                    <h2 className="text-base font-semibold text-gray-900 mb-4">
                        Gestion du site
                    </h2>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                        <AdminQuickCard
                            icon={UserPlus}
                            title="Ajouter un utilisateur"
                            subtitle="Creer un employe ou admin"
                            onClick={() => navigate("/admin/add-users")}
                        />
                        <AdminQuickCard
                            icon={Building2}
                            title="Gerer les hotels"
                            subtitle="Ajouter, modifier, supprimer"
                            onClick={() => navigate("/admin/hotels")}
                        />
                        <AdminQuickCard
                            icon={BedDouble}
                            title="Gerer les chambres"
                            subtitle="Ajouter, modifier, photos"
                            onClick={() => navigate("/admin/chambres")}
                        />
                        <AdminQuickCard
                            icon={BarChart3}
                            title="Statistiques"
                            subtitle="Bientot disponible"
                            disabled
                        />
                    </div>
                </div>

                {demoOpen && (
                    <div
                        className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
                        onClick={() => setDemoOpen(false)}
                    >
                        <div
                            className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 relative dark:bg-gray-900"
                            onClick={(e) => e.stopPropagation()}
                        >
                            <button
                                onClick={() => setDemoOpen(false)}
                                aria-label="Fermer la demo"
                                className="absolute top-4 right-4 p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition-colors dark:hover:bg-gray-800 dark:hover:text-gray-200"
                            >
                                <X size={18} />
                            </button>
                            <PastellLifecycleDemo />
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
}

// eslint-disable-next-line no-unused-vars -- faux positif : Icon est rendu en JSX plus bas
function AdminQuickCard({ icon: Icon, title, subtitle, onClick, disabled }) {
    const baseClass = "bg-white border rounded-2xl p-4 shadow-sm transition-all text-left";
    if (disabled) {
        return (
            <div className={`${baseClass} border-gray-200 opacity-60 cursor-not-allowed`}>
                <Icon size={20} className="text-gray-400 mb-2" />
                <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
                <p className="text-xs text-gray-500 mt-1">{subtitle}</p>
            </div>
        );
    }
    return (
        <button
            onClick={onClick}
            className={`${baseClass} border-gray-200 hover:border-[#0EA5E9] hover:shadow-md cursor-pointer w-full`}
        >
            <Icon size={20} className="text-[#0EA5E9] mb-2" />
            <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
            <p className="text-xs text-gray-500 mt-1">{subtitle}</p>
        </button>
    );
}

function ActivityRow({ item, onClick }) {
    const iconConfig = getActivityIcon(item.type);
    return (
        <li
            onClick={onClick}
            className="grid grid-cols-[24px_1fr_auto_auto] gap-3 items-center py-2.5 cursor-pointer hover:bg-gray-50 -mx-2 px-2 rounded-lg transition-colors"
        >
            <div className={`w-6 h-6 rounded-full flex items-center justify-center ${iconConfig.bg}`}>
                {iconConfig.icon}
            </div>
            <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{item.title}</p>
                {item.subtitle && (
                    <p className="text-xs text-gray-400 truncate">{item.subtitle}</p>
                )}
            </div>
            <StatusBadge status={item.type} />
            <span className="text-xs text-gray-400 whitespace-nowrap">
                {formatRelative(item.occurredAt)}
            </span>
        </li>
    );
}

function getActivityIcon(type) {
    switch (type) {
        case "OK":
            return { bg: "bg-emerald-50", icon: <CheckCircle2 size={12} className="text-emerald-600" /> };
        case "EN_RETRY":
            return { bg: "bg-amber-50", icon: <RefreshCw size={12} className="text-amber-700" /> };
        case "PENDING":
            return { bg: "bg-sky-50", icon: <ArrowUp size={12} className="text-[#0369A1]" /> };
        case "EN_ERREUR":
        case "DIVERGENCE":
            return { bg: "bg-red-50", icon: <AlertTriangle size={12} className="text-red-700" /> };
        default:
            return { bg: "bg-gray-50", icon: <Clock size={12} className="text-gray-500" /> };
    }
}

function formatRelative(iso) {
    if (!iso) return "";
    const date = new Date(iso);
    const now = new Date();
    const diffMs = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return "a l'instant";
    if (diffMin < 60) return `il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `il y a ${diffH}h`;
    const diffD = Math.floor(diffH / 24);
    return `il y a ${diffD}j`;
}