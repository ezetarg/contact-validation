FROM openjdk:14-alpine
COPY build/libs/*-all.jar app.jar
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "app.jar"]