import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import HotelRooms from '../components/DetailPages/HotelRooms';
// ... autres imports

export default function DetailsPage() {
  const { hotelId } = useParams();
  const navigate = useNavigate();
  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);

  // ✅ DEBUG
  useEffect(() => {
    console.log('📍 DetailsPage - hotelId from URL:', hotelId);
  }, [hotelId]);

  useEffect(() => {
    if (!hotelId) {
      console.error('❌ Pas de hotelId !');
      return;
    }

    fetch(`http://localhost:8080/api/hotels/${hotelId}`)
      .then((response) => {
        if (!response.ok) throw new Error('Hôtel non trouvé');
        return response.json();
      })
      .then((data) => {
        console.log('✅ Hôtel chargé:', data);
        setHotel(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error('❌ Erreur:', error);
        setLoading(false);
      });
  }, [hotelId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!hotel) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold mb-4">Hôtel non trouvé</h2>
          <button
            onClick={() => navigate('/')}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg"
          >
            Retour à l'accueil
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen bg-gray-50">
      {/* ... header ... */}

      <section className="max-w-7xl mx-auto px-6 py-10 grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-10">
          {/* ... autres composants ... */}

          {/* ✅ Passer hotelId en tant que NUMBER */}
          <HotelRooms hotelId={parseInt(hotelId)} />
        </div>

        <div className="lg:col-span-1">
          {/* ... summary card ... */}
        </div>
      </section>
    </div>
  );
}