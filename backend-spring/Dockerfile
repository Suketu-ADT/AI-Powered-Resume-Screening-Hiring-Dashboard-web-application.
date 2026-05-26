# Stage 1: Build the application jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy configuration and source files
COPY pom.xml .
COPY src ./src

# Compile and package the application jar, skipping unit tests for speed during build
RUN mvn clean package -DskipTests

# Stage 2: Minimalist runtime container
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (matches the one configured in application.yml)
EXPOSE 8888

# Run the spring application
ENTRYPOINT ["java", "-jar", "app.jar"]
