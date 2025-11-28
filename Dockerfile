# Etapa 1: construir o JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia **apenas** arquivos essenciais
COPY pom.xml .
COPY src ./src

RUN mvn -e -X clean package -DskipTests

# Etapa 2: rodar o JAR
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=10000
EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]