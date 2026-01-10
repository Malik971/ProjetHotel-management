function HotelEquipments() {
  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Équipements & Services</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div label="WiFi gratuit" desc="Connexion haut débit" />
        <div label="Restaurant gastronomique" desc="Cuisine raffinée" />
        <div label="Parking avec voiturier" desc="24h/24" />
        <div label="Spa & Wellness" desc="Massages et soins" />
      </div>
    </div>
  );
}

export default HotelEquipments;