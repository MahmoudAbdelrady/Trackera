# Use Amazon Corretto JDK
FROM amazoncorretto:25-alpine AS build

WORKDIR /app

# Copy ONLY Maven files first (for dependency caching)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the app (uses cached dependencies)
RUN ./mvnw clean package -DskipTests

# Run stage
FROM amazoncorretto:25-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]