// src/components/HotelMap.jsx
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { useEffect } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

// Fix l'icône par défaut de Leaflet (bug connu avec Webpack/Vite)
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// Icône rouge pour l'hôtel sélectionné
const selectedIcon = new L.Icon({
  iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

// Composant interne pour re-centrer la carte quand la sélection change
function MapFocus({ hotels, selectedHotelId }) {
  const map = useMap();

  useEffect(() => {
    if (!selectedHotelId) return;
    const hotel = hotels.find((h) => h.id === selectedHotelId);
    if (hotel?.latitude && hotel?.longitude) {
      map.flyTo([hotel.latitude, hotel.longitude], 14, { duration: 1 });
    }
  }, [selectedHotelId, hotels, map]);

  return null;
}

function renderStars(categorie = 0) {
  const maxStars = 5;
  let stars = "";

  for (let i = 1; i <= maxStars; i++) {
    if (i <= categorie) {
      stars += "★"; // pleine
    } else {
      stars += "☆"; // vide
    }
  }

  return stars;
}

// Composant principal — reçoit uniquement des props, n'a aucun état propre
export default function HotelMap({ hotels = [], onHotelClick, selectedHotelId }) {
  // Filtrer les hôtels sans coordonnées GPS
  const hotelsAvecCoords = hotels.filter(
      (h) => h.latitude != null && h.longitude != null
  );

  // Centre de la carte : moyenne des coordonnées, ou Paris par défaut
  const center =
      hotelsAvecCoords.length > 0
          ? [
            hotelsAvecCoords.reduce((acc, h) => acc + h.latitude, 0) / hotelsAvecCoords.length,
            hotelsAvecCoords.reduce((acc, h) => acc + h.longitude, 0) / hotelsAvecCoords.length,
          ]
          : [48.8566, 2.3522]; // Paris par défaut

  return (
      <MapContainer
          center={center}
          zoom={12}
          style={{ height: "100%", width: "100%", borderRadius: "0.5rem" }}
          scrollWheelZoom={true}
      >
        {/* Fond de carte OpenStreetMap (gratuit, pas de clé API) */}
        <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Re-centrage automatique sur l'hôtel sélectionné */}
        <MapFocus hotels={hotelsAvecCoords} selectedHotelId={selectedHotelId} />

        {/* Un marqueur par hôtel */}
        {hotelsAvecCoords.map((hotel) => (
            <Marker
                key={hotel.id}
                position={[hotel.latitude, hotel.longitude]}
                icon={selectedHotelId === hotel.id ? selectedIcon : new L.Icon.Default()}
                eventHandlers={{
                  click: () => onHotelClick && onHotelClick(hotel),
                }}
            >
              <Popup>
                <div style={{ minWidth: "160px" }}>
                  {hotel.imageUrl && (
                      <img
                          src={hotel.imageUrl}
                          alt={hotel.nom}
                          style={{
                            width: "100%",
                            height: "80px",
                            objectFit: "cover",
                            borderRadius: "4px",
                            marginBottom: "6px",
                          }}
                      />
                  )}
                  <strong style={{ fontSize: "14px" }}>{hotel.nom}</strong>
                  <p style={{ margin: "2px 0", fontSize: "12px", color: "#555" }}>
                    {hotel.ville}
                  </p>
                  {hotel.prixMoyenNuit && (
                      <p style={{ margin: "2px 0", fontSize: "13px", fontWeight: "bold", color: "#2563eb" }}>
                        À partir de {hotel.prixMoyenNuit}€ / nuit
                      </p>
                  )}
                  {hotel.noteMoyenne && (
                      <p style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                        {[1,2,3,4,5].map((i) => (
                            <span
                                key={i}
                                style={{
                                  color: i <= hotel.categorie ? "#f59e0b" : "#e5e7eb",
                                  fontSize: "16px"
                                }}
                            >
      ★
    </span>
                        ))}
                        <span style={{ marginLeft: "6px", fontSize: "12px", color: "#555" }}>
    {hotel.categorie}/5
  </span>
                      </p>
                  )}
                </div>
              </Popup>
            </Marker>
        ))}

        {/* Message si aucun hôtel n'a de coordonnées */}
        {hotelsAvecCoords.length === 0 && hotels.length > 0 && (
            <div
                style={{
                  position: "absolute",
                  top: "50%",
                  left: "50%",
                  transform: "translate(-50%, -50%)",
                  background: "white",
                  padding: "12px 20px",
                  borderRadius: "8px",
                  zIndex: 1000,
                  fontSize: "14px",
                  color: "#555",
                }}
            >
              Aucun hôtel n'a de coordonnées GPS renseignées
            </div>
        )}
      </MapContainer>
  );
}