# Step 1: Build the application using Maven and Java 23
FROM maven:3.9.9-amazoncorretto-23 AS build
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the application using Java 23
FROM amazoncorretto:23-alpine
COPY --from=build /target/skillswap-0.0.1-SNAPSHOT.jar skillswap.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "skillswap.jar"]
