FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY apigateway-service/pom.xml ./pom.xml
COPY apigateway-service/src ./src

RUN chmod +x ./mvnw
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/apigateway-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8443
CMD ["java", "-jar", "app.jar"]
