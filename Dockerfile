FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copia as dependências primeiro para aproveitar o cache do Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e gera o .jar
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Etapa 2: Execução leve ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copia o .jar gerado na etapa de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]