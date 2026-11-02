FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/FIFA-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
