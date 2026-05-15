# Discount Code Service

Spring Boot REST service for creating and redeeming discount coupons. It enforces case-insensitive coupon code uniqueness, maximum usage counts, one redemption per user, and country restrictions based on the caller IP address.

Authentication is intentionally out of scope for this assignment version. A production deployment must protect coupon creation and any operational endpoints.

## Technology Stack

- Java 21
- Spring Boot 4
- Gradle wrapper
- Spring Web MVC, Validation, Data JPA
- MariaDB 11.4
- Flyway migrations
- Docker Compose
- JUnit with Spring Boot Test and H2 for automated tests

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

## Run Locally

Start MariaDB:

```powershell
docker compose up -d mariadb
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

Start the app:

```powershell
.\gradlew.bat bootRun
```

The app listens on port `8080`. On startup, Flyway applies migrations from `src/main/resources/db/migration`; the expected application tables are `coupons`, `coupon_usages`, and `flyway_schema_history`.

Verify the tables directly:

```powershell
docker compose exec mariadb mariadb -udiscount_code -pdiscount_code discount_code -e "SHOW TABLES;"
```

Datasource values can be overridden with environment variables:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mariadb://localhost:3306/discount_code"
$env:SPRING_DATASOURCE_USERNAME = "discount_code"
$env:SPRING_DATASOURCE_PASSWORD = "discount_code"
.\gradlew.bat bootRun
```

Flyway owns schema creation and evolution. Do not create or change application tables manually for normal development; add a new migration instead.

## Demo Data

After the app has run once and Flyway has created the schema, seed repeatable manual demo coupons:

```powershell
.\scripts\seed-demo-data.ps1
```

The script upserts these coupons without adding production migration data:

```text
PLTEST  country PL  max uses 2
USTEST  country US  max uses 1
LIMIT1  country US  max uses 1
```

Use `user-1`, `user-2`, and `user-3` as demo `userId` values in redemption requests. Re-running the script resets usage rows for those demo coupons so manual demos can start from a known state. Automated tests create their own data and do not depend on this script.

## GeoIP Modes

The default GeoIP provider is production-like:

```properties
app.geoip.provider=ip-api
```

This calls the free `ip-api.com` HTTP endpoint with a two-second timeout. That keeps setup simple for the assignment, but production deployments should use HTTPS through a paid `ip-api.com` service tier, another trusted GeoIP service, or a local GeoIP database.

Use the deterministic stub resolver for local demos and tests when you do not want external GeoIP calls:

```powershell
.\gradlew.bat bootRun --args="--app.geoip.provider=stub --app.geoip.stub.default-country-code=PL"
```

You can also map specific IP addresses in stub mode:

```powershell
.\gradlew.bat bootRun --args="--app.geoip.provider=stub --app.geoip.stub.country-by-ip[203.0.113.10]=US --app.geoip.stub.default-country-code=PL"
```

Optional real-provider check:

```powershell
.\scripts\check-geoip.ps1 -IpAddress 8.8.8.8
```

GeoIP behavior:

- Private/local/unrecognized IPs, missing provider country codes, or malformed country codes return `403 COUNTRY_NOT_VERIFIED`.
- Provider outages, timeouts, and dependency failures return `503 GEOIP_DEPENDENCY_UNAVAILABLE`.
- Stub mode does not call external services; behavior comes from the configured IP map or default country.

Client IP resolution trusts the first value in `X-Forwarded-For` and falls back to the request remote address. This is only safe when traffic arrives through trusted infrastructure such as an owned frontend, reverse proxy, or load balancer. Public direct-to-backend traffic can forge this header; a production version should make this explicit with a feature flag such as `app.client-ip.trust-forwarded-headers=true`.

## API Examples

Create a coupon:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/coupons `
  -ContentType "application/json" `
  -Body '{"code":"save10","maxUses":10,"countryCode":"pl"}'
```

Fetch a coupon for manual verification:

```powershell
Invoke-RestMethod http://localhost:8080/api/coupons/save10
```

Redeem a coupon:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/coupons/save10/redeem `
  -Headers @{ "X-Forwarded-For" = "203.0.113.10" } `
  -ContentType "application/json" `
  -Body '{"userId":"user-1"}'
```

Create response:

```json
{
  "code": "SAVE10",
  "createdAt": "2026-05-15T12:00:00Z",
  "maxUses": 10,
  "currentUses": 0,
  "countryCode": "PL"
}
```

Redeem response:

```json
{
  "couponCode": "SAVE10",
  "userId": "user-1",
  "redeemedAt": "2026-05-15T12:01:00Z",
  "status": "REDEEMED"
}
```

Rules:

- Coupon codes and country codes are stored uppercase.
- Coupon codes must be 1-50 ASCII letters or digits.
- Coupon creation rejects spaces instead of trimming them.
- Coupon lookup and redemption normalize the path code after trimming surrounding whitespace.
- `maxUses` must be greater than zero.
- `countryCode` must be a valid ISO 3166-1 alpha-2 country code.
- `userId` must be 1-100 characters.
- Request bodies never accept explicit IP addresses.
- `currentUses` is server-controlled and is not accepted from API input.
- Redeem responses do not expose `currentUses`.

## Error Format

Expected errors use this shape:

```json
{
  "code": "COUPON_NOT_FOUND",
  "message": "Coupon not found."
}
```

Status codes:

| Scenario | Status | Code |
|---|---:|---|
| Invalid JSON or validation failure | `400` | `INVALID_REQUEST` |
| Coupon not found | `404` | `COUPON_NOT_FOUND` |
| Duplicate coupon code | `409` | `DUPLICATE_COUPON_CODE` |
| Country cannot be verified | `403` | `COUNTRY_NOT_VERIFIED` |
| Coupon country mismatch | `403` | `COUPON_COUNTRY_MISMATCH` |
| Usage limit reached | `403` | `COUPON_USAGE_LIMIT_REACHED` |
| Same user redeems the same coupon again | `403` | `COUPON_ALREADY_REDEEMED` |
| GeoIP dependency unavailable | `503` | `GEOIP_DEPENDENCY_UNAVAILABLE` |
| Unexpected failure | `500` | `INTERNAL_ERROR` |

Unexpected errors intentionally return a vague message and should not leak implementation details.

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

Tests use the stub GeoIP resolver from `src/test/resources/application.properties`.

## Docker Image

Build the jar locally first:

```powershell
.\gradlew.bat bootJar
```

Build the application image:

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

## Concurrency And Scalability

Multiple app instances can run because durable state lives in MariaDB. Database constraints protect key invariants: coupon code uniqueness, one usage row per coupon/user pair, valid usage counts, and valid stored country code shapes.

Redemption uses pessimistic locking on the coupon row before incrementing `currentUses`. This keeps max-use enforcement correct under concurrent redemption attempts, but a very hot coupon can bottleneck on that row lock.

Future high-volume options include:

- Atomic conditional update for remaining-use checks.
- Coupon slot table with one redeemable row per available use.
- Redis/Lua front gate with database reconciliation.
- Provider-specific queueing or reservation strategy for campaign traffic spikes.

If the data model is sharded later, prefer `coupon_id` or normalized `coupon_code` as the shard key. Sharding by `user_id` would spread a single coupon's usage counter across shards and make max-use enforcement distributed and harder.

## Logging And Data

Coupon usage rows store `user_id`, `ip_address`, and `resolved_country_code` for auditability. This first version keeps usage rows indefinitely; production systems should define retention and deletion rules for `userId`, `ipAddress`, and `resolvedCountryCode`.

Operational logs should cover startup/configuration, coupon creation success and failure, redemption success and failure, GeoIP dependency failures, and unexpected exceptions. Logs should prefer error codes, counts, correlation/request IDs, and masked or hashed identifiers. Avoid raw IP addresses, raw `userId` values, full failed coupon codes, and provider response bodies where possible.

