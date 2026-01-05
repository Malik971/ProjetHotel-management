import React from "react";
import { Link } from "react-router-dom";

export default function CardHotel({ hotel }) {
  const imageSrc = hotel.imageUrl
    ? `http://localhost:8080${hotel.imageUrl}`
    : "https://via.placeholder.com/400x250?text=No+Image";

  return (
    <div className="bg-white shadow rounded overflow-hidden">
      <img
        src={imageSrc}
        alt={hotel.nom}
        className="w-full h-48 object-cover"
      />

      <div className="p-4">
        <h3 className="text-lg font-semibold">{hotel.nom}</h3>
        <p className="text-gray-500">{hotel.ville}</p>

        <div className="flex justify-between items-center mt-3">
          <span className="text-yellow-500 font-bold">
            ⭐ {hotel.noteMoyenne}
          </span>

           <Link
                      to={`/hotel/${hotel.id}`}
                      className="text-blue-600 hover:underline"
                    >
                      Voir détails
                    </Link>
        </div>
      </div>
    </div>
  );
}
