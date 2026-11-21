FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package

# Runtime must be a Paper/Spigot 1.12.2 server.
# The previous EaglerX 1.8 image is not compatible with this plugin's 1.12.2 API.
