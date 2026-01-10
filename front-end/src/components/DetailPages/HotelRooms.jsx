import React, { useState, useEffect } from 'react';
import { useChambres } from '../../hooks/useChambres';
import RoomCard from './RoomCard';
import ReservationModal from './ReservationModal';

export default function HotelRooms({ hotelId }) {
  const { chambres, loading, error } = useChambres(hotelId);
  const [selectedChambre, setSelectedChambre] = useState(null);
    const rooms = [
        {
            id: 1,
            name: "Chambre Classique",
            capacity: 2,
            size: 25,
            bedType: "Lit Queen",
            amenities: ["Climatisation", "Minibar", "Télévision écran plat"],
            pricePerNight: 245,
            imageUrl: "https://picsum.photos/400/200",
        },
        {
            id: 2,
            name: "Suite Deluxe",
            capacity: 4,
            size: 45,
            bedType: "2 Lits King",
            amenities: ["Climatisation", "Minibar", "Télévision écran plat", "Jacuzzi"],
            pricePerNight: 450,
            imageUrl: "https://picsum.photos/400/201",
        },
        {
            id: 3,
            name: "Chambre Familiale",
            capacity: 5,
            size: 35,
            bedType: "1 Lit King + 1 Canapé-lit",
            amenities: ["Climatisation", "Minibar", "Télévision écran plat", "Kitchenette"],
            pricePerNight: 320,
            imageUrl: "https://picsum.photos/400/202",
        },
    ]
  // ✅ DEBUG : Afficher ce qui est reçu
  useEffect(() => {
    console.log('🏨 HotelRooms - hotelId:', hotelId);
    console.log('🛏️ HotelRooms - chambres:', chambres);
    console.log('⏳ HotelRooms - loading:', loading);
    console.log('❌ HotelRooms - error:', error);
  }, [hotelId, chambres, loading, error]);

  const handleReserver = (chambre) => {
    console.log('📅 Réservation pour:', chambre);
    setSelectedChambre(chambre);
  };

  const handleCloseModal = () => {
    setSelectedChambre(null);
  };

  if (loading) {
    return (
      <div className="text-center py-10">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">Chargement des chambres...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-800 p-4 rounded-lg">
        ❌ Erreur : {error}
      </div>
    );
  }

  if (!chambres || chambres.length === 0) {
    return (
      <div className="text-center py-10">
        <p className="text-gray-500 mb-4">Aucune chambre disponible pour cet hôtel.</p>
        <p className="text-sm text-gray-400">Hotel ID: {hotelId}</p>
      </div>
    );
  }

  return (
    <section>
      <h2 className="text-2xl font-bold mb-6">
        Chambres disponibles ({chambres.length})
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {chambres.map((chambre, index) => {
          console.log(`🛏️ Rendu chambre ${index}:`, chambre); // DEBUG
          return (
            <RoomCard
              key={chambre.id || index}
              chambre={chambre}
              onReserver={handleReserver}
            />
          );
        })}
      </div>

      {/* Modal de réservation */}
      {selectedChambre && (
        <ReservationModal
          chambre={selectedChambre}
          onClose={handleCloseModal}
          onSuccess={() => {
            console.log('✅ Réservation réussie');
          }}
        />
      )}
    </section>
  );
}