# syntax=docker/dockerfile:1
# Habit Tracker API — container image for Render (or any Docker host).
#
#   docker build -t habit-tracker-api .
#   docker run --rm -p 8080:8080 --env-file .env habit-tracker-api
#
# Render injects PORT; the app listens on ${PORT:-8080}.

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Dependencies first, so code-only changes reuse this cached layer.
# go-offline can miss a few plugin artifacts; the package step fetches whatever is left.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q package -DskipTests \
 && cp target/habit-tracker-api.jar /src/app.jar

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --uid 1001 --no-create-home app
COPY --from=build --chown=app:app /src/app.jar app.jar
USER app

# JVM sized for small instances (Render free/starter = 512 MB): heap = 70% of the container limit,
# small thread stacks, serial GC, C1-only JIT for faster cold starts, and exit (so Render restarts) on OOM.
# Override JAVA_OPTS in Render's environment settings if you move to a bigger instance.
ENV TZ=UTC \
    PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
