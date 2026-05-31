/**
 * App.jsx
 * Composant racine de l'application.
 *
 * Evolution lot 3 (chambre detail) :
 *   - Route /hotel/:hotelId renommee en /hotel/:hotelSlug
 *     Le slug encode le nom + l'id : "hotel-des-arceaux-42"
 *   - DetailsPage remplace par HotelDetailsPage
 *   - Nouvelle route /hotel/:hotelSlug/chambre/:chambreId → ChambreDetailsPage
 *   - Nouvelle route /paiement → PagePayement (recoit l'etat via navigate)
 */

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "sonner";

import { AuthProvider } from "./context/AuthContext";

import MainLayout from "./layout/MainLayout";

import HomePage from "./Pages/HomePage";
import ConnexionUser from "./Pages/ConnexionUser";
import InscriptionUser from "./Pages/InscriptionUser";
import HotelDetailsPage from "./Pages/HotelDetailsPage";
import ChambreDetailsPage from "./Pages/ChambreDetailsPage";
import PagePayement from "./Pages/PagePayement";
import MesReservationsPage from "./Pages/MesReservationsPage";
import MonProfilPage from "./Pages/MonProfilPage";
import SuiviReservationPage from "./Pages/SuiviReservationPage";

// ADMIN
import AdminDashboard from "./Pages/admin/AdminDashboard";
import AdminUsers from "./Pages/admin/AdminUsers";
import AdminHotel from "./Pages/admin/AdminHotel";
import AdminChambres from "./Pages/admin/AdminChambres";
import ProtectedRoute from "./components/ProtectedRoute";

// EMPLOYE
import EmployeDashboard from "./Pages/employe/EmployeDashboard";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster position="top-right" richColors closeButton duration={4000} />

        <Routes>
          {/* Pages AVEC layout (navbar) */}
          <Route element={<MainLayout />}>
            <Route path="/" element={<HomePage />} />
          </Route>

          {/* Hotel + Chambre : sans layout, navbar geree en interne */}
          <Route path="/hotel/:hotelSlug" element={<HotelDetailsPage />} />
          <Route
            path="/hotel/:hotelSlug/chambre/:chambreId"
            element={<ChambreDetailsPage />}
          />

          {/* Paiement : recoit l'etat via navigate(state) */}
          <Route path="/paiement" element={<PagePayement />} />

          {/* Auth */}
          <Route path="/Connexion" element={<ConnexionUser />} />
          <Route path="/Inscription" element={<InscriptionUser />} />

          {/* Client protege */}
          <Route
            path="/mes-reservations"
            element={
              <ProtectedRoute>
                <MesReservationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/mes-reservations/:id"
            element={
              <ProtectedRoute>
                <SuiviReservationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/mon-profil"
            element={
              <ProtectedRoute>
                <MonProfilPage />
              </ProtectedRoute>
            }
          />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/add-users"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/hotels"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <AdminHotel />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/chambres"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <AdminChambres />
              </ProtectedRoute>
            }
          />

          {/* Employe */}
          <Route
            path="/employe"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <EmployeDashboard />
              </ProtectedRoute>
            }
          />

          {/* 404 */}
          <Route path="*" element={<h1>404</h1>} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
