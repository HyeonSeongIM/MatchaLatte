FROM eclipse-temurin:17-jdk

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

CMD ["java", "-jar", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=dev", "/app/app.jar"]
