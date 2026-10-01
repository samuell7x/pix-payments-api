# ---- Build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Maven instalado direto na imagem: o mvnw dava falha silenciosa dentro do
# Docker. O wrapper continua existindo apenas para uso local.
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
# curl e necessario apenas para o HEALTHCHECK abaixo (imagem base nao traz)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN addgroup --system app && adduser --system --ingroup app app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
