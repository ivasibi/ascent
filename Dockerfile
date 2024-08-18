FROM maven:3.9.8 AS build
WORKDIR /ascent
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21.0.4 AS runtime
WORKDIR /ascent
COPY --from=build /ascent/target/*.jar ascent.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ascent.jar"]