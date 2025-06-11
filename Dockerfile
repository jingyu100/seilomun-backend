FROM openjdk:21
ARG JAR_FILE=seilomun/build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
