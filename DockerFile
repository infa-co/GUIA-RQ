# Etapa 1: Build com Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Rodar o app
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/guia-rq-1.0.0.jar app.jar

# Define porta padrão do Spring Boot
EXPOSE 8080

# Define comando de execução
ENTRYPOINT ["java", "-jar", "app.jar"]
