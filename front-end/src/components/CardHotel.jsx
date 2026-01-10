import React from "react";
import { Link } from "react-router-dom";
export default function CardHotel({ hotel }) {
  console.log("🧩 CardHotel reçu :", hotel);

  if (!hotel) {
    console.log("❌ hotel est null ou undefined");
    return null;
  }



  const imageSrc = hotel.imageUrl
    ? `http://localhost:8080${hotel.imageUrl}`
    : "https://via.placeholder.com/400x250?text=No+Image";

  return (
    <div className="bg-white shadow rounded overflow-hidden hover:shadow-lg transition-shadow">
      <img
        src={imageSrc}
        alt={hotel.nom || "Hôtel"}
        className="w-full h-48 object-cover"
        onError={(e) => {
          e.target.src = "https://via.placeholder.com/400x250?text=Image+Non+Disponible";
        }}
      />

      <div className="p-4">
        <h3 className="text-lg font-semibold">{hotel.nom || "Nom non disponible"}</h3>
        <p className="text-gray-500">{hotel.ville || "Ville non disponible"}</p>

        <div className="flex justify-between items-center mt-3">
          <span className="text-yellow-500 font-bold">
            ⭐ {hotel.noteMoyenne || "N/A"}
          </span>

          <Link
            to={`/hotel/${hotel.id}`}
            className="text-blue-600 hover:underline font-medium"
          >
            Voir détails
          </Link>
        </div>
      </div>
    </div>
  );
}
