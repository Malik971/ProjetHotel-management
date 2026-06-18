/**
 * App.jsx
 * Composant racine de l'application.
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
import AdminDocs from "./Pages/admin/AdminDocs";
import AdminPastellList from "./Pages/admin/AdminPastellList";
import AdminPastellDetail from "./Pages/admin/AdminPastellDetail";
import AdminReservationsEnAttente from "./Pages/admin/AdminReservationsEnAttente";
import AdminSignaturePage from "./Pages/admin/AdminSignaturePage";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster position="top-center" richColors closeButton duration={3000} />

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

          {/* Paiement */}
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
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/docs"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminDocs />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/pastell"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminPastellList />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/pastell/:reservationId"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminPastellDetail />
              </ProtectedRoute>
            }
          />

          {/* Workflow de validation avec signature */}
          <Route
            path="/admin/reservations/en-attente"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminReservationsEnAttente />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/reservations/:id/signer"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminSignaturePage />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/add-users"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/hotels"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminHotel />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/chambres"
            element={
              <ProtectedRoute roleRequired="ROLE_EMPLOYE">
                <AdminChambres />
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