FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=trading-engine-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8002

CMD ["java", "-jar", "/app/app.jar"]