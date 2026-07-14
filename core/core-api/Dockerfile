FROM eclipse-temurin:17-jdk

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

CMD ["java", \
     "-Xmx450m", \
     "-XX:+UseG1GC", \
     "-XX:ParallelGCThreads=2", \
     "-XX:+ExitOnOutOfMemoryError", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/app/logs/heap-dump.hprof", \
     "-Duser.timezone=Asia/Seoul", \
     "-Dspring.profiles.active=dev", \
     "-jar", "/app/app.jar"]
