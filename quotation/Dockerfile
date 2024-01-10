FROM openjdk:latest
LABEL authors="AlexLing"

ARG packageName=quotation-1.0.0.jar

WORKDIR /app

COPY target/${packageName}  /app/app.jar

EXPOSE 8005

CMD ["java", "-jar", "/app/app.jar"]