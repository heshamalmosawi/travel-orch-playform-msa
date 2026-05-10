FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn

COPY common/pom.xml ./common/pom.xml
COPY common/src ./common/src

RUN chmod +x ./mvnw
RUN ./mvnw -f common/pom.xml install -DskipTests

COPY user-service/pom.xml ./pom.xml
COPY user-service/src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
