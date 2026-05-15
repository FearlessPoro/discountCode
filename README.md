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

The local database is intentionally simple to set up:

1. Docker Compose starts MariaDB.
2. The Spring Boot app connects to it.
3. Flyway runs the SQL migrations from `src/main/resources/db/migration`.
4. The schema is ready before the app finishes startup.

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

Run the app once to apply Flyway migrations:

```powershell
.\gradlew.bat bootRun
```

On a fresh database, startup logs should show Flyway validating and applying the migrations:

```text
Migrating schema `discount_code` to version "1 - create coupons"
Migrating schema `discount_code` to version "2 - create coupon usages"
Successfully applied 2 migrations
```

You can verify the tables directly through the MariaDB container:

```powershell
docker compose exec mariadb mariadb -udiscount_code -pdiscount_code discount_code -e "SHOW TABLES;"
```

Expected application tables:

```text
coupons
coupon_usages
flyway_schema_history
```

You can seed repeatable manual demo coupons after the app has run once and Flyway has created the schema:

```powershell
.\scripts\seed-demo-data.ps1
```

The script connects to the MariaDB service from Docker Compose and upserts these coupons without adding any production Flyway migration data:

```text
PLTEST  country PL  max uses 2
USTEST  country US  max uses 1
LIMIT1  country US  max uses 1
```

Use `user-1`, `user-2`, and `user-3` as demo `userId` values in redemption requests. Re-running the script resets usage rows for those demo coupons so manual demos can start from a known state. Automated tests create their own data and do not depend on this script.

Flyway owns schema creation and evolution. Do not create or change application tables manually for normal development; add a new migration instead. Coupon redemption audit rows store `user_id`, `ip_address`, and `resolved_country_code`; production deployments should define retention and deletion rules for those fields.

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

The production-like GeoIP mode is enabled by default:

```properties
app.geoip.provider=ip-api
```

This calls the free `ip-api.com` HTTP endpoint. That keeps setup simple for the assignment, but production deployments should use an HTTPS provider, a paid `ip-api.com` plan, another trusted GeoIP service, or a local GeoIP database. Local and test runs can use the deterministic stub resolver instead:

```powershell
$env:APP_GEOIP_PROVIDER = "stub"
.\gradlew.bat bootRun
```

Client IP resolution trusts the first value in `X-Forwarded-For` and falls back to the request remote address. That is only safe when requests arrive through trusted infrastructure such as an owned frontend, reverse proxy, or load balancer. Public direct-to-backend traffic can forge this header; a production version should make this explicit with a feature flag such as `app.client-ip.trust-forwarded-headers=true`.

Optionally check the real GeoIP provider from PowerShell:

```powershell
.\scripts\check-geoip.ps1 -IpAddress 8.8.8.8
```

## Coupon API

Create a coupon:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/coupons `
  -ContentType "application/json" `
  -Body '{"code":"save10","maxUses":10,"countryCode":"pl"}'
```

The service stores coupon codes and country codes normalized to uppercase. Coupon codes must be 1-50 ASCII letters or digits; creation rejects spaces instead of trimming them. `maxUses` must be greater than zero, and `countryCode` must be a valid ISO 3166-1 alpha-2 code.

Fetch a coupon for manual verification:

```powershell
Invoke-RestMethod http://localhost:8080/api/coupons/save10
```

`POST /api/coupons` returns `201 Created` with the stored coupon. Invalid input returns a structured `400 Bad Request`, duplicate coupon codes return `409 Conflict`, missing coupon lookups return `404 Not Found`, and unexpected failures return a vague structured `500` response. Authentication is intentionally out of scope for this version, but coupon creation must be protected before production use.

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
