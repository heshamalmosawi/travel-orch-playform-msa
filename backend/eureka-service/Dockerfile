FROM eclipse-temurin:24-jdk

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8761

CMD ["java", "-jar", "target/eurekaserver-0.0.1-SNAPSHOT.jar"]