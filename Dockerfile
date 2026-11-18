FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package

FROM ghcr.io/yangchuansheng/eaglerx1.8server:2.2.7
ENV MINECRAFT_VERSION=1.8
COPY --from=build /build/target/ThonereachSMP.jar /opt/eaglerX-1.8-server-image/server-1.8/plugins/ThonereachSMP.jar
