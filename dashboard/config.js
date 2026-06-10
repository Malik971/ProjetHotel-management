/**
 * dashboard/config.js
 * --------------------------------------------------------------
 * Configuration runtime du dashboard de demo Pastell.
 *
 * Pourquoi un fichier .js plutot que des variables d'environnement
 * compilees ? Le dashboard est statique pur (pas de build step), donc
 * il n'y a pas d'etape ou injecter des variables au moment du bundle.
 * Une option externalisable au runtime est plus simple : on modifie ce
 * fichier (ou on le re-genere via le netlify.toml) sans redeployer le
 * code du dashboard.
 *
 * Les valeurs ci-dessous peuvent etre surchargees au moment du deploy
 * Netlify en branchant le contenu de ce fichier sur une variable
 * d'environnement, ou en l'editant directement avant publish.
 *
 * SECURITE : DEMO_ADMIN_TOKEN sera visible cote client, c'est assume.
 * Cette protection sert uniquement a couper le bruit de bots aveugles,
 * pas a empecher un attaquant determine. Voir DEMO_PUBLIQUE.md.
 */
window.SPRINGHOTEL_CONFIG = {
  // URL du backend principal sejour-backend deploye sur Railway.
  backendUrl: "https://sejour-backend-production.up.railway.app",

  // URL du mock Pastell deploye sur Railway (service separe).
  mockUrl: "https://pastell-mock-production.up.railway.app",

  // Token envoye sur le header X-Demo-Token pour les operations destructives
  // (typiquement le bouton "forcer un poll" du dashboard).
  // Definir la meme valeur cote Render (variable DEMO_ADMIN_TOKEN).
  demoAdminToken: "b3d92d3683a75662c00ce072b0274f17",

  // Intervalle de rafraichissement de la page status.html (millisecondes).
  statusRefreshIntervalMs: 30000,
};
