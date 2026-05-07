FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG MODULE

COPY pom.xml ./
COPY shared ./shared
COPY generator ./generator
COPY consumer-plate ./consumer-plate
COPY consumer-sign ./consumer-sign

RUN mvn -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

ARG MODULE

COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0.0.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]