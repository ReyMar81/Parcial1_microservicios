FROM openjdk:21-jdk-slim

WORKDIR /app

COPY API-Gateway/build/libs/API-Gateway-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
