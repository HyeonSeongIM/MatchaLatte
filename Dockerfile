FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# JAR 복사
COPY core/core-api/build/libs/*.jar app.jar

# entrypoint는 wait-for-it으로 변경
CMD ["java", "-jar", "-Duser.timezone=Asia/Seoul", "/app/app.jar"]
