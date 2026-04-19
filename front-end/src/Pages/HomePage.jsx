// src/Pages/HomePage.jsx
import { useEffect, useState } from "react";
import BarRecherche from "../components/BarRecherche";
import CardHotel from "../components/CardHotel";
import HotelMap from "../components/HotelMap";
import { useHotelSearch } from "../hooks/useHotelSearch";
import Filter from "../components/FilterPro.jsx";
import { SlidersHorizontal, List, Map, X } from "lucide-react";

export default function HomePage() {
  const [allHotels, setAllHotels] = useState([]);
  const [displayedHotels, setDisplayedHotels] = useState([]);
  const [showMap, setShowMap] = useState(false);
  const [selectedHotelId, setSelectedHotelId] = useState(null);
  const [currentFilters, setCurrentFilters] = useState({});
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const { hotels, loading, error, searchHotels, getAllHotels } = useHotelSearch();

  useEffect(() => {
    loadAllHotels();
  }, []);

  const loadAllHotels = async () => {
    try {
      await getAllHotels();
    } catch (error) {
      console.error("Erreur lors du chargement des hôtels:", error);
      fetch(`${import.meta.env.VITE_API_URL}/api/hotels`)
          .then((res) => res.json())
          .then((data) => {
            setAllHotels(data);
            setDisplayedHotels(data);
          })
          .catch((err) => console.error("Erreur:", err));
    }
  };

  useEffect(() => {
    if (hotels && hotels.length > 0) {
      setAllHotels(hotels);
      applyFilters(hotels, currentFilters);
    }
  }, [hotels]);

  const handleSearch = async (searchParams) => {
    try {
      await searchHotels(searchParams);
    } catch (error) {
      console.error("Erreur lors de la recherche:", error);
    }
  };

  const applyFilters = (hotelsToFilter, filters) => {
    let result = [...(hotelsToFilter || allHotels)];

    if (!filters || Object.keys(filters).length === 0) {
      setDisplayedHotels(result);
      return;
    }

    result = result.filter((hotel) => {
      if (filters.prixMax && hotel.prixMoyenNuit > filters.prixMax) return false;
      if (filters.prixMin && hotel.prixMoyenNuit < filters.prixMin) return false;
      if (filters.categorie?.length > 0 && !filters.categorie.includes(hotel.categorie)) return false;
      if (filters.notationMin && hotel.noteMoyenne < filters.notationMin) return false;
      if (filters.equipements?.length > 0) {
        const hotelEquip = (hotel.equipements || []).map((e) => e.toLowerCase());
        if (!filters.equipements.every((eq) => hotelEquip.includes(eq.toLowerCase()))) return false;
      }
      if (filters.ville?.trim()) {
        if (!hotel.ville?.toLowerCase().includes(filters.ville.toLowerCase())) return false;
      }
      return true;
    });

    if (filters.tri) {
      result.sort((a, b) => {
        switch (filters.tri) {
          case "prix_asc":  return (a.prixMoyenNuit || 0) - (b.prixMoyenNuit || 0);
          case "prix_desc": return (b.prixMoyenNuit || 0) - (a.prixMoyenNuit || 0);
          case "note_desc": return (b.noteMoyenne || 0) - (a.noteMoyenne || 0);
          case "nom_asc":   return (a.nom || "").localeCompare(b.nom || "");
          default:          return 0;
        }
      });
    }

    setDisplayedHotels(result);
  };

  const handleFilterChange = (filters) => {
    setCurrentFilters(filters);
    applyFilters(allHotels, filters);
    setMobileFilterOpen(false);
  };

  const handleResetFilters = () => {
    setCurrentFilters({});
    setDisplayedHotels(allHotels);
  };

  const handleHotelClick = (hotel) => {
    setSelectedHotelId(hotel.id);
    if (!showMap) {
      const el = document.getElementById(`hotel-${hotel.id}`);
      if (el) el.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  };

  const toggleView = (isMapView) => {
    setShowMap(isMapView);
    setSelectedHotelId(null);
  };

  return (
      <div className="min-h-screen bg-[#F8FAFC]">

        {/* Hero + barre de recherche */}
        <BarRecherche onSearch={handleSearch} />

        {/* Contenu principal */}
        <div className="w-full max-w-[1400px] mx-auto px-4 md:px-8 mt-8 pb-16">

          {/* Barre de contrôles */}
          <div className="flex items-center justify-between mb-6 gap-4">
            <div>
              <h2 className="text-lg font-semibold text-gray-800 tracking-wide">
                Nos établissements
              </h2>
              <p className="text-gray-400 text-xs mt-0.5">
                {displayedHotels.length} hôtel{displayedHotels.length !== 1 ? "s" : ""} disponible{displayedHotels.length !== 1 ? "s" : ""}
              </p>
            </div>

            <div className="flex items-center gap-2">
              {/* Bouton filtre mobile */}
              <button
                  onClick={() => setMobileFilterOpen(true)}
                  className="md:hidden flex items-center gap-2 bg-white border border-gray-200 text-gray-700 text-xs font-semibold px-3 py-2 rounded-xl hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-colors shadow-sm"
              >
                <SlidersHorizontal size={14} className="text-[#0EA5E9]" />
                Filtres
              </button>

              {/* Toggle liste / carte */}
              <div className="flex bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
                <button
                    onClick={() => toggleView(false)}
                    className={`flex items-center gap-1.5 px-4 py-2 text-xs font-semibold transition-all duration-200 ${
                        !showMap
                            ? "bg-[#0EA5E9] text-white"
                            : "text-gray-500 hover:text-gray-800 hover:bg-gray-50"
                    }`}
                >
                  <List size={14} />
                  <span className="hidden sm:inline">Liste</span>
                </button>
                <button
                    onClick={() => toggleView(true)}
                    className={`flex items-center gap-1.5 px-4 py-2 text-xs font-semibold transition-all duration-200 ${
                        showMap
                            ? "bg-[#0EA5E9] text-white"
                            : "text-gray-500 hover:text-gray-800 hover:bg-gray-50"
                    }`}
                >
                  <Map size={14} />
                  <span className="hidden sm:inline">Carte</span>
                </button>
              </div>
            </div>
          </div>

          {/* Layout sidebar + contenu */}
          <div className="flex gap-6">

            {/* Sidebar filtres — desktop */}
            <div className="hidden md:block w-72 flex-shrink-0">
              <Filter
                  onFilterChange={handleFilterChange}
                  onReset={handleResetFilters}
              />
            </div>

            {/* Drawer filtres — mobile */}
            {mobileFilterOpen && (
                <div className="fixed inset-0 z-50 md:hidden">
                  <div
                      className="absolute inset-0 bg-black/50 backdrop-blur-sm"
                      onClick={() => setMobileFilterOpen(false)}
                  />
                  <div className="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-200 rounded-t-2xl max-h-[85vh] overflow-y-auto">
                    <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
                  <span className="text-xs font-semibold text-[#0EA5E9] uppercase tracking-widest">
                    Filtres
                  </span>
                      <button
                          onClick={() => setMobileFilterOpen(false)}
                          className="text-gray-400 hover:text-gray-700 transition-colors"
                      >
                        <X size={18} />
                      </button>
                    </div>
                    <div className="p-4">
                      <Filter
                          onFilterChange={handleFilterChange}
                          onReset={handleResetFilters}
                      />
                    </div>
                  </div>
                </div>
            )}

            {/* Contenu principal */}
            <div className="flex-1 min-w-0">

              {/* Chargement */}
              {loading && (
                  <div className="flex justify-center items-center h-64">
                    <div className="text-center">
                      <div className="w-10 h-10 border-2 border-[#BAE6FD] border-t-[#0EA5E9] rounded-full animate-spin mx-auto mb-4" />
                      <p className="text-gray-400 text-sm">Chargement...</p>
                    </div>
                  </div>
              )}

              {/* Erreur */}
              {error && (
                  <div className="bg-red-50 border border-red-200 text-red-600 p-4 rounded-xl text-sm">
                    <p className="font-semibold mb-1">Erreur</p>
                    <p className="text-red-400">{error}</p>
                  </div>
              )}

              {/* Aucun résultat */}
              {!loading && !error && displayedHotels.length === 0 && (
                  <div className="text-center py-20 bg-white border border-gray-100 rounded-2xl shadow-sm">
                    <p className="text-gray-200 text-5xl mb-4">—</p>
                    <h2 className="text-lg font-semibold text-gray-700 mb-2">
                      Aucun établissement trouvé
                    </h2>
                    <p className="text-gray-400 text-sm mb-6">
                      Aucun hôtel ne correspond à vos critères
                    </p>
                    <button
                        onClick={handleResetFilters}
                        className="bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold px-6 py-2.5 rounded-xl transition-colors"
                    >
                      Réinitialiser les filtres
                    </button>
                  </div>
              )}

              {/* Résultats */}
              {!loading && !error && displayedHotels.length > 0 && (
                  <>
                    {!showMap ? (
                        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5 items-stretch">
                          {displayedHotels.map((hotel) => (
                              <div
                                  key={hotel.id}
                                  id={`hotel-${hotel.id}`}
                                  className="flex"
                                  onClick={() => setSelectedHotelId(hotel.id)}
                              >
                                <CardHotel
                                    hotel={hotel}
                                    isSelected={selectedHotelId === hotel.id}
                                />
                              </div>
                          ))}
                        </div>
                    ) : (
                        <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm">
                          <div className="h-[600px] rounded-xl overflow-hidden">
                            <HotelMap
                                hotels={displayedHotels}
                                onHotelClick={handleHotelClick}
                                selectedHotelId={selectedHotelId}
                            />
                          </div>
                          <div className="mt-4 flex items-center justify-center gap-6 text-xs text-gray-400">
                            <div className="flex items-center gap-2">
                              <div className="w-2.5 h-2.5 bg-[#0EA5E9] rounded-full" />
                              <span>Hôtel disponible</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <div className="w-2.5 h-2.5 bg-[#F59E0B] rounded-full" />
                              <span>Hôtel sélectionné</span>
                            </div>
                          </div>
                        </div>
                    )}
                  </>
              )}
            </div>
          </div>
        </div>
      </div>
  );
}