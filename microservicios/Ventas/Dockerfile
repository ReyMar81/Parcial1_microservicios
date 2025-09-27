FROM openjdk:17-jdk-slim

WORKDIR /app

COPY microservicios/Ventas/build/libs/Ventas-*.jar app.jar

EXPOSE 8083

CMD ["java", "-jar", "app.jar"]
