/**
 * httpClient.js
 * Client HTTP centralise pour toutes les communications avec le backend Sejour.
 *
 * Pourquoi ce fichier existe :
 *   - Une seule source de verite pour l'URL du backend (lue depuis VITE_API_URL)
 *   - Injection automatique du JWT dans toutes les requetes via interceptor
 *   - Gestion centralisee des 401 (token expire) avec redirection login
 *   - Plus de fetch eparpilles dans 10 fichiers services differents
 *
 * Toutes les pages et services doivent passer par ce client.
 *
 * Usage :
 *   import { httpClient } from '../api/httpClient';
 *   const { data } = await httpClient.get('/api/hotels');
 *   const { data } = await httpClient.post('/api/reservations', payload);
 */

import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Cles localStorage utilisees pour stocker l'authentification.
 * Centralisees ici pour eviter les fautes de frappe et pouvoir les changer
 * en un seul endroit.
 */
export const STORAGE_KEYS = {
    TOKEN: 'sejour_token',
    EMAIL: 'sejour_email',
    ROLES: 'sejour_roles',
};

/**
 * Cles legacy a nettoyer au demarrage (heritage pre-JWT).
 * On les supprime pour que les anciennes sessions soient bien invalidees.
 */
const LEGACY_KEYS = ['id', 'email', 'role', 'roles', 'token', 'user'];

/**
 * Nettoyage des cles legacy. Appele une fois au demarrage de l'app.
 * Si on trouve une ancienne session (id/email/role sans token), on clean tout.
 */
export function cleanLegacyStorage() {
    const hasLegacy = LEGACY_KEYS.some((k) => localStorage.getItem(k) !== null);
    const hasNewToken = localStorage.getItem(STORAGE_KEYS.TOKEN) !== null;

    if (hasLegacy && !hasNewToken) {
        LEGACY_KEYS.forEach((k) => localStorage.removeItem(k));
    }
}

/**
 * Instance axios configuree. Tous les imports doivent l'utiliser, pas axios
 * directement.
 */
export const httpClient = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 30000, // 30s, le temps que Render se reveille en cas de cold start
});

/**
 * Interceptor de requete : ajoute automatiquement le JWT dans le header
 * Authorization si on en a un en localStorage.
 */
httpClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem(STORAGE_KEYS.TOKEN);
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

/**
 * Interceptor de reponse : si le serveur renvoie 401, on considere que la
 * session est invalide. On nettoie le localStorage et on redirige vers la
 * page de connexion.
 *
 * Note : pour eviter une boucle infinie, on ne redirige PAS si l'erreur 401
 * vient de l'endpoint /api/v1/login lui-meme (le user a entre de mauvais
 * identifiants, ce qui n'a rien a voir avec une expiration de session).
 */
httpClient.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const requestUrl = error.config?.url || '';
        const isLoginAttempt = requestUrl.includes('/api/v1/login');

        if (status === 401 && !isLoginAttempt) {
            localStorage.removeItem(STORAGE_KEYS.TOKEN);
            localStorage.removeItem(STORAGE_KEYS.EMAIL);
            localStorage.removeItem(STORAGE_KEYS.ROLES);

            // Redirection forcee si on n'est pas deja sur la page login,
            // pour eviter de boucler.
            if (!window.location.pathname.includes('/Connexion')) {
                window.location.href = '/Connexion';
            }
        }

        return Promise.reject(error);
    }
);