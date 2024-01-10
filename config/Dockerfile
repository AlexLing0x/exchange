#Dockerfile for config
FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=config-1.0.0.jar

WORKDIR /app

COPY config-repo /app/config-repo

COPY target/${packageName}  /app/app.jar

EXPOSE 8888

CMD ["java", "-jar", "/app/app.jar"]
