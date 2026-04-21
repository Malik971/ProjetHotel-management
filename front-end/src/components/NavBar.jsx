// src/components/NavBar.jsx
import { Link, useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { Menu, X, LogOut, User } from "lucide-react";

export default function Navbar() {
    const [mobileOpen, setMobileOpen] = useState(false);
    const navigate = useNavigate();

    const role = typeof window !== "undefined" ? localStorage.getItem("role") : null;
    const isLogged = !!role;
    const isAdmin = role === "ROLE_ADMIN";
    const isEmploye = role === "ROLE_EMPLOYE";

    // Fermer le menu mobile lors d'un changement de route
    useEffect(() => {
        setMobileOpen(false);
    }, []);

    const logout = () => {
        localStorage.clear();
        window.location.href = "/";
    };

    const Logo = () => (
        <Link to="/" className="flex items-center gap-2 group">
            {/* Icône logo — vague stylisée */}
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] flex items-center justify-center shadow-sm group-hover:shadow-md transition-shadow">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 16C3 16 5 14 7 14C9 14 10 16 12 16C14 16 15 14 17 14C19 14 21 16 21 16" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    <path d="M3 20C3 20 5 18 7 18C9 18 10 20 12 20C14 20 15 18 17 18C19 18 21 20 21 20" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    <circle cx="12" cy="7" r="3" stroke="white" strokeWidth="2" fill="#F59E0B"/>
                </svg>
            </div>
            <span className="text-xl font-bold text-[#0369A1] tracking-tight"
                  style={{ fontFamily: "'Georgia', 'Times New Roman', serif" }}>
        Séjour
      </span>
        </Link>
    );

    return (
        <nav className="w-full bg-white border-b border-gray-100 sticky top-0 z-40">
            <div className="max-w-[1400px] mx-auto px-4 md:px-8 h-16 flex items-center justify-between">

                {/* Logo */}
                <Logo />

                {/* Nav centrale — desktop uniquement */}
                <ul className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-600">
                    <li>
                        <Link to="/" className="hover:text-[#0EA5E9] transition-colors">
                            Accueil
                        </Link>
                    </li>
                    {isLogged && !isAdmin && !isEmploye && (
                        <li>
                            <Link to="/mes-reservations" className="hover:text-[#0EA5E9] transition-colors">
                                Mes réservations
                            </Link>
                        </li>
                    )}
                </ul>

                {/* Actions droite — desktop */}
                <div className="hidden md:flex items-center gap-3">
                    {!isLogged && (
                        <>
                            <Link
                                to="/Connexion"
                                className="px-4 py-2 rounded-xl text-sm font-medium text-gray-700 hover:text-[#0EA5E9] hover:bg-gray-50 transition-all"
                            >
                                Connexion
                            </Link>
                            <Link
                                to="/Inscription"
                                className="px-4 py-2 rounded-xl bg-[#0EA5E9] hover:bg-[#0284C7] text-white text-sm font-semibold transition-colors shadow-sm"
                            >
                                Inscription
                            </Link>
                        </>
                    )}

                    {isAdmin && (
                        <Link
                            to="/admin"
                            className="px-4 py-2 rounded-xl bg-[#0369A1] hover:bg-[#075985] text-white text-sm font-semibold transition-colors"
                        >
                            Espace admin
                        </Link>
                    )}

                    {isEmploye && (
                        <Link
                            to="/employe"
                            className="px-4 py-2 rounded-xl bg-[#0369A1] hover:bg-[#075985] text-white text-sm font-semibold transition-colors"
                        >
                            Espace employé
                        </Link>
                    )}

                    {isLogged && (
                        <button
                            onClick={logout}
                            className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-gray-200 text-gray-600 text-sm font-medium hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-all"
                        >
                            <LogOut size={14} />
                            Déconnexion
                        </button>
                    )}
                </div>

                {/* Burger mobile */}
                <button
                    onClick={() => setMobileOpen(!mobileOpen)}
                    className="md:hidden text-gray-700 hover:text-[#0EA5E9] transition-colors"
                    aria-label="Menu"
                >
                    {mobileOpen ? <X size={24} /> : <Menu size={24} />}
                </button>
            </div>

            {/* Menu mobile */}
            {mobileOpen && (
                <div className="md:hidden border-t border-gray-100 bg-white">
                    <div className="px-4 py-4 space-y-2">
                        <Link
                            to="/"
                            onClick={() => setMobileOpen(false)}
                            className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-[#0EA5E9]"
                        >
                            Accueil
                        </Link>

                        {isLogged && !isAdmin && !isEmploye && (
                            <Link
                                to="/mes-reservations"
                                onClick={() => setMobileOpen(false)}
                                className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-[#0EA5E9]"
                            >
                                Mes réservations
                            </Link>
                        )}

                        <div className="pt-2 mt-2 border-t border-gray-100 space-y-2">
                            {!isLogged && (
                                <>
                                    <Link
                                        to="/Connexion"
                                        onClick={() => setMobileOpen(false)}
                                        className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                                    >
                                        Connexion
                                    </Link>
                                    <Link
                                        to="/Inscription"
                                        onClick={() => setMobileOpen(false)}
                                        className="block px-3 py-2.5 rounded-lg bg-[#0EA5E9] text-white text-sm font-semibold text-center"
                                    >
                                        Inscription
                                    </Link>
                                </>
                            )}

                            {isAdmin && (
                                <Link
                                    to="/admin"
                                    onClick={() => setMobileOpen(false)}
                                    className="block px-3 py-2.5 rounded-lg bg-[#0369A1] text-white text-sm font-semibold text-center"
                                >
                                    Espace admin
                                </Link>
                            )}

                            {isEmploye && (
                                <Link
                                    to="/employe"
                                    onClick={() => setMobileOpen(false)}
                                    className="block px-3 py-2.5 rounded-lg bg-[#0369A1] text-white text-sm font-semibold text-center"
                                >
                                    Espace employé
                                </Link>
                            )}

                            {isLogged && (
                                <button
                                    onClick={logout}
                                    className="w-full flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg border border-gray-200 text-gray-600 text-sm font-medium"
                                >
                                    <LogOut size={14} />
                                    Déconnexion
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </nav>
    );
}