// src/Pages/admin/AdminReservationsEnAttente.jsx

/**
 * Page admin /admin/reservations/en-attente.
 *
 * Liste les reservations dont le statut est EN_ATTENTE ou SIGNATURE_EN_COURS.
 * Chaque ligne propose un bouton "Signer" qui ouvre la page de signature.
 *
 * Source de donnees :
 *   GET /api/admin/reservations/en-attente?page=0&size=20
 *
 * Flux de navigation :
 *   clic "Signer" -> /admin/reservations/:id/signer (AdminSignaturePage)
 *   retour apres signature reussie -> retour ici, liste rafraichie
 */

import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    ChevronLeft,
    ChevronRight,
    Loader2,
    AlertCircle,
    Inbox,
    PenLine,
    RefreshCw,
} from "lucide-react";
import { getReservationsEnAttente } from "../../services/signatureService";

const PAGE_SIZE = 20;

const STATUT_LABELS = {
    EN_ATTENTE:          { label: "En attente",         cls: "bg-amber-50 text-amber-700 border-amber-200" },
    SIGNATURE_EN_COURS:  { label: "Signature en cours", cls: "bg-sky-50 text-sky-700 border-sky-200"     },
};

export default function AdminReservationsEnAttente() {
    const navigate = useNavigate();

    const [content, setContent]       = useState([]);
    const [meta, setMeta]             = useState({ page: 0, totalPages: 1, totalElements: 0, first: true, last: true });
    const [currentPage, setCurrentPage] = useState(0);
    const [loading, setLoading]       = useState(true);
    const [error, setError]           = useState(null);

    const charger = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getReservationsEnAttente(currentPage, PAGE_SIZE);
            // Le backend retourne une List directe (pas un PagedResponseDTO)
            // car AdminSignatureController retourne ResponseEntity<List<...>>.
            // On accepte les deux formes.
            if (Array.isArray(data)) {
                setContent(data);
                setMeta({ page: 0, totalPages: 1, totalElements: data.length, first: true, last: true });
            } else {
                setContent(data.content ?? []);
                setMeta({
                    page:          data.page          ?? 0,
                    totalPages:    data.totalPages    ?? 1,
                    totalElements: data.totalElements ?? 0,
                    first:         data.first         ?? true,
                    last:          data.last          ?? true,
                });
            }
        } catch (e) {
            console.error("Echec chargement reservations en attente :", e);
            setError("Impossible de charger les dossiers. Reessayez dans un instant.");
        } finally {
            setLoading(false);
        }
    }, [currentPage]);

    useEffect(() => { charger(); }, [charger]);

    return (
        <div className="min-h-screen bg-[#F8FAFC]">
            <div className="max-w-7xl mx-auto px-4 md:px-8 py-6 md:py-10">

                {/* En-tete */}
                <div className="mb-6 md:mb-8">
                    <div className="flex items-start justify-between gap-4 flex-wrap">
                        <div>
                            <h1 className="text-2xl md:text-3xl font-semibold text-gray-900">
                                Dossiers a valider
                            </h1>
                            <p className="text-sm text-gray-600 mt-1 max-w-2xl">
                                Reservations en attente de validation par un agent. Cliquez sur
                                "Signer" pour ouvrir la page de signature et confirmer le dossier.
                            </p>
                        </div>
                        <div className="flex items-center gap-3">
                            <button
                                onClick={charger}
                                disabled={loading}
                                className="inline-flex items-center gap-2 px-3 py-2 text-sm rounded-lg
                                           border border-gray-200 bg-white text-gray-700
                                           hover:border-[#0EA5E9] hover:text-[#0369A1]
                                           disabled:opacity-40 transition"
                            >
                                <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
                                Actualiser
                            </button>
                            <button
                                onClick={() => navigate("/admin")}
                                className="text-sm text-[#0369A1] hover:text-[#0EA5E9] font-medium"
                            >
                                Retour au tableau de bord
                            </button>
                        </div>
                    </div>
                </div>

                {/* Compteur */}
                {!loading && !error && (
                    <p className="text-sm text-gray-500 mb-4">
                        {meta.totalElements} dossier{meta.totalElements > 1 ? "s" : ""} en attente
                    </p>
                )}

                {/* Conteneur tableau */}
                <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">

                    {loading && content.length === 0 && (
                        <div className="flex items-center justify-center py-20 text-gray-500">
                            <Loader2 className="animate-spin mr-2" size={18} />
                            <span className="text-sm">Chargement...</span>
                        </div>
                    )}

                    {error && (
                        <div className="flex items-center justify-center py-20 text-red-600 gap-2">
                            <AlertCircle size={18} />
                            <span className="text-sm">{error}</span>
                        </div>
                    )}

                    {!loading && !error && content.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 text-gray-500 gap-3">
                            <Inbox size={40} className="text-gray-300" />
                            <p className="text-sm font-medium">Aucun dossier en attente</p>
                            <p className="text-xs text-gray-400">
                                Tous les dossiers ont ete traites.
                            </p>
                        </div>
                    )}

                    {content.length > 0 && (
                        <>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead className="bg-[#F8FAFC] border-b border-gray-200">
                                        <tr>
                                            <Th>Ref</Th>
                                            <Th>Client</Th>
                                            <Th>Hotel</Th>
                                            <Th>Chambre</Th>
                                            <Th>Arrivee</Th>
                                            <Th>Depart</Th>
                                            <Th>Prix</Th>
                                            <Th>Statut</Th>
                                            <Th className="text-right">Action</Th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {content.map((r) => (
                                            <ReservationRow
                                                key={r.id}
                                                reservation={r}
                                                onSigner={() =>
                                                    navigate(`/admin/reservations/${r.id}/signer`)
                                                }
                                            />
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* Pagination */}
                            {meta.totalPages > 1 && (
                                <div className="flex items-center justify-between px-4 md:px-6 py-3 border-t border-gray-200 bg-[#F8FAFC]">
                                    <p className="text-xs text-gray-600">
                                        Page {meta.page + 1} sur {Math.max(1, meta.totalPages)}
                                    </p>
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                                            disabled={meta.first || loading}
                                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium
                                                       bg-white border border-gray-200 text-gray-700
                                                       hover:border-[#0EA5E9] hover:text-[#0369A1]
                                                       disabled:opacity-40 disabled:cursor-not-allowed transition"
                                        >
                                            <ChevronLeft size={14} />
                                            Precedent
                                        </button>
                                        <button
                                            onClick={() => setCurrentPage((p) => p + 1)}
                                            disabled={meta.last || loading}
                                            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-medium
                                                       bg-white border border-gray-200 text-gray-700
                                                       hover:border-[#0EA5E9] hover:text-[#0369A1]
                                                       disabled:opacity-40 disabled:cursor-not-allowed transition"
                                        >
                                            Suivant
                                            <ChevronRight size={14} />
                                        </button>
                                    </div>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

// --- Sous-composants ---

function Th({ children, className = "" }) {
    return (
        <th
            className={`px-4 md:px-6 py-3 text-left text-xs font-semibold text-gray-500
                        uppercase tracking-wider whitespace-nowrap ${className}`}
        >
            {children}
        </th>
    );
}

function Td({ children, className = "" }) {
    return (
        <td className={`px-4 md:px-6 py-3 whitespace-nowrap text-gray-700 ${className}`}>
            {children}
        </td>
    );
}

function ReservationRow({ reservation: r, onSigner }) {
    const statut = STATUT_LABELS[r.statut] ?? { label: r.statut, cls: "bg-gray-100 text-gray-600 border-gray-200" };

    return (
        <tr className="hover:bg-sky-50/40 transition">
            <Td>
                <span className="font-mono text-xs text-[#0369A1]">
                    #{r.id}
                </span>
                <div className="text-xs text-gray-400 font-mono mt-0.5">
                    {r.codeConfirmation}
                </div>
            </Td>
            <Td>
                <div className="flex flex-col">
                    <span className="font-medium text-gray-900">{r.nomClient}</span>
                    <span className="text-xs text-gray-500">{r.emailClient}</span>
                </div>
            </Td>
            <Td>{r.hotelNom ?? "-"}</Td>
            <Td>{r.chambreNom ?? "-"}</Td>
            <Td>
                <span className="text-xs">
                    {r.dateDebut ? new Date(r.dateDebut).toLocaleDateString("fr-FR") : "-"}
                </span>
            </Td>
            <Td>
                <span className="text-xs">
                    {r.dateFin ? new Date(r.dateFin).toLocaleDateString("fr-FR") : "-"}
                </span>
            </Td>
            <Td>
                <span className="text-xs font-medium">
                    {r.prixTotal != null
                        ? Number(r.prixTotal).toLocaleString("fr-FR", { style: "currency", currency: "EUR" })
                        : "-"}
                </span>
            </Td>
            <Td>
                <span
                    className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border ${statut.cls}`}
                >
                    {statut.label}
                </span>
            </Td>
            <Td className="text-right">
                <button
                    onClick={onSigner}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold
                               bg-[#0EA5E9] text-white hover:bg-[#0369A1] transition"
                >
                    <PenLine size={12} />
                    Signer
                </button>
            </Td>
        </tr>
    );
}