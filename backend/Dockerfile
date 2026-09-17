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
ENV SPRING_PROFILES_ACTIVE=cloud
ENV DB_POOL_MAX_SIZE=5
ENV DB_POOL_MIN_IDLE=1
# Render's free web service is limited to 512 MiB. Keep explicit headroom for
# metaspace, code cache, thread stacks and native allocations instead of letting
# the heap consume 75% of the container by itself.
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -Xss512k -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=5m --retries=3 CMD wget -q -O - "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

