FROM maven:3.9.2-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/target/kstreams-wordcount-1.0-SNAPSHOT.jar app.jar
COPY --from=builder /workspace/target/classes/logback.xml resources/logback.xml
COPY --from=builder /workspace/target/classes/print_latest_rocksdb_options.sh resources/print_latest_rocksdb_options.sh

RUN chmod +x resources/print_latest_rocksdb_options.sh

EXPOSE 7071

ENTRYPOINT java $JAVA_OPTS -Dlogback.configurationFile="/app/resources/logback.xml" -cp /app/app.jar studio.goodlabs.WordCountStreamApp
