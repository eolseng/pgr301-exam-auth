# syntax = docker/dockerfile:experimental

# This is a multi-stage Dockerfile for Spring Boot Applications made as a Layered Jar
# It uses experimental features - enabled by first line - and also needs 'DOCKER_BUILDKIT=1' enabled
# '--mount=type=cache,target=/root/.m2' caches the Maven Dependencies so build times are much faster
# Must have Docker version 18.09 or higher

# Build the image with: 'DOCKER_BUILDKIT=1 docker build -f Dockerfile -t name:tag .'
# Optionally add "--build-arg JAR_FILE=..." to specify the JAR-file name.

#########################
### BUILD IMAGE STAGE ###
#########################
# Loads a JDK11 Base Image with Maven installed
FROM maven:3.6.3-jdk-11 as builder
WORKDIR app
# Copies over the POM-file for dependencies and the src-folder
COPY pom.xml .
COPY src src
# Caches the /.m2-folder to cache Maven Dependencies. This is an experimental feature.
# Builds the program with 'mvn install'. '-T 1C' makes the build multi-threded (1 thread / core) to speed it up.
RUN --mount=type=cache,target=/root/.m2 mvn -T 1C install -DskipTests
# Locates the generated .jar-file.
# '*.jar' must be replaced with precise filename if multiple .jar-files exist after 'mvn install'.
ARG JAR_FILE=target/*.jar
# Unpacks the .jar-file that has been created as a Spring Boot Layered Jar.
# Allows caching of different layers to speed up build time in the Production Image.
RUN java -Djarmode=layertools -jar ${JAR_FILE} extract

##############################
### PRODUCTION IMAGE STAGE ###
##############################
# Loads a Slim Alpine JDK11 Base Image for smallest possible image size.
FROM adoptopenjdk/openjdk11:alpine-slim
WORKDIR app
# Run the application with non-Root Privileges for security reasons.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
# Copy over the different layers of the .jar
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
