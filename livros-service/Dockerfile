# ---------- Estagio 1: build com Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia so o pom primeiro para aproveitar o cache das dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Estagio 2: imagem final, so com o JRE ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
