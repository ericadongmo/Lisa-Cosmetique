# Image de production du backend Lisa Cosmétique (Spring Boot 3.5, Java 21).
# Utilisée par Render / Railway / Koyeb / Fly.io. Aucune variable sensible n'est copiée :
# elles sont fournies par l'hébergeur au démarrage (voir docs/DEPLOIEMENT.md).

# ---- 1. Compilation ---------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dépendances d'abord (mises en cache tant que pom.xml ne change pas)
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/*.jar app.jar

# ---- 2. Exécution -----------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

# Render fournit PORT ; SERVER_PORT reste utilisable ailleurs.
EXPOSE 8080
CMD ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]
