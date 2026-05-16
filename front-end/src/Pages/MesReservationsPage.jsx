/**
 * MesReservationsPage.jsx
 * Page "Mes reservations" pour un utilisateur connecte.
 *
 * Correction Lot 1 :
 *   - appelle desormais /api/client/reservations/mes-reservations
 *     (endpoint client deja existant cote backend) au lieu de
 *     /api/admin/reservations/user/{id} qui exige ROLE_ADMIN.
 *
 * L'endpoint client lit l'utilisateur depuis le JWT (via Authentication),
 * donc plus besoin de passer user.id dans l'URL.
 *
 * Tous les champs (hotelNom, hotelVille, chambreNom, dateDebut, dateFin,
 * prixTotal, statut, codeConfirmation) viennent du ReservationResponseDTO
 * du backend.
 */

import { useEffect, useState } from 'react';
import { toast } from 'sonner';
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
            <div className="flex items-center justify-center py-20">
                <p className="text-gray-500">Chargement de vos reservations...</p>
            </div>
        );
    }

    if (reservations.length === 0) {
        return (
            <div className="max-w-4xl mx-auto py-12 px-4">
                <h1 className="text-3xl font-bold text-gray-800 mb-6">
                    Mes reservations
                </h1>
                <div className="bg-white rounded-lg shadow p-8 text-center">
                    <p className="text-gray-600">
                        Vous n'avez pas encore de reservation.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto py-12 px-4">
            <h1 className="text-3xl font-bold text-gray-800 mb-6">
                Mes reservations
            </h1>
            <div className="space-y-4">
                {reservations.map((reservation) => (
                    <div
                        key={reservation.id}
                        className="bg-white rounded-lg shadow p-6"
                    >
                        <div className="flex justify-between items-start mb-4">
                            <div>
                                <h2 className="text-lg font-semibold text-gray-800">
                                    {reservation.hotelNom || `Reservation #${reservation.id}`}
                                </h2>
                                <p className="text-sm text-gray-500">
                                    {reservation.hotelVille && (
                                        <span>{reservation.hotelVille} - </span>
                                    )}
                                    Du {reservation.dateDebut} au {reservation.dateFin}
                                </p>
                            </div>
                            <StatusBadge statut={reservation.statut} />
                        </div>

                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <span className="text-gray-500">Chambre :</span>{' '}
                                <span className="font-medium">
                                    {reservation.chambreNom || 'N/A'}
                                </span>
                            </div>
                            <div>
                                <span className="text-gray-500">Prix total :</span>{' '}
                                <span className="font-medium">
                                    {reservation.prixTotal} EUR
                                </span>
                            </div>
                        </div>

                        {reservation.codeConfirmation && (
                            <div className="mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500">
                                Code de confirmation : <span className="font-mono">{reservation.codeConfirmation}</span>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}

/**
 * Badge colore selon le statut de la reservation.
 * Au lot 2 on remplacera ca par une timeline visuelle complete.
 */
function StatusBadge({ statut }) {
    const styles = {
        EN_ATTENTE: 'bg-yellow-100 text-yellow-800',
        CONFIRMEE: 'bg-green-100 text-green-800',
        TERMINEE: 'bg-gray-100 text-gray-800',
        ANNULEE: 'bg-red-100 text-red-800',
    };

    const labels = {
        EN_ATTENTE: 'En attente',
        CONFIRMEE: 'Confirmee',
        TERMINEE: 'Terminee',
        ANNULEE: 'Annulee',
    };

    const className = styles[statut] || 'bg-gray-100 text-gray-800';

    return (
        <span
            className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${className}`}
        >
            {labels[statut] || statut}
        </span>
    );
}