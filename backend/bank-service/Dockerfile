# syntax=docker/dockerfile:1
# Build context is the backend/ folder (the parent pom is needed):
#   docker build -f bank-service/Dockerfile -t sep/bank-service .
FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl bank-service -am -DskipTests package

FROM eclipse-temurin:25-jre AS runtime
# The service runs as a normal user, not as root.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/bank-service/target/bank-service.jar app.jar
USER app
EXPOSE 8086
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
