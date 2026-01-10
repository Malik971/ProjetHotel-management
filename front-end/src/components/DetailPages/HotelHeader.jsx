function HotelHeader() {
  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <span className="bg-yellow-400 text-xs px-2 py-1 rounded-full">
          5★ Luxe
        </span>
        <span className="text-xs text-gray-500">Recommandé</span>
      </div>

      <h1 className="text-3xl font-bold">Grand Hôtel de Paris</h1>

      <div className="flex items-center gap-4 text-sm text-gray-600">
        <span>📍 Paris, France</span>
        <span>⭐ 4.8 (350 avis)</span>
      </div>
    </div>
  );
}
export default HotelHeader;