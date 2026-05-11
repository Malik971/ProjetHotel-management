# ============================================================
# sejour-backend/Dockerfile
# ------------------------------------------------------------
# Multi-stage Maven 3.9.6 + Temurin Java 21.
#
# IMPORTANT : ce Dockerfile attend que le contexte de build soit la
# RACINE du repo, pas le dossier sejour-backend/. Sur Render, configurer :
#   Dockerfile Path : sejour-backend/Dockerfile
#   Docker Context  : . (racine du repo)
#
# Raison : c'est un projet Maven multi-module. Le parent pom.xml vit
# a la racine, et il connait les deux modules. Il faut donc demarrer
# le build depuis la racine pour que Maven puisse resoudre la chaine
# parent -> modules.
# ============================================================

# ───────────────────────────────
# Stage 1 : build avec Maven
# ───────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copier les poms en premier pour profiter du cache Docker :
# si seul le code change (sans toucher aux dependances), cette etape
# ne sera pas re-jouee, et Maven n'aura pas a retelecharger Internet.
COPY pom.xml /workspace/pom.xml
COPY sejour-backend/pom.xml /workspace/sejour-backend/pom.xml
COPY pastell-mock/pom.xml /workspace/pastell-mock/pom.xml

# Pre-fetch des dependances Maven du module cible.
# -pl sejour-backend : on construit ce module
# -am               : et ses dependances inter-modules (none ici, mais defensif)
# go-offline        : telecharge tout ce qu'il faut sans encore compiler.
RUN mvn -pl sejour-backend -am dependency:go-offline -B

# Copier le code source du module cible UNIQUEMENT.
# On ne copie pas pastell-mock/src, pour ne pas invalider le cache
# Docker quand on touche au mock seul.
COPY sejour-backend/src /workspace/sejour-backend/src

# Build, sans les tests (rapide sur Render, les tests tournent en CI
# avant le merge sur main).
RUN mvn -pl sejour-backend -am clean package -DskipTests -B

# ───────────────────────────────
# Stage 2 : runtime minimal
# ───────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copier le JAR Spring Boot fat-jar depuis le builder.
# Le wildcard *.jar capture le nom version sans avoir a le coder en dur.
COPY --from=builder /workspace/sejour-backend/target/*.jar /app/app.jar

# Port d'ecoute (Render le configure via la variable PORT).
# EXPOSE est informatif uniquement.
EXPOSE 8080

# JVM tuning leger pour les conteneurs free tier (512 Mo RAM Render).
# UseContainerSupport est ON par defaut depuis Java 10, mais on le rappelle.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0"

# Profil prod active par defaut (Render peut surcharger via SPRING_PROFILES_ACTIVE).
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck Docker (Render n'en a pas besoin, mais utile en local).
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
