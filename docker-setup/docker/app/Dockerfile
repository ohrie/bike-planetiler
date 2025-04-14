FROM eclipse-temurin:21-alpine

COPY --from=compiler /app/target/*-with-deps.jar /app/planetiler.jar

CMD ["java", "-jar", "/app/planetiler.jar"]
