FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle ./
COPY teg-admin/build.gradle teg-admin/
RUN chmod +x gradlew && ./gradlew :teg-admin:dependencies --no-daemon || true
COPY teg-admin/src/ teg-admin/src/
RUN ./gradlew :teg-admin:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/teg-admin/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
