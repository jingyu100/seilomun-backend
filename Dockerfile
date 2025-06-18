FROM openjdk:21
ARG JAR_FILE=seilomun/build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 타임존 설정 추가
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM 타임존과 함께 실행
ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-jar","/app.jar"]
