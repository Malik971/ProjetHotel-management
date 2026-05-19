/**
 * MesReservationsPage.jsx
 * Page "Mes reservations" pour un utilisateur connecte.
 *
 * Evolution Lot 2 : chaque carte de reservation est maintenant cliquable
 * et mene a la page de suivi /mes-reservations/:id avec timeline visuelle.
 * Un bouton "Voir le suivi" est ajoute en bas de chaque carte pour le rendre
 * explicite.
 *
 * Iteration UX : ajout d'un lien "Retour a l'accueil" en haut de la page
 * (cette page n'est pas dans MainLayout, donc sans navbar). Sans ce lien,
 * l'utilisateur n'avait aucun moyen de revenir en arriere depuis la liste.
 */

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { httpClient } from '../api/httpClient';
import { useAuth } from '../hooks/useAuth';

export default function MesReservationsPage() {
    const { user } = useAuth();
    const [reservations, setReservations] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) return;

        setLoading(true);
        httpClient
            .get('/api/client/reservations/mes-reservations')
            .then((res) => {
                setReservations(res.data || []);
            })
            .catch((err) => {
                console.error('Erreur chargement reservations :', err);
                toast.error(
                    'Impossible de charger vos reservations. Reessayez plus tard.'
                );
            })
            .finally(() => setLoading(false));
    }, [user]);

    if (loading) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center">
                <p className="text-gray-500">Chargement de vos reservations...</p>
            </div>
        );
    }

    if (reservations.length === 0) {
        return (
            <div className="min-h-screen bg-[#F8FAFC] py-12 px-4">
                <div className="max-w-4xl mx-auto">
                    <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">
                        Mes reservations
                    </h1>
                    <div className="bg-white border border-gray-100 rounded-2xl p-8 shadow-sm text-center">
                        <p className="text-gray-600 mb-4">
                            Vous n'avez pas encore de reservation.
                        </p>
                        <Link
                            to="/"
                            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold transition-colors"
                        >
                            Decouvrir nos hotels
                        </Link>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#F8FAFC] py-6 md:py-12">
            <div className="max-w-4xl mx-auto px-4">

                {/* Retour a l'accueil */}
                <Link
                    to="/"
                    className="inline-flex items-center gap-2 text-gray-500 hover:text-[#0EA5E9] text-sm font-medium transition-colors mb-6"
                >
                    <ArrowLeft size={16} />
                    Retour a l'accueil
                </Link>

                <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-6">
                    Mes reservations
                </h1>
                <div className="space-y-4">
                    {reservations.map((reservation) => (
                        <Link
                            key={reservation.id}
                            to={`/mes-reservations/${reservation.id}`}
                            className="block bg-white border border-gray-100 rounded-2xl p-5 md:p-6 shadow-sm hover:shadow-md hover:border-[#0EA5E9] transition-all group"
                        >
                            <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-3 mb-4">
                                <div className="flex-1">
                                    <h2 className="text-lg font-semibold text-gray-900 group-hover:text-[#0369A1] transition-colors">
                                        {reservation.hotelNom || `Reservation #${reservation.id}`}
                                    </h2>
                                    <p className="text-sm text-gray-500 mt-0.5">
                                        {reservation.hotelVille && (
                                            <span>{reservation.hotelVille} · </span>
                                        )}
                                        Du {reservation.dateDebut} au {reservation.dateFin}
                                    </p>
                                </div>
                                <StatusBadge statut={reservation.statut} />
                            </div>

                            <div className="grid grid-cols-2 gap-4 text-sm mb-4">
                                <div>
                                    <span className="text-gray-500">Chambre </span>
                                    <span className="font-medium text-gray-800">
                                        {reservation.chambreNom || 'N/A'}
                                    </span>
                                </div>
                                <div className="text-right sm:text-left">
                                    <span className="text-gray-500">Prix total </span>
                                    <span className="font-semibold text-[#0369A1]">
                                        {reservation.prixTotal} €
                                    </span>
                                </div>
                            </div>

                            <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                                {reservation.codeConfirmation && (
                                    <span className="text-xs text-gray-400">
                                        Code : <span className="font-mono">{reservation.codeConfirmation}</span>
                                    </span>
                                )}
                                <span className="ml-auto flex items-center gap-1 text-sm font-medium text-[#0EA5E9] group-hover:gap-2 transition-all">
                                    Voir le suivi
                                    <ArrowRight size={14} />
                                </span>
                            </div>
                        </Link>
                    ))}
                </div>
            </div>
        </div>
    );
}

/**
 * Badge colore selon le statut de la reservation.
 */
function StatusBadge({ statut }) {
    const styles = {
        EN_ATTENTE: 'bg-amber-50 text-amber-700 border border-amber-200',
        CONFIRMEE: 'bg-green-50 text-green-700 border border-green-200',
        TERMINEE: 'bg-gray-50 text-gray-600 border border-gray-200',
        ANNULEE: 'bg-red-50 text-red-700 border border-red-200',
    };

    const labels = {
        EN_ATTENTE: 'En attente',
        CONFIRMEE: 'Confirmee',
        TERMINEE: 'Terminee',
        ANNULEE: 'Annulee',
    };

    const className = styles[statut] || 'bg-gray-50 text-gray-600 border border-gray-200';

    return (
        <span
            className={`inline-block px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap ${className}`}
        >
            {labels[statut] || statut}
        </span>
    );
}