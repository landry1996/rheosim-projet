FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY rheosim-domain/pom.xml rheosim-domain/
COPY rheosim-application/pom.xml rheosim-application/
COPY rheosim-infrastructure/pom.xml rheosim-infrastructure/
COPY rheosim-bootstrap/pom.xml rheosim-bootstrap/
RUN apk add --no-cache maven && mvn dependency:go-offline -B

COPY . .
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S rheosim && adduser -S rheosim -G rheosim
COPY --from=build /app/rheosim-bootstrap/target/*.jar app.jar
RUN chown rheosim:rheosim app.jar
USER rheosim

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
