import { BrowserRouter, Routes, Route } from "react-router-dom";

import MainLayout from "./layout/MainLayout";

import HomePage from "./Pages/HomePage";
import ConnexionUser from "./Pages/ConnexionUser";
import InscriptionUser from "./Pages/InscriptionUser";
import DetailsPage from "./Pages/DetailsPage";
import MesReservationsPage from './Pages/MesReservationsPage';

// ADMIN
import ChambreManagementPage from "./Pages/ChambreManagementPage";
import AdminDashboard from "./Pages/admin/AdminDashboard";
import AdminUsers from "./Pages/admin/AdminUsers";
import CrudHotel from "./Pages/admin/CrudHotel";
import ProtectedRoute from "./components/ProtectedRoute";

// EMPLOYE
import EmployeDashboard from "./Pages/employe/EmployeDashboard";

function App() {
    return (
        <BrowserRouter>
            <Routes>

                {/* Pages AVEC layout */}
                <Route element={<MainLayout />}>
                    <Route path="/" element={<HomePage />} />
                     {/* Page détails d'un hôtel avec ses chambres */}
                           <Route path="/hotel/:hotelId" element={<DetailsPage />} />

                            {/* Page de gestion des chambres (Admin) */}
                            <Route path="/chambres" element={<ChambreManagementPage />} />


                </Route>

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

                <Route path="/mes-reservations" element={
                  <ProtectedRoute>
                    <MesReservationsPage />
                  </ProtectedRoute>
                } />

                <Route
                    path="/admin/add-user"
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
        </BrowserRouter>
    );
}

export default App;
