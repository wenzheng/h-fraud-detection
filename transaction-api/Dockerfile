FROM eclipse-temurin:17-jre
WORKDIR /app
COPY transaction-api/target/transaction-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
