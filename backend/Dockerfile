FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -Dmaven.test.skip=true dependency:go-offline

COPY src ./src
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
RUN groupadd --system campushub \
    && useradd --system --gid campushub --home-dir /app campushub \
    && mkdir -p /app/uploads /app/uploads-private \
    && chown -R campushub:campushub /app

COPY --from=build --chown=campushub:campushub /workspace/target/campus-hub-*.jar /app/app.jar

USER campushub
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
