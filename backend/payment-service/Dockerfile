FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8084

CMD ["java", "-jar", "target/payment-service-0.0.1-SNAPSHOT.jar"]
