function HotelSummaryCard() {
  return (
    <div
      className="
      sticky top-24
      bg-white rounded-xl shadow
      p-5 space-y-4
    "
    >
      <p className="text-sm text-gray-500">À partir de</p>
      <p className="text-2xl font-bold text-blue-600">245€ / nuit</p>

      <ul className="text-sm text-gray-600 space-y-2">
        <li>⭐ Noté 4.8/5</li>
        <li>✔ Annulation gratuite</li>
        <li>✔ Établissement recommandé</li>
      </ul>

      <button className="w-full bg-black text-white py-3 rounded-lg">
        Voir les disponibilités
      </button>
    </div>
  );
}
export default HotelSummaryCard;