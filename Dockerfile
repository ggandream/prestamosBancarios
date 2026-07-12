# =====================================================================================
# Dockerfile STARTER (Fase 2 — Andrea). Base funcional para que `docker compose up`
# levante la app end-to-end. Luis lo endurecerá/optimizará en la Fase 5 (capas de
# dependencias cacheadas, JRE slim final, afinado de usuario no root, etc.).
# =====================================================================================

# ---- build ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Se copia primero el pom para aprovechar la cache de dependencias entre builds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime -------------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app
# Usuario no root por seguridad básica.
RUN useradd --system --uid 1001 spring
COPY --from=build /app/target/prestamos-*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
