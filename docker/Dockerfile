FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu
LABEL authors="JackWu"

WORKDIR application
COPY ./start/build/libs/*.jar ./application.jar

ENTRYPOINT ["java", "-jar", "application.jar"]
