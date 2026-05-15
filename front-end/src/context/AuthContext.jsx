/**
 * AuthContext.jsx
 * Context React qui centralise l'etat d'authentification de l'application.
 *
 * Pourquoi ce fichier existe :
 *   - Une seule source de verite pour savoir qui est connecte
 *   - Plus de localStorage.getItem('role') dans 15 fichiers differents
 *   - Notification automatique de tous les composants quand l'user change
 *
 * Toutes les pages utilisent ce contexte via le hook useAuth.
 *
 * Usage :
 *   import { useAuth } from '../hooks/useAuth';
 *   const { user, isAuthenticated, isAdmin, login, logout } = useAuth();
 */

import { createContext, useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { httpClient, STORAGE_KEYS, cleanLegacyStorage } from '../api/httpClient';

export const AuthContext = createContext(null);

/**
 * Provider a placer en haut de l'arbre React, dans App.jsx.
 * Tous ses enfants pourront acceder a l'authentification via useAuth().
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    

    /**
     * Au demarrage de l'app :
     *   un, nettoyage des cles legacy pre-JWT,
     *   deux, si un token existe en localStorage, on appelle /api/me pour
     *         recuperer les infos a jour de l'utilisateur,
     *   trois, si l'appel echoue (token expire ou backend down), on clean.
     */
    useEffect(() => {
        cleanLegacyStorage();

        const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
        if (!token) {
            setLoading(false);
            return;
        }

        // Token present : on verifie qu'il est encore valide en interrogeant /api/me
        httpClient
            .get('/api/me')
            .then((res) => {
                setUser(res.data);
            })
            .catch(() => {
                // Token rejete par le backend, le interceptor a deja clean
                setUser(null);
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    /**
     * Connecte l'utilisateur : appelle /api/v1/login, stocke le token,
     * puis appelle /api/me pour recuperer le profil complet.
     *
     * @returns {Promise<boolean>} true si la connexion a reussi
     */
    const login = useCallback(async (email, password) => {
        try {
            const { data } = await httpClient.post('/api/v1/login', {
                email,
                password,
            });

            localStorage.setItem(STORAGE_KEYS.TOKEN, data.token);
            localStorage.setItem(STORAGE_KEYS.EMAIL, data.email);
            localStorage.setItem(STORAGE_KEYS.ROLES, JSON.stringify(data.roles));

            // Recuperation du profil complet pour avoir firstName, lastName, etc.
            const me = await httpClient.get('/api/me');
            setUser(me.data);

            toast.success(`Bienvenue ${me.data.firstName || me.data.email}`);
            return true;
        } catch (err) {
            const message =
                err.response?.status === 401
                    ? 'Identifiants invalides'
                    : 'Erreur de connexion, veuillez reessayer';
            toast.error(message);
            return false;
        }
    }, []);

    /**
     * Deconnecte l'utilisateur : clean le localStorage et le state.
     * Pas d'appel backend, le JWT etant stateless on a juste a oublier le token.
     */
    const logout = useCallback(() => {
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.EMAIL);
        localStorage.removeItem(STORAGE_KEYS.ROLES);
        setUser(null);
        toast.success('A bientot !');
    }, []);

    const value = {
        user,
        loading,
        isAuthenticated: user !== null,
        isAdmin: user?.roles?.includes('ROLE_ADMIN') ?? false,
        isEmploye: user?.roles?.includes('ROLE_EMPLOYE') ?? false,
        login,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}