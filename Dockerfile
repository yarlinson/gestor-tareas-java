# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app

# Se copia primero solo el pom para aprovechar la caché de capas de Docker.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/target/gestor-tareas-*.jar app.jar
USER appuser

EXPOSE 8080

# Ajustes de memoria pensados para instancias pequeñas (512 MB). Se pueden cambiar
# definiendo la variable de entorno JAVA_OPTS en la plataforma.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
