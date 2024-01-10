FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=eureka-server-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8761

CMD ["java", "-jar", "/app/app.jar"]