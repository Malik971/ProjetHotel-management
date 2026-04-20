// src/components/DetailPages/HotelRooms.jsx
import React, { useState } from "react";
import { useChambres } from "../../hooks/useChambres";
import RoomCard from "./RoomCard";
import ReservationModal from "./ReservationModal";

export default function HotelRooms({ hotelId }) {
    const { chambres, loading, error } = useChambres(hotelId);
    const [selectedChambre, setSelectedChambre] = useState(null);

    const handleReserver = (chambre) => {
        setSelectedChambre(chambre);
    };

    const handleCloseModal = () => {
        setSelectedChambre(null);
    };

    if (loading) {
        return (
            <div className="text-center py-10">
                <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin mx-auto" />
                <p className="mt-4 text-gray-500 text-sm">Chargement des chambres...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-red-50 border border-red-200 text-red-600 p-4 rounded-xl text-sm">
                <p className="font-semibold mb-1">Erreur</p>
                <p className="text-red-400">{error}</p>
            </div>
        );
    }

    if (!chambres || chambres.length === 0) {
        return (
            <div className="text-center py-10 bg-white border border-gray-100 rounded-2xl">
                <p className="text-gray-200 text-4xl mb-3">—</p>
                <p className="text-gray-500 text-sm">Aucune chambre disponible pour cet hôtel.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            {chambres.map((chambre, index) => (
                <RoomCard
                    key={chambre.id || index}
                    chambre={chambre}
                    onReserver={handleReserver}
                />
            ))}

            {selectedChambre && (
                <ReservationModal
                    chambre={selectedChambre}
                    onClose={handleCloseModal}
                    onSuccess={() => {}}
                />
            )}
        </div>
    );
}