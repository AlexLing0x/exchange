FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=push-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8006

CMD ["java", "-jar", "/app/app.jar"]