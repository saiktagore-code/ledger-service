FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q package -DskipTests -DskipITs -Dmaven.javadoc.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/account-service-0.1.0.jar ./account-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "./account-service.jar"]
