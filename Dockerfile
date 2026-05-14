FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system discountcode && adduser --system --ingroup discountcode discountcode
USER discountcode

COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
