/**
 * App.jsx
 * Composant racine de l'application.
 *
 * Modifications Lot 1 par rapport a la version precedente :
 *   - wrap toute l'app dans <AuthProvider>
 *   - ajout du <Toaster /> de sonner
 *   - le reste de la structure de routes est INCHANGE
 */

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "sonner";

import { AuthProvider } from "./context/AuthContext";

import MainLayout from "./layout/MainLayout";

import HomePage from "./Pages/HomePage";
import ConnexionUser from "./Pages/ConnexionUser";
import InscriptionUser from "./Pages/InscriptionUser";
import DetailsPage from "./Pages/DetailsPage";
import MesReservationsPage from "./Pages/MesReservationsPage";

// ADMIN
import ChambreManagementPage from "./Pages/ChambreManagementPage";
import AdminDashboard from "./Pages/admin/AdminDashboard";
import AdminUsers from "./Pages/admin/AdminUsers";
import CrudHotel from "./Pages/admin/CrudHotel";
import ProtectedRoute from "./components/ProtectedRoute";

// EMPLOYE
import EmployeDashboard from "./Pages/employe/EmployeDashboard";
import MonProfilPage from "./Pages/MonProfilPage";
import SuiviReservationPage from "./Pages/SuiviReservationPage";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster position="top-center" richColors closeButton duration={4000} />

        <Routes>
          {/* Pages AVEC layout (navbar) */}
          <Route element={<MainLayout />}>
            <Route path="/" element={<HomePage />} />
            {/* Page de gestion des chambres (Admin) */}
            <Route path="/chambres" element={<ChambreManagementPage />} />
          </Route>

          {/* Pages SANS layout, navbar retiree */}
          <Route path="/hotel/:hotelId" element={<DetailsPage />} />

          {/* Auth */}
          <Route path="/Connexion" element={<ConnexionUser />} />
          <Route path="/Inscription" element={<InscriptionUser />} />

          {/* ADMIN */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/mes-reservations"
            element={
              <ProtectedRoute>
                <MesReservationsPage />
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

          <Route
            path="/mes-reservations/:id"
            element={
              <ProtectedRoute>
                <SuiviReservationPage />
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
            path="/admin/add-hotel"
            element={
              <ProtectedRoute roleRequired="ROLE_ADMIN">
                <CrudHotel />
              </ProtectedRoute>
            }
          />

          {/* EMPLOYE */}
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
