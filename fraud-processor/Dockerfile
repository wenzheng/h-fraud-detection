FROM eclipse-temurin:17-jre
WORKDIR /app
COPY fraud-processor/target/fraud-processor-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
