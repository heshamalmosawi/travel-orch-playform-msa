FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8443

COPY wait-for-services.sh .
RUN chmod +x wait-for-services.sh

CMD ["./wait-for-services.sh", "java", "-jar", "target/apigateway-0.0.1-SNAPSHOT.jar"]