FROM maven:3.9.2-eclipse-temurin-11 AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=builder /workspace/target/order-processor-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /workspace/target/classes/logback.xml resources/logback.xml

EXPOSE 7071

ENTRYPOINT ["java", "-Dlogback.configurationFile=/app/resources/logback.xml", "-cp", "/app/app.jar:/app/libs/*"]
CMD ["com.example.OrderProcessorApplication"]
