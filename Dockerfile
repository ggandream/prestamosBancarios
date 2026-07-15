# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean verify

FROM eclipse-temurin:25-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S prestamos \
    && adduser -S -G prestamos -h /app prestamos

WORKDIR /app
COPY --from=build --chown=prestamos:prestamos /workspace/target/prestamos-*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"
EXPOSE 8080

USER prestamos:prestamos

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
