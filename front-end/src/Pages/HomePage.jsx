  // front-end/src/Pages/HomePage.jsx (version complète)
  import { useEffect, useState } from "react";
  import BarRecherche from "../components/BarRecherche";
  import CardHotel from "../components/CardHotel";
  import HotelMap from "../components/HotelMap";
  import { useHotelSearch } from "../hooks/useHotelSearch";
  import Filter from "../components/FilterPro.jsx";

  export default function HomePage() {
    const [allHotels, setAllHotels] = useState([]);
    const [displayedHotels, setDisplayedHotels] = useState([]);
    const [showMap, setShowMap] = useState(false);
    const [selectedHotelId, setSelectedHotelId] = useState(null);
    const [currentFilters, setCurrentFilters] = useState({});
    const { hotels, loading, error, searchHotels, getAllHotels } = useHotelSearch();

    // Charger tous les hôtels au démarrage
    useEffect(() => {
      loadAllHotels();
    }, []);

    const loadAllHotels = async () => {
      try {
        await getAllHotels();
      } catch (error) {
        console.error("Erreur lors du chargement des hôtels:", error);
        // Fallback sur l'ancienne méthode si le hook échoue
        fetch(`${import.meta.env.VITE_API_URL}/api/hotels`)
          .then((response) => response.json())
          .then((data) => {
            setAllHotels(data);
            setDisplayedHotels(data);
          })
          .catch((error) => console.error("Erreur:", error));
      }
    };

    // Mettre à jour les hôtels affichés après une recherche
    useEffect(() => {
      if (hotels && hotels.length > 0) {
        setAllHotels(hotels);
        applyFilters(hotels, currentFilters);
      }
    }, [hotels]);

    // Fonction de recherche depuis la barre de recherche
    const handleSearch = async (searchParams) => {
      try {
        await searchHotels(searchParams);
      } catch (error) {
        console.error("Erreur lors de la recherche:", error);
      }
    };

    // Fonction pour appliquer les filtres
    const applyFilters = (hotelsToFilter, filters) => {
      let result = [...(hotelsToFilter || allHotels)];

      // Pas de filtre actif → tout afficher
      if (!filters || Object.keys(filters).length === 0) {
        setDisplayedHotels(result);
        return;
      }

      result = result.filter((hotel) => {
        // ── Prix ──
        if (filters.prixMax && hotel.prixMoyenNuit > filters.prixMax) return false;
        if (filters.prixMin && hotel.prixMoyenNuit < filters.prixMin) return false;

        // ── Catégorie (tableau de nombres) ── clé alignée avec Filter
        if (filters.categorie && filters.categorie.length > 0) {
          if (!filters.categorie.includes(hotel.categorie)) return false;
        }

        // ── Note ──
        if (filters.notationMin && hotel.noteMoyenne < filters.notationMin) return false;

        // ── Équipements ── clé alignée + comparaison insensible à la casse
        if (filters.equipements && filters.equipements.length > 0) {
          const hotelEquip = (hotel.equipements || []).map((e) => e.toLowerCase());
          const tousPresents = filters.equipements.every((eq) =>
              hotelEquip.includes(eq.toLowerCase())
          );
          if (!tousPresents) return false;
        }

        // ── Ville ──
        if (filters.ville && filters.ville.trim() !== "") {
          if (!hotel.ville?.toLowerCase().includes(filters.ville.toLowerCase())) return false;
        }

        return true;
      });

      // ── Tri ──
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

// handleFilterChange — inchangé mais appelle la nouvelle version
    const handleFilterChange = (filters) => {
      setCurrentFilters(filters);
      applyFilters(allHotels, filters);
    };

    // Réinitialisation des filtres
    const handleResetFilters = () => {
      setCurrentFilters({});
      setDisplayedHotels(allHotels);
    };

    // Clic sur un hôtel depuis la carte
    const handleHotelClick = (hotel) => {
      setSelectedHotelId(hotel.id);

      // Si on est en vue liste, scroll vers la card
      if (!showMap) {
        const element = document.getElementById(`hotel-${hotel.id}`);
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }
    };

    // Basculer entre carte et liste
    const toggleView = (isMapView) => {
      setShowMap(isMapView);
      setSelectedHotelId(null); // Réinitialiser la sélection
    };

    return (
      <div className="min-h-screen bg-gray-50">
        {/* Barre de recherche */}
        <BarRecherche onSearch={handleSearch} />

        <div className="w-[90%] mx-auto mt-10">
          {/* Boutons pour basculer entre liste et carte */}
          <div className="flex justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-800">
                🏨 Nos hôtels
              </h1>
              <p className="text-gray-600 mt-1">
                {displayedHotels.length} hôtel(s) disponible(s)
              </p>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => toggleView(false)}
                className={`px-6 py-2 rounded-lg font-semibold transition-all ${
                  !showMap
                    ? "bg-blue-600 text-white shadow-lg"
                    : "bg-white text-gray-700 border border-gray-300 hover:border-blue-400"
                }`}
              >
                📋 Liste
              </button>
              <button
                onClick={() => toggleView(true)}
                className={`px-6 py-2 rounded-lg font-semibold transition-all ${
                  showMap
                    ? "bg-blue-600 text-white shadow-lg"
                    : "bg-white text-gray-700 border border-gray-300 hover:border-blue-400"
                }`}
              >
                🗺️ Carte
              </button>
            </div>
          </div>

          <div className="flex gap-6">
            {/* Filtres latéraux */}
            <div className="w-80 flex-shrink-0">
              <Filter
                onFilterChange={handleFilterChange}
                onReset={handleResetFilters}
              />
            </div>

            {/* Contenu principal */}
            <div className="flex-1">
              {/* État de chargement */}
              {loading && (
                <div className="flex justify-center items-center h-96">
                  <div className="text-center">
                    <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
                    <p className="text-gray-600 font-semibold">
                      Chargement des hôtels...
                    </p>
                  </div>
                </div>
              )}

              {/* Erreur */}
              {error && (
                <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded-lg shadow">
                  <div className="flex items-center">
                    <span className="text-2xl mr-3">⚠️</span>
                    <div>
                      <p className="font-bold">Erreur</p>
                      <p>{error}</p>
                    </div>
                  </div>
                </div>
              )}

              {/* Aucun résultat */}
              {!loading && !error && displayedHotels.length === 0 && (
                <div className="text-center py-20 bg-white rounded-xl shadow-lg">
                  <div className="text-6xl mb-4">😔</div>
                  <h2 className="text-2xl font-bold text-gray-700 mb-2">
                    Aucun hôtel trouvé
                  </h2>
                  <p className="text-gray-500 mb-6">
                    Aucun hôtel ne correspond à vos critères de recherche
                  </p>
                  <button
                    onClick={handleResetFilters}
                    className="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700 transition font-semibold shadow-lg"
                  >
                    🔄 Réinitialiser les filtres
                  </button>
                </div>
              )}

              {/* Résultats */}
              {!loading && !error && displayedHotels.length > 0 && (
                <>
                  {!showMap ? (
                    // Vue en grille (liste)
                    <div>
                      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
                        {displayedHotels.map((hotel) => (
                          <div
                            key={hotel.id}
                            id={`hotel-${hotel.id}`}
                            className={`transition-all duration-300 ${
                              selectedHotelId === hotel.id
                                ? "ring-4 ring-blue-500 rounded-xl scale-105"
                                : ""
                            }`}
                            onClick={() => setSelectedHotelId(hotel.id)}
                          >
                            <CardHotel hotel={hotel} />
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : (
                    // Vue carte interactive
                    <div className="bg-white rounded-xl shadow-lg p-4">
                      <div className="h-[700px] rounded-lg overflow-hidden">
                        <HotelMap
                          hotels={displayedHotels}
                          onHotelClick={handleHotelClick}
                          selectedHotelId={selectedHotelId}
                        />
                      </div>

                      {/* Légende de la carte */}
                      <div className="mt-4 flex items-center justify-center gap-6 text-sm text-gray-600">
                        <div className="flex items-center gap-2">
                          <div className="w-4 h-4 bg-red-500 rounded-full"></div>
                          <span>Hôtel disponible</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-4 h-4 bg-blue-600 rounded-full"></div>
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