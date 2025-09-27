FROM openjdk:21-jdk-slim

WORKDIR /app

COPY microservicios/Clientes/build/libs/Clientes-1.0.0.jar app.jar

EXPOSE 8082

CMD ["java", "-jar", "app.jar"]
