FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
COPY scripts/render-start.sh ./scripts/render-start.sh

RUN mvn -B clean package -DskipTests \
    && find target -maxdepth 1 -type f -name "*.jar" ! -name "original-*" -exec cp {} /app/app.jar \;

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar /app/app.jar
COPY --from=build /app/scripts/render-start.sh /app/render-start.sh

RUN chmod +x /app/render-start.sh

EXPOSE 10000

CMD ["/app/render-start.sh"]
