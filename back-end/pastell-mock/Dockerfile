# ============================================================
# pastell-mock/Dockerfile
# ------------------------------------------------------------
# Multi-stage symetrique a sejour-backend/Dockerfile.
#
# Sur Render, configurer :
#   Dockerfile Path : pastell-mock/Dockerfile
#   Docker Context  : . (racine du repo)
# ============================================================

# ───────────────────────────────
# Stage 1 : build avec Maven
# ───────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml /workspace/pom.xml
COPY sejour-backend/pom.xml /workspace/sejour-backend/pom.xml
COPY pastell-mock/pom.xml /workspace/pastell-mock/pom.xml

RUN mvn -pl pastell-mock -am dependency:go-offline -B

# Cette fois on copie le src du mock uniquement.
COPY pastell-mock/src /workspace/pastell-mock/src

RUN mvn -pl pastell-mock -am clean package -DskipTests -B

# ───────────────────────────────
# Stage 2 : runtime minimal
# ───────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/pastell-mock/target/*.jar /app/app.jar

EXPOSE 8090

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8090}/actuator/health || exit 1

ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
