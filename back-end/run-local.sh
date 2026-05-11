#!/bin/bash
# Lance les deux services localement avec les bonnes variables d'env.

export PASTELL_MASTER_SECRET=f63f76d64847845a37c2eea49f8018867fd571722405c530c28c26929e4bf74b
export DEMO_ADMIN_TOKEN=d38b3228d5007745c44f367627ebce51

echo "Variables exportees :"
echo "  PASTELL_MASTER_SECRET : ${PASTELL_MASTER_SECRET:0:8}... (tronque)"
echo "  DEMO_ADMIN_TOKEN      : ${DEMO_ADMIN_TOKEN:0:8}... (tronque)"

# Choix : tu lances ce script avec un argument pour savoir quel service demarrer
case "$1" in
  mock)
    java -jar pastell-mock/target/pastell-mock-0.0.1-SNAPSHOT.jar
    ;;
  backend)
    java -jar sejour-backend/target/sejour-backend-0.0.1-SNAPSHOT.jar
    ;;
  *)
    echo "Usage : ./run-local.sh [mock|backend]"
    exit 1
    ;;
esac