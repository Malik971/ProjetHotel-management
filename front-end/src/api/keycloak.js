/**
 * keycloak.js
 * Module qui encapsule keycloak-js pour le flow Authorization Code + PKCE.
 *
 * Pourquoi ce fichier existe :
 *   AuthContext gere l'etat React (user, loading, isAdmin...).
 *   Ce module gere le protocole OAuth2 pur : redirection, echange de code,
 *   refresh de token. On separe les deux responsabilites pour ne pas
 *   transformer AuthContext en fichier de 300 lignes.
 *
 * Flow Authorization Code PKCE (ce qui se passe quand on clique "Se connecter avec Keycloak") :
 *   1. loginWithKeycloak() redirige le navigateur vers Keycloak
 *      (http://localhost:8180/realms/springhotel/protocol/openid-connect/auth
 *       ?client_id=springhotel-frontend
 *       &redirect_uri=http://localhost:5173/Connexion
 *       &response_type=code
 *       &scope=openid profile email
 *       &code_challenge=<hash PKCE>
 *       &code_challenge_method=S256)
 *   2. L'utilisateur saisit ses identifiants sur la page Keycloak
 *   3. Keycloak redirige vers http://localhost:5173/Connexion?code=...&state=...
 *   4. handleKeycloakCallback() detecte ce code dans l'URL,
 *      echange le code contre un access_token via POST /token,
 *      stocke le token sous la cle sejour_token (meme cle que le JWT maison),
 *      supprime le code de l'URL pour ne pas le rejouer.
 *   5. AuthContext appelle /api/me avec ce token pour recuperer le profil.
 *
 * Pourquoi PKCE (Proof Key for Code Exchange) :
 *   Le client est public (pas de secret cote navigateur). Sans PKCE, un
 *   attaquant qui intercepte le code OAuth2 dans l'URL peut l'echanger
 *   contre un token. PKCE ajoute un verifier secret connu seulement du
 *   navigateur initiateur, rendant le code inutilisable sans ce verifier.
 *
 * Usage :
 *   import { loginWithKeycloak, handleKeycloakCallback } from '../api/keycloak';
 */

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180';
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'springhotel';
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'springhotel-frontend';

// L'URL vers laquelle Keycloak redirigera apres connexion.
// Doit correspondre exactement a une valeur dans redirectUris du realm-export.json.
const REDIRECT_URI = `${window.location.origin}/Connexion`;

// Cle localStorage pour stocker le code_verifier PKCE entre la redirection
// et le retour (deux navigations differentes, impossible de garder en memoire).
const PKCE_VERIFIER_KEY = 'keycloak_pkce_verifier';
const PKCE_STATE_KEY = 'keycloak_pkce_state';

// Cle partagee avec httpClient.js : le token est lu depuis cette cle
// par l'interceptor axios. Les deux flux (maison et Keycloak) utilisent
// la meme cle pour que httpClient n'ait pas a distinguer l'origine du token.
const TOKEN_KEY = 'sejour_token';

// ============================================================
// Utilitaires PKCE
// ============================================================

/**
 * Genere un code_verifier aleatoire de 64 octets encode en base64url.
 * Le code_verifier est secret, stocke en localStorage, jamais envoye a Keycloak.
 */
function generateCodeVerifier() {
    const array = new Uint8Array(64);
    window.crypto.getRandomValues(array);
    return base64urlEncode(array);
}

/**
 * Calcule le code_challenge = BASE64URL(SHA256(code_verifier)).
 * Le code_challenge est envoye a Keycloak lors de la demande d'autorisation.
 * Keycloak le stocke, puis verifie que SHA256(code_verifier recu) == code_challenge.
 */
async function generateCodeChallenge(verifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const digest = await window.crypto.subtle.digest('SHA-256', data);
    return base64urlEncode(new Uint8Array(digest));
}

/**
 * Encode un tableau d'octets en base64url (sans padding =, + remplace par -, / par _).
 */
function base64urlEncode(array) {
    return btoa(String.fromCharCode(...array))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}

/**
 * Genere un state aleatoire pour proteger contre les attaques CSRF.
 * Le state est envoye a Keycloak et renvoyé tel quel dans la redirection retour.
 * On verifie qu'il correspond au state qu'on avait genere.
 */
function generateState() {
    const array = new Uint8Array(16);
    window.crypto.getRandomValues(array);
    return base64urlEncode(array);
}

// ============================================================
// API publique du module
// ============================================================

/**
 * Declenche le flow Authorization Code PKCE.
 * Redirige le navigateur vers la page de login Keycloak.
 * Apres connexion reussie, Keycloak redirige vers REDIRECT_URI?code=...
 *
 * @param {string} scope - Scopes OAuth2 demandes (ex: 'openid' ou 'openid pastell-admin')
 */
export async function loginWithKeycloak(scope = 'openid pastell-admin') {
    const verifier = generateCodeVerifier();
    const challenge = await generateCodeChallenge(verifier);
    const state = generateState();

    // Stockage du verifier et du state pour la verification au retour
    localStorage.setItem(PKCE_VERIFIER_KEY, verifier);
    localStorage.setItem(PKCE_STATE_KEY, state);

    const authUrl = new URL(
        `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/auth`
    );
    authUrl.searchParams.set('client_id', KEYCLOAK_CLIENT_ID);
    authUrl.searchParams.set('redirect_uri', REDIRECT_URI);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('scope', scope);
    authUrl.searchParams.set('code_challenge', challenge);
    authUrl.searchParams.set('code_challenge_method', 'S256');
    authUrl.searchParams.set('state', state);

    window.location.href = authUrl.toString();
}

/**
 * Traite le retour de Keycloak apres connexion.
 * Detecte les parametres code et state dans l'URL courante,
 * echange le code contre un access_token, stocke le token.
 *
 * Doit etre appele dans un useEffect sur la page de redirection (/Connexion).
 *
 * @returns {Promise<boolean>} true si un token a ete obtenu, false sinon
 */
export async function handleKeycloakCallback() {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');

    // Pas de code dans l'URL : ce n'est pas un retour Keycloak
    if (!code) {
        return false;
    }

    // Verification du state anti-CSRF
    const storedState = localStorage.getItem(PKCE_STATE_KEY);
    if (state !== storedState) {
        console.error('State PKCE invalide, possible attaque CSRF.');
        localStorage.removeItem(PKCE_VERIFIER_KEY);
        localStorage.removeItem(PKCE_STATE_KEY);
        return false;
    }

    const verifier = localStorage.getItem(PKCE_VERIFIER_KEY);
    if (!verifier) {
        console.error('Code verifier PKCE absent du localStorage.');
        return false;
    }

    // Nettoyage des donnees PKCE : ne plus en avoir besoin apres l'echange
    localStorage.removeItem(PKCE_VERIFIER_KEY);
    localStorage.removeItem(PKCE_STATE_KEY);

    // Nettoyage de l'URL : on supprime code et state pour eviter un rejeu
    // si l'utilisateur recharge la page ou partage l'URL
    const cleanUrl = window.location.pathname;
    window.history.replaceState({}, document.title, cleanUrl);

    // Echange du code contre un access_token
    const tokenUrl = `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`;

    const body = new URLSearchParams();
    body.set('grant_type', 'authorization_code');
    body.set('client_id', KEYCLOAK_CLIENT_ID);
    body.set('redirect_uri', REDIRECT_URI);
    body.set('code', code);
    body.set('code_verifier', verifier);

    const response = await fetch(tokenUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: body.toString(),
    });

    if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        console.error('Echec echange code Keycloak :', err);
        return false;
    }

    const data = await response.json();

    // Stockage du token sous la meme cle que le JWT maison.
    // httpClient.js l'injecte automatiquement dans toutes les requetes.
    localStorage.setItem(TOKEN_KEY, data.access_token);

    // Stockage optionnel du refresh_token pour un rafraichissement futur
    if (data.refresh_token) {
        localStorage.setItem('keycloak_refresh_token', data.refresh_token);
    }

    return true;
}

/**
 * Deconnecte l'utilisateur de Keycloak ET du frontend.
 * Redirige vers la page de deconnexion Keycloak qui invalide la session SSO,
 * puis Keycloak redirige vers la page d'accueil du frontend.
 */
export function logoutFromKeycloak() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem('keycloak_refresh_token');

    const logoutUrl = new URL(
        `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/logout`
    );
    logoutUrl.searchParams.set('client_id', KEYCLOAK_CLIENT_ID);
    logoutUrl.searchParams.set('post_logout_redirect_uri', window.location.origin);

    window.location.href = logoutUrl.toString();
}