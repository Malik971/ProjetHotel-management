// src/components/NavBar.jsx
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { Menu, X, LogOut, Sun, Moon } from "lucide-react";
import { useAuth } from "../hooks/useAuth";

export default function Navbar() {
    const [mobileOpen, setMobileOpen] = useState(false);
    const navigate = useNavigate();

    const { user, isAuthenticated, isAdmin, isEmploye, logout } = useAuth();
    const isLogged = isAuthenticated;

    // Theme sombre : pilote par la classe .dark sur <html>, memorise en localStorage.
    const [dark, setDark] = useState(
        () => typeof document !== "undefined" && document.documentElement.classList.contains("dark")
    );
    const toggleTheme = () => {
        setDark((prev) => {
            const next = !prev;
            document.documentElement.classList.toggle("dark", next);
            localStorage.setItem("theme", next ? "dark" : "light");
            return next;
        });
    };

    const handleLogout = () => {
        logout();
        navigate("/");
    };

    // Premiere lettre de l'utilisateur pour le cercle profil.
    // Priorite : firstName, sinon email, sinon point d'interrogation.
    const initiale = (
        user?.firstName?.charAt(0)
        || user?.email?.charAt(0)
        || "?"
    ).toUpperCase();

    const ThemeToggle = ({ className = "" }) => (
        <button
            onClick={toggleTheme}
            aria-label={dark ? "Passer en mode clair" : "Passer en mode sombre"}
            title={dark ? "Mode clair" : "Mode sombre"}
            className={`p-2 rounded-xl border border-gray-200 text-gray-600 hover:text-[#0EA5E9] hover:border-[#0EA5E9] transition-all dark:border-gray-700 dark:text-gray-300 dark:hover:text-[#38BDF8] dark:hover:border-[#38BDF8] ${className}`}
        >
            {dark ? <Sun size={16} /> : <Moon size={16} />}
        </button>
    );

    const Logo = () => (
        <Link to="/" className="flex items-center gap-2 group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] flex items-center justify-center shadow-sm group-hover:shadow-md transition-shadow">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M3 16C3 16 5 14 7 14C9 14 10 16 12 16C14 16 15 14 17 14C19 14 21 16 21 16" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    <path d="M3 20C3 20 5 18 7 18C9 18 10 20 12 20C14 20 15 18 17 18C19 18 21 20 21 20" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    <circle cx="12" cy="7" r="3" stroke="white" strokeWidth="2" fill="#F59E0B"/>
                </svg>
            </div>
            <span className="text-xl font-bold text-[#0369A1] dark:text-[#38BDF8] tracking-tight"
                  style={{ fontFamily: "'Georgia', 'Times New Roman', serif" }}>
                Séjour
            </span>
        </Link>
    );

    return (
        <nav className="w-full bg-white border-b border-gray-100 sticky top-0 z-40 dark:bg-gray-900 dark:border-gray-800">
            <div className="max-w-[1400px] mx-auto px-4 md:px-8 h-16 flex items-center justify-between">

                {/* Logo */}
                <Logo />

                {/* Nav centrale — desktop uniquement */}
                <ul className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-600 dark:text-gray-300">
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
                    <ThemeToggle />

                    {!isLogged && (
                        <>
                            <Link
                                to="/Connexion"
                                className="px-4 py-2 rounded-xl text-sm font-medium text-gray-700 hover:text-[#0EA5E9] hover:bg-gray-50 transition-all dark:text-gray-200 dark:hover:bg-gray-800"
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
                            to="/admin"
                            className="px-4 py-2 rounded-xl bg-[#0369A1] hover:bg-[#075985] text-white text-sm font-semibold transition-colors"
                        >
                            Espace employé
                        </Link>
                    )}

                    {/* Bouton "Mon profil" : cercle avec initiale + libelle.
                        Visible des qu'un utilisateur est connecte (admin, employe ou client). */}
                    {isLogged && (
                        <Link
                            to="/mon-profil"
                            className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-gray-200 text-gray-700 text-sm font-medium hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-all dark:border-gray-700 dark:text-gray-200"
                            title="Mon profil"
                        >
                            <span className="w-7 h-7 rounded-full bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] text-white flex items-center justify-center text-xs font-bold">
                                {initiale}
                            </span>
                            <span>Mon profil</span>
                        </Link>
                    )}

                    {isLogged && (
                        <button
                            onClick={handleLogout}
                            className="flex items-center gap-1.5 px-4 py-2 rounded-xl border border-gray-200 text-gray-600 text-sm font-medium hover:border-[#0EA5E9] hover:text-[#0EA5E9] transition-all dark:border-gray-700 dark:text-gray-300"
                        >
                            <LogOut size={14} />
                            Déconnexion
                        </button>
                    )}
                </div>

                {/* Actions mobile : interrupteur + burger */}
                <div className="md:hidden flex items-center gap-2">
                    <ThemeToggle />
                    <button
                        onClick={() => setMobileOpen(!mobileOpen)}
                        className="text-gray-700 hover:text-[#0EA5E9] transition-colors dark:text-gray-200"
                        aria-label="Menu"
                    >
                        {mobileOpen ? <X size={24} /> : <Menu size={24} />}
                    </button>
                </div>
            </div>

            {/* Menu mobile */}
            {mobileOpen && (
                <div className="md:hidden border-t border-gray-100 bg-white dark:bg-gray-900 dark:border-gray-800">
                    <div className="px-4 py-4 space-y-2">
                        <Link
                            to="/"
                            onClick={() => setMobileOpen(false)}
                            className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-[#0EA5E9] dark:text-gray-200 dark:hover:bg-gray-800"
                        >
                            Accueil
                        </Link>

                        {isLogged && !isAdmin && !isEmploye && (
                            <Link
                                to="/mes-reservations"
                                onClick={() => setMobileOpen(false)}
                                className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-[#0EA5E9] dark:text-gray-200 dark:hover:bg-gray-800"
                            >
                                Mes réservations
                            </Link>
                        )}

                        {/* Mon profil mobile, avec initiale en cercle */}
                        {isLogged && (
                            <Link
                                to="/mon-profil"
                                onClick={() => setMobileOpen(false)}
                                className="flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-[#0EA5E9] dark:text-gray-200 dark:hover:bg-gray-800"
                            >
                                <span className="w-6 h-6 rounded-full bg-gradient-to-br from-[#0EA5E9] to-[#0369A1] text-white flex items-center justify-center text-xs font-bold">
                                    {initiale}
                                </span>
                                Mon profil
                            </Link>
                        )}

                        <div className="pt-2 mt-2 border-t border-gray-100 space-y-2 dark:border-gray-800">
                            {!isLogged && (
                                <>
                                    <Link
                                        to="/Connexion"
                                        onClick={() => setMobileOpen(false)}
                                        className="block px-3 py-2.5 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 dark:text-gray-200 dark:hover:bg-gray-800"
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
                                    to="/admin"
                                    onClick={() => setMobileOpen(false)}
                                    className="block px-3 py-2.5 rounded-lg bg-[#0369A1] text-white text-sm font-semibold text-center"
                                >
                                    Espace employé
                                </Link>
                            )}

                            {isLogged && (
                                <button
                                    onClick={handleLogout}
                                    className="w-full flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg border border-gray-200 text-gray-600 text-sm font-medium dark:border-gray-700 dark:text-gray-300"
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
