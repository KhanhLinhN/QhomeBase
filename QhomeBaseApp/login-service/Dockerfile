# Stage 1: Build
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy toàn bộ project (vì context là QhomeBaseApp)
COPY . .

# Build riêng login-service
RUN mvn clean package -DskipTests -pl login-service -am

# Stage 2: Run
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy JAR đã build từ login-service
COPY --from=build /app/login-service/target/login-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]