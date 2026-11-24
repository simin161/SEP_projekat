FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl plugins/crypto-plugin -am -DskipTests package

FROM eclipse-temurin:25-jre AS runtime
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/plugins/crypto-plugin/target/crypto-plugin.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
