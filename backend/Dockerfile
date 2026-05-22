FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:8-jre

WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads/repair /app/uploads/service /app/uploads/social
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
