# Step 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Tuned for Render Free Tier (512MB RAM cap):
# - Limits heap to 256MB, leaving 256MB for Metaspace, threads, off-heap and OS
# - Serial GC drastically cuts GC memory bookkeeping overhead vs G1GC
# - Caps Metaspace & CodeCache to prevent slow memory creep
# - Reduces thread stack from 1MB to 512KB
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+UseSerialGC -Djava.awt.headless=true -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]