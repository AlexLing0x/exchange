FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=ui-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8000

CMD ["java", "-jar", "/app/app.jar"]