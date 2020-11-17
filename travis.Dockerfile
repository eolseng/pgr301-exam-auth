FROM openjdk:11.0.8-jre-slim
WORKDIR app
COPY dependencies/ ./
COPY snapshot-dependencies/ ./
COPY spring-boot-loader/ ./
COPY application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
