import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function MesReservationsPage() {
  const [reservations, setReservations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchReservations();
  }, []);

  const fetchReservations = async () => {
    try {
      const token = localStorage.getItem('token');

      if (!token) {
        navigate('/connexion');
        return;
      }

      const response = await fetch('http://localhost:8080/api/client/reservations/mes-reservations', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Erreur lors du chargement des réservations');
      }

      const data = await response.json();
      setReservations(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const annulerReservation = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir annuler cette réservation ?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/client/reservations/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Erreur lors de l\'annulation');
      }

      fetchReservations();
      alert('Réservation annulée avec succès');
    } catch (err) {
      alert(err.message);
    }
  };

  const getStatutBadge = (statut) => {
    const styles = {
      'CONFIRMEE': 'bg-green-100 text-green-800',
      'EN_ATTENTE': 'bg-yellow-100 text-yellow-800',
      'ANNULEE': 'bg-red-100 text-red-800',
    };
    return styles[statut] || 'bg-gray-100 text-gray-800';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-6 py-10">
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-7xl mx-auto px-6">
        <h1 className="text-3xl font-bold mb-8">📅 Mes Réservations</h1>

        {reservations.length === 0 ? (
          <div className="bg-white rounded-xl shadow-lg p-10 text-center">
            <div className="text-6xl mb-4">📅</div>
            <h2 className="text-2xl font-semibold text-gray-700 mb-2">
              Aucune réservation
            </h2>
            <p className="text-gray-500 mb-6">
              Vous n'avez pas encore effectué de réservation
            </p>
            <button
              onClick={() => navigate('/')}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition"
            >
              Découvrir nos hôtels
            </button>
          </div>
        ) : (
          <div className="space-y-6">
            {reservations.map((reservation) => (
              <div
                key={reservation.id}
                className="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition"
              >
                <div className="md:flex">
                  <div className="md:w-1/3">
                    <img
                      src={reservation.hotelImageUrl
                        ? `http://localhost:8080${reservation.hotelImageUrl}`
                        : 'https://via.placeholder.com/400x300?text=Hotel'}
                      alt={reservation.hotelNom}
                      className="w-full h-full object-cover"
                    />
                  </div>

                  <div className="md:w-2/3 p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-2xl font-bold text-gray-800">
                          {reservation.hotelNom}
                        </h3>
                        <p className="text-gray-600">
                          📍 {reservation.hotelVille}
                        </p>
                      </div>
                      <span className={`px-4 py-2 rounded-full text-sm font-semibold ${getStatutBadge(reservation.statut)}`}>
                        {reservation.statut}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-4">
                      <div>
                        <p className="text-sm text-gray-500">Chambre</p>
                        <p className="font-semibold">{reservation.chambreType}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">Numéro</p>
                        <p className="font-semibold">#{reservation.id}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">Arrivée</p>
                        <p className="font-semibold">
                          {new Date(reservation.dateArrivee).toLocaleDateString('fr-FR')}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">Départ</p>
                        <p className="font-semibold">
                          {new Date(reservation.dateDepart).toLocaleDateString('fr-FR')}
                        </p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">Personnes</p>
                        <p className="font-semibold">{reservation.nombrePersonnes}</p>
                      </div>
                      <div>
                        <p className="text-sm text-gray-500">Prix total</p>
                        <p className="font-semibold text-blue-600 text-xl">
                          {reservation.prixTotal} €
                        </p>
                      </div>
                    </div>

                    <div className="flex gap-3">
                      <button
                        onClick={() => navigate(`/hotel/${reservation.hotelId}`)}
                        className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition"
                      >
                        Voir l'hôtel
                      </button>
                      {reservation.statut !== 'ANNULEE' && (
                        <button
                          onClick={() => annulerReservation(reservation.id)}
                          className="flex-1 bg-red-600 text-white py-2 rounded-lg hover:bg-red-700 transition"
                        >
                          Annuler
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}