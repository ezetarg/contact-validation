FROM openjdk:14-alpine
COPY build/libs/phone-validation-*-all.jar phone-validation.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "phone-validation.jar"]