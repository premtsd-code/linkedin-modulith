# Multi-stage build -> small runtime image with just the JRE + fat jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Profiles/DB/Kafka are supplied by docker-compose via env vars.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
