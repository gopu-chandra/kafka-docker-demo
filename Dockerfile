# ---- Stage 1: build the JAR using Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first (cached unless pom.xml changes, so rebuilds are faster)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn -q package -DskipTests

# ---- Stage 2: run the JAR on a slim Java runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
