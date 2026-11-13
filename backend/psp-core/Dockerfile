FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl psp-core -am -DskipTests package

FROM eclipse-temurin:25-jre AS runtime
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/psp-core/target/psp-core.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
