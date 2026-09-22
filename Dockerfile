# -------------------------------------------------------------
# Stage 1: Build the application with Maven
# -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy pom.xml and cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and compile JAR
COPY src ./src
RUN mvn clean package -DskipTests

# -------------------------------------------------------------
# Stage 2: Lightweight Production JRE Runtime (~180MB)
# -------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Persistent directory for SQLite database
RUN mkdir -p /app/data

# Copy executable jar from builder stage
COPY --from=builder /build/target/goal-decomposition-engine.jar app.jar

# Default environment variables
ENV PORT=8080
ENV DB_PATH=/app/data/goal_decomposition.db

EXPOSE 8080

# Expose SQLite data volume
VOLUME ["/app/data"]

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
