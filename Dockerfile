FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]