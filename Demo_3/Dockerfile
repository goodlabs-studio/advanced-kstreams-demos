FROM maven:3.9.2-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests \
 && mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/target/kstreams-joins-examples-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /workspace/target/dependency/*.jar libs/

COPY --from=builder /workspace/src/main/resources/logback.xml resources/logback.xml

ENTRYPOINT ["java", "-Dlogback.configurationFile=/app/resources/logback.xml", "-cp", "/app/app.jar:/app/libs/*"]
CMD ["studio.goodlabs.StreamStreamJoinApp"]
