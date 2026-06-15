// ============================================================
// config.js - Source unique de configuration du dashboard
// ============================================================
//
// Une seule source de verite pour les reglages : pas de script inline dans
// index.html, pas de substitution localhost. Les valeurs par defaut ci-dessous
// peuvent etre surchargees par l'utilisateur via le ConfigDrawer, et sont alors
// persistees en localStorage (voir composables/useConfig.js).
//
// Le dashboard ne parle qu'a sejour-backend : c'est lui qui porte les
// credentials Pastell cote serveur et relaie vers le connecteur. Le navigateur
// ne contacte jamais le mock en direct.
// ============================================================

/**
 * Reglages par defaut.
 *
 * backendUrl : racine de sejour-backend en production sur Railway.
 * demoToken  : en-tete X-Demo-Token attendu par les operations qui font avancer
 *              le bus (poll, relance, action de demo). Repris de l'ancien
 *              dashboard/config.js. Visible cote client, c'est assume : il ne
 *              sert qu'a couper le bruit des robots, pas a authentifier.
 */
export const DEFAULT_CONFIG = {
    backendUrl: "https://sejour-backend-production.up.railway.app",
    demoToken: "b3d92d3683a75662c00ce072b0274f17",
};

/**
 * Cle de persistance en localStorage. Prefixe dedie pour eviter toute collision
 * avec d'autres applications servies sur le meme domaine.
 */
export const STORAGE_KEY = "dashboard-vue.config";
