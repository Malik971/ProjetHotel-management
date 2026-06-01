#!/bin/bash
# Lance les services locaux SpringHotel.
# Prerequis : Docker installe et en cours d'execution.

export PASTELL_MASTER_SECRET=f63f76d64847845a37c2eea49f8018867fd571722405c530c28c26929e4bf74b
export DEMO_ADMIN_TOKEN=d38b3228d5007745c44f367627ebce51

echo "Variables exportees :"
echo "  PASTELL_MASTER_SECRET : ${PASTELL_MASTER_SECRET:0:8}... (tronque)"
echo "  DEMO_ADMIN_TOKEN      : ${DEMO_ADMIN_TOKEN:0:8}... (tronque)"
echo ""

case "$1" in

  infra)
    echo "Demarrage de l'infrastructure Docker (PostgreSQL + Keycloak)..."
    docker compose up -d
    echo ""
    echo "Services disponibles :"
    echo "  PostgreSQL     : localhost:5432"
    echo "  Keycloak       : http://localhost:8180"
    echo "  Console admin  : http://localhost:8180/admin  (admin / admin)"
    echo ""
    echo "Keycloak met 30 a 60 secondes a etre pret."
    echo "Surveiller les logs : docker compose logs -f keycloak"
    echo "Le realm 'springhotel' est importe automatiquement au premier demarrage."
    ;;

  infra-stop)
    echo "Arret des conteneurs..."
    docker compose down
    ;;

  infra-reset)
    echo "Suppression des volumes (le realm sera reimporte au prochain demarrage)..."
    docker compose down -v
    ;;

  mock)
    java -jar pastell-mock/target/pastell-mock-0.0.1-SNAPSHOT.jar
    ;;

  backend)
    java -jar sejour-backend/target/sejour-backend-0.0.1-SNAPSHOT.jar
    ;;

  *)
    echo "Usage : ./run-local.sh [commande]"
    echo ""
    echo "  infra        : demarre PostgreSQL et Keycloak (Docker Compose)"
    echo "  infra-stop   : arrete les conteneurs"
    echo "  infra-reset  : supprime les volumes (reset complet, reimport realm)"
    echo "  mock         : demarre pastell-mock sur le port 8090"
    echo "  backend      : demarre sejour-backend sur le port 8080"
    echo ""
    echo "Ordre de demarrage recommande :"
    echo "  1. ./run-local.sh infra"
    echo "  2. Attendre que Keycloak soit pret (voir logs)"
    echo "  3. ./run-local.sh mock &"
    echo "  4. ./run-local.sh backend"
    echo "  5. cd sejour-frontend && npm run dev"
    exit 1
    ;;

esac
