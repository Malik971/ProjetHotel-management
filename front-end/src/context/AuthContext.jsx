/**
 * AuthContext.jsx
 * Context React qui centralise l'etat d'authentification de l'application.
 *
 * Evolution Lot K4 : ajout du flux Keycloak en coexistence avec le JWT maison.
 *
 * Deux flux d'authentification coexistent :
 *   Un, JWT maison : login(email, password) appelle POST /api/v1/login,
 *       recoit un token HS256, le stocke sous sejour_token. Inchange.
 *   Deux, Keycloak PKCE : loginWithKeycloak() redirige vers Keycloak,
 *       handleKeycloakCallback() echange le code contre un access_token RS256,
 *       le stocke sous la meme cle sejour_token. httpClient l'injecte
 *       automatiquement sans distinction d'origine.
 *
 * Ce qui ne change pas :
 *   - STORAGE_KEYS, httpClient, l'interceptor axios : rien ne bouge
 *   - logout() couvre les deux flux (supprime sejour_token dans les deux cas)
 *   - isAdmin, isEmploye, isAuthenticated : meme logique, meme source (/api/me)
 *   - ProtectedRoute : aucune modification
 *
 * Nouvelles entrees dans le contexte :
 *   - loginWithKeycloak(scope) : declenche la redirection PKCE
 *   - isKeycloakUser : true si l'utilisateur courant est connecte via Keycloak
 *     (utile pour choisir entre logout() et logoutFromKeycloak() dans NavBar)
 *
 * Usage :
 *   const { user, isAuthenticated, isAdmin, login, loginWithKeycloak, logout } = useAuth();
 */

import { useState, useEffect, useCallback } from 'react';
import { toast } from 'sonner';
import { httpClient, STORAGE_KEYS, cleanLegacyStorage } from '../api/httpClient';
import {
    loginWithKeycloak as keycloakLogin,
    handleKeycloakCallback,
    logoutFromKeycloak,
} from '../api/keycloak';
import { AuthContext } from './authContextInstance';

/**
 * Provider a placer en haut de l'arbre React, dans App.jsx.
 * Tous ses enfants pourront acceder a l'authentification via useAuth().
 */
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    /**
     * Au demarrage de l'app, trois cas possibles :
     *
     * Cas 1 : retour depuis Keycloak (URL contient ?code=...).
     *   On echange le code contre un token, on stocke le token,
     *   puis on appelle /api/me pour recuperer le profil.
     *   Le JIT provisioning (Lot K3) cree le profil en base si necessaire.
     *
     * Cas 2 : token present en localStorage (session existante JWT maison ou Keycloak).
     *   On appelle /api/me pour verifier que le token est encore valide.
     *
     * Cas 3 : pas de token, pas de code.
     *   L'utilisateur n'est pas connecte, loading passe a false directement.
     */
    useEffect(() => {
        cleanLegacyStorage();

        const init = async () => {
            // Cas 1 : retour Keycloak
            const isCallback = window.location.search.includes('code=');
            if (isCallback) {
                try {
                    const success = await handleKeycloakCallback();
                    if (success) {
                        const me = await httpClient.get('/api/me');
                        setUser(me.data);
                        toast.success(`Bienvenue ${me.data.firstName || me.data.email}`);
                    }
                } catch (err) {
                    console.error('Erreur traitement callback Keycloak :', err);
                    toast.error('Erreur de connexion via Keycloak');
                } finally {
                    setLoading(false);
                }
                return;
            }

            // Cas 2 : token existant
            const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
            if (token) {
                try {
                    const me = await httpClient.get('/api/me');
                    setUser(me.data);
                } catch {
                    setUser(null);
                } finally {
                    setLoading(false);
                }
                return;
            }

            // Cas 3 : pas de session
            setLoading(false);
        };

        init();
    }, []);

    /**
     * Flux JWT maison : appelle POST /api/v1/login, stocke le token,
     * recupere le profil complet via /api/me.
     * Inchange depuis le Lot 1.
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
     * Flux Keycloak PKCE : redirige le navigateur vers Keycloak.
     * L'utilisateur ne revient sur le frontend qu'apres s'etre connecte.
     * Le retour est gere dans le useEffect ci-dessus (cas 1).
     *
     * @param {string} scope - Scopes demandes. 'openid profile email' par defaut.
     *   Passer 'openid profile email pastell-admin' pour les admins Pastell.
     */
    const loginWithKeycloak = useCallback(async (scope = 'openid pastell-admin', idpHint = null) => {
        try {
            await keycloakLogin(scope, idpHint);
            // Pas de return : la fonction redirige le navigateur,
            // le code apres cette ligne ne s'execute jamais.
        } catch (err) {
            console.error('Erreur lancement login Keycloak :', err);
            toast.error('Impossible de contacter Keycloak');
        }
    }, []);

    /**
     * Deconnecte l'utilisateur.
     * Detecte si la session est Keycloak (refresh_token present) ou JWT maison,
     * et applique la deconnexion appropriee.
     *
     * Deconnexion JWT maison : supprime le localStorage, c'est tout.
     * Deconnexion Keycloak : invalide la session SSO cote Keycloak en plus.
     */
    const logout = useCallback(() => {
        const isKeycloakSession = !!localStorage.getItem('keycloak_refresh_token');

        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        localStorage.removeItem(STORAGE_KEYS.EMAIL);
        localStorage.removeItem(STORAGE_KEYS.ROLES);
        setUser(null);

        if (isKeycloakSession) {
            // La redirection vers Keycloak invalide la session SSO
            logoutFromKeycloak();
        } else {
            toast.success('A bientot !');
        }
    }, []);

    const value = {
        user,
        loading,
        isAuthenticated: user !== null,
        isAdmin: user?.roles?.includes('ROLE_ADMIN') ?? false,
        isEmploye: user?.roles?.includes('ROLE_EMPLOYE') ?? false,
        isKeycloakUser: !!localStorage.getItem('keycloak_refresh_token'),
        login,
        loginWithKeycloak,
        logout,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}