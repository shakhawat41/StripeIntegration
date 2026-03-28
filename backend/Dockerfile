# =============================================================================
# Backend Dockerfile — Copy-only (pre-built JAR)
# Build locally with: ./gradlew bootJar
# Then Docker just copies the JAR into a minimal JRE image.
# =============================================================================

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the pre-built Spring Boot fat JAR from the local build output
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
