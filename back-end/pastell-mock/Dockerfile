# ============================================================
# pastell-mock/Dockerfile
# ------------------------------------------------------------
# Multi-stage Maven 3.9.6 + Temurin Java 21 pour le mock Pastell.
#
# IMPORTANT : ce Dockerfile attend que le contexte de build soit la
# RACINE du PROJET MAVEN (back-end/), pas le dossier pastell-mock/.
# Sur Render, configurer :
#   Dockerfile Path : back-end/pastell-mock/Dockerfile
#   Docker Context  : ./back-end
#
# Raison : c'est un projet Maven multi-module. Le parent pom.xml vit
# a la racine du projet (back-end/) et connait les deux modules. Il
# faut donc demarrer le build depuis cette racine pour que Maven puisse
# resoudre la chaine parent -> modules.
# ============================================================

# ───────────────────────────────
# Stage 1 : build avec Maven
# ───────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copier les poms en premier pour profiter du cache Docker.
COPY pom.xml /workspace/pom.xml
COPY sejour-backend/pom.xml /workspace/sejour-backend/pom.xml
COPY pastell-mock/pom.xml /workspace/pastell-mock/pom.xml

# Pre-fetch des dependances Maven du module cible.
# -pl pastell-mock : on construit CE module specifiquement
# -am              : et ses dependances inter-modules si necessaire
# go-offline       : telecharge tout sans encore compiler
RUN mvn -pl pastell-mock -am dependency:go-offline -B

# Copier le code source du module cible UNIQUEMENT.
# On ne copie pas sejour-backend/src pour ne pas invalider le cache
# Docker quand on touche au backend seul.
COPY pastell-mock/src /workspace/pastell-mock/src

# Build, sans les tests (rapide sur Render).
RUN mvn -pl pastell-mock -am clean package -DskipTests -B

# ───────────────────────────────
# Stage 2 : runtime minimal
# ───────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copier le JAR Spring Boot fat-jar depuis le builder.
COPY --from=builder /workspace/pastell-mock/target/*.jar /app/app.jar

# Port d'ecoute (Render le configure via la variable PORT).
EXPOSE 8090

# JVM tuning leger pour les conteneurs free tier (512 Mo RAM Render).
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0"

# Profil prod active par defaut (Render peut surcharger via SPRING_PROFILES_ACTIVE).
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck Docker (Render utilise plutot /api/version.php cote prod
# parce que le mock a un catch-all controller qui attrape /actuator/health
# en disant "Route inexistante". On garde la commande wget ici pour le
# fallback local mais ce n'est pas le healthcheck reel cote Render).
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8090}/api/version.php || exit 1

ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
