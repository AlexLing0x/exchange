FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=trading-sequencer-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8003

CMD ["java", "-jar", "/app/app.jar"]