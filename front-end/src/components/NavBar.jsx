import { Link } from "react-router-dom";
import React from "react";

export default function Navbar() {
  const role = localStorage.getItem("role");
  const isLogged = !!role;
  const isAdmin = role === "ROLE_ADMIN";
  const isEmploye = role === "ROLE_EMPLOYE";

  const logout = () => {
    localStorage.clear();
    window.location.href = "/";
  };

  return (
    <nav className="w-full bg-white shadow-sm p-4 flex items-center justify-between">
      <div className="text-2xl font-bold text-blue-600">
        <Link to="/">HotelBooking</Link>
      </div>

      <ul className="flex items-center gap-6 text-gray-700 font-medium">
        <li className="cursor-pointer hover:text-blue-600">
          <Link to="/">Accueil</Link>
        </li>
        <li className="cursor-pointer hover:text-blue-600">Destinations</li>
        {isLogged && (
          <li className="cursor-pointer hover:text-blue-600">
            Mes réservations
          </li>
        )}
      </ul>

      <div className="flex items-center gap-4">
        {!isLogged && (
          <>
            <Link
              to="/Connexion"
              className="px-4 py-2 rounded-lg text-gray-700 hover:bg-gray-100"
            >
              Connexion
            </Link>

            <Link
              to="/Inscription"
              className="px-4 py-2 rounded-lg bg-black text-white hover:bg-gray-800"
            >
              Inscription
            </Link>
          </>
        )}

        {isAdmin && (
          <Link
            to="/admin"
            className="px-4 py-2 rounded-lg bg-red-600 text-white hover:bg-red-700"
          >
            Admin
          </Link>
        )}

        {isEmploye && (
          <Link
            to="/employe"
            className="px-4 py-2 rounded-lg bg-green-600 text-white hover:bg-green-700"
          >
            Employé
          </Link>
        )}

        {isLogged && (
          <>
            <span className="px-4 py-2 rounded-lg bg-yellow-400 text-black font-medium">
              {role.replace("ROLE_", "")}
            </span>
            <button
              onClick={logout}
              className="px-4 py-2 rounded-lg bg-gray-300 hover:bg-gray-400"
            >
              Déconnexion
            </button>
          </>
        )}
      </div>
    </nav>
  );
}
