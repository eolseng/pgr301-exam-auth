# This is a multi-stage Dockerfile for Spring Boot Applications made as a Layered Jar
# Build the image with: 'docker build -f Dockerfile -t [IMAGE_NAME]:[TAG] .'

#########################
### BUILD IMAGE STAGE ###
#########################
# Loads a JDK11 Base Image with Maven installed
FROM maven:3.6.3-jdk-11 as builder
# Create and set the working directory
WORKDIR application
# Copies over the POM-file for dependencies and the src-folder
COPY pom.xml .
COPY src src
# Builds the program with 'mvn install'. '-T 1C' makes the build multi-threded (1 thread / core) to speed it up
RUN mvn -T 1C install -DskipTests
# Locates the generated .jar-file
# '*.jar' must be replaced with precise filename if multiple .jar-files exist after 'mvn install'
ARG JAR_FILE=target/*.jar
# Unpacks the .jar-file that has been created as a Spring Boot Layered Jar
# Allows caching of different layers to speed up build time in the Production Image
RUN java -Djarmode=layertools -jar ${JAR_FILE} extract

##############################
### PRODUCTION IMAGE STAGE ###
##############################
# Loads a Slim Alpine JDK11 Base Image
FROM adoptopenjdk/openjdk11:alpine-slim
WORKDIR application
# Run the application with non-Root Privileges for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
# Copy over the different layers of the .jar
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
