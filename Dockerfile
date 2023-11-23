FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu
LABEL authors="JackWu"

WORKDIR application
COPY ./start/build/libs/*.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
