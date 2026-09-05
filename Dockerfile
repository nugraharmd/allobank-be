# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml dan source code
COPY pom.xml .
COPY src ./src

# Build project
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy hasil build dari stage 1
COPY --from=build /app/target/allobank-0.0.1-SNAPSHOT.jar app.jar

# Jalankan aplikasi
ENTRYPOINT ["java", "-jar", "app.jar"]
