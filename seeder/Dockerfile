# =============================================================================
# Seeder Dockerfile — Copy-only (pre-built JAR)
# Build locally with: ./gradlew bootJar
# Short-lived container that seeds test data, then exits.
# =============================================================================

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the pre-built seeder JAR from the local build output
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
