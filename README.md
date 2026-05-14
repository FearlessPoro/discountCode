# Discount Code Service

Spring Boot REST service for managing discount coupons.

## Requirements

- JDK 21 available on `PATH`
- Docker Desktop with Docker Compose
- Internet access for the first Gradle/Docker run, so dependencies and images can be downloaded

Check the local tools:

```powershell
java -version
docker --version
docker compose version
```

`java -version` should report Java 21.

## Database

Start MariaDB locally:

```powershell
docker compose up -d mariadb
```

Check that it is healthy:

```powershell
docker compose ps
```

Local database settings are defined in `docker-compose.yml`:

```text
database: discount_code
user: discount_code
password: discount_code
root password: discount_code_root
port: 3306
```

## Build And Test

Use the Gradle wrapper from the repository:

```powershell
.\gradlew.bat test
.\gradlew.bat bootJar
```

On Linux or macOS:

```sh
./gradlew test
./gradlew bootJar
```

## Run The App

With MariaDB already running, start the app locally:

```powershell
.\gradlew.bat bootRun
```

The app listens on port `8080`.

The default local datasource configuration is in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/discount_code
spring.datasource.username=discount_code
spring.datasource.password=discount_code
```

The values can be overridden with environment variables:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mariadb://localhost:3306/discount_code"
$env:SPRING_DATASOURCE_USERNAME = "discount_code"
$env:SPRING_DATASOURCE_PASSWORD = "discount_code"
.\gradlew.bat bootRun
```

## Docker Image

Build the jar locally first:

```powershell
.\gradlew.bat bootJar
```

Then build the application image:

```powershell
docker compose build app
```

Run MariaDB and the app together:

```powershell
docker compose up -d
```

View container status and logs:

```powershell
docker compose ps
docker compose logs -f app
```

Stop the local stack:

```powershell
docker compose down
```

Remove the MariaDB volume as well:

```powershell
docker compose down -v
```
