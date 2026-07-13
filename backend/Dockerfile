# ============================================
# Build stage — Maven + JDK 21
# ============================================
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B || true

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# Runtime stage — JRE 21
# ============================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose PORT (Render injects this)
EXPOSE ${PORT:-8080}

# Run the application — respect Render's PORT env var
ENTRYPOINT ["java", "-jar", "app.jar"]
