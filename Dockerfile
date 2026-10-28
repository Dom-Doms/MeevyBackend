# Stage di build: Java 21 richiesto da MeevyBackend.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Mantiene la cache delle dipendenze quando cambia soltanto il codice.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Runtime leggero, senza Maven e senza privilegi root.
FROM eclipse-temurin:21-jre-noble
RUN useradd --system --uid 10001 --create-home --home-dir /app meevy
WORKDIR /app
COPY --from=build --chown=meevy:meevy /workspace/target/meevy-backend-0.0.1-SNAPSHOT.jar /app/app.jar
USER meevy
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
