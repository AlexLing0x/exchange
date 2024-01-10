FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=trading-api-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8001

CMD ["java", "-jar", "/app/app.jar"]