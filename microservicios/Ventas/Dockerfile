FROM openjdk:21-jdk-slim

WORKDIR /app

# Usar el JAR shadow que incluye todas las dependencias
COPY microservicios/Ventas/build/libs/Ventas-1.0.0.jar app.jar

EXPOSE 8083

CMD ["java", "-jar", "app.jar"]
