# ============================================================
# back-end/keycloak/Dockerfile
#
# Image Keycloak standalone pour Railway (ou tout hote Docker).
#
# Ce Dockerfile embarque le realm-export.json directement dans
# l'image. Au premier demarrage, Keycloak importe automatiquement
# le realm "springhotel" avec :
#   - les roles (ROLE_ADMIN, ROLE_EMPLOYE, ROLE_USER)
#   - les scopes (openid, profile, email, pastell-admin)
#   - le client public PKCE springhotel-frontend
#   - les comptes de demo (admin-demo / user-demo)
#
# Mode start-dev : base de donnees H2 embarquee (pas de Postgres
# requis). A chaque redemarrage du conteneur, la base est reinitia-
# lisee et le realm est reimporte depuis ce fichier. C'est
# intentionnel pour un usage demo/portfolio.
#
# Variables d'environnement a definir dans Railway :
#   KC_BOOTSTRAP_ADMIN_USERNAME  (ex: admin)
#   KC_BOOTSTRAP_ADMIN_PASSWORD  (fort, ne pas mettre "admin" en prod)
#
# Port expose : 8080 (Railway le detecte automatiquement).
# ============================================================

FROM quay.io/keycloak/keycloak:26.0

# Copier le realm dans le dossier d'import automatique de Keycloak.
# Ce chemin est lu au demarrage quand --import-realm est passe.
COPY realm-export.json /opt/keycloak/data/import/realm-export.json

# hostname-strict=false : necessaire derriere le reverse proxy de Railway
# pour que Keycloak accepte les requetes sans validation stricte du hostname.
# proxy-headers=xforwarded : Railway transmet X-Forwarded-For et
# X-Forwarded-Proto, ce qui permet a Keycloak de connaitre son URL publique
# HTTPS et de generer des tokens avec le bon "iss".
CMD ["start-dev", \
     "--import-realm", \
     "--hostname-strict=false", \
     "--proxy-headers=xforwarded"]