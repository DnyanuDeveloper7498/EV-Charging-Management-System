# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["sh","-c","exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080} && printf 'GET /actuator/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
