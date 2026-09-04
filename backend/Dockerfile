# =====================================================================
# Multi-stage Dockerfile for EdTech Platform Backend (Spring Boot 3)
# =====================================================================

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy build descriptor and source
COPY pom.xml .
COPY src ./src

# Install maven and build jar (skip tests for fast image builds)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    cp target/*.jar app.jar && \
    java -Djarmode=layertools -jar app.jar extract

# Stage 2: Production JRE runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy layered jar dependencies
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Container environment
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -q -O - http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

