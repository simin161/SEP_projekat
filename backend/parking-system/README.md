# Online parking system

The online parking system is one of the two main systems of the SEP 2026/27 project (see `SEP - Specifikacija 2026_2027.pdf` in the repository root). Users find a parking zone, start a session and pay for it. Every payment goes through the PSP (Payment Service Provider, the middleman that handles payments), which is a separate project in `../psp/`. This project builds, starts and deploys without the other projects.

| Module | Purpose | Port | Database | Language |
|---|---|---|---|---|
| `parking-service` | Users, zones, vehicles, orders, sessions, extensions, notifications | 8081 | `parking_db` | Java |

The web client is not part of this project.

## Requirements

- JDK 25 (Spring Boot 4.1 supports Java 17 up to 26, see [system requirements](https://docs.spring.io/spring-boot//4.1-SNAPSHOT/system-requirements.html))
- Maven 3.9 or newer
- Docker with Docker Compose

## Run locally

All commands run in this folder (`backend/parking-system/`). The service runs on your machine. Docker runs only what it depends on: PostgreSQL and Mailpit (a fake mail server that shows the e-mails in a web page).

1. Create the settings file. It holds the local passwords.

   ```
   cp .env.example .env          # PowerShell: Copy-Item .env.example .env
   ```

2. Start the infrastructure and wait until PostgreSQL is "healthy".

   ```
   docker compose up -d
   docker compose ps
   ```

   | Service | Address |
   |---|---|
   | PostgreSQL | `localhost:5433` (differs from the other projects, so all three can run at the same time) |
   | Mailpit | SMTP `localhost:1025`, web page <http://localhost:8025> |

3. Build once.

   ```
   mvn clean package
   ```

4. Start the service with the `local` profile. It turns TLS off and uses the passwords from step 1.

   ```
   cd parking-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

5. Check it: <http://localhost:8081/actuator/health>. Expected answer: `{"status":"UP"}`. Every other URL answers 403 until a feature opens it.

Paying needs the PSP. Start it from `../psp/` (the `local` profile expects it on `http://localhost:8082`).

Stop with `docker compose down`. To also delete the data, use `docker compose down -v`. The database is created only when the data volume is empty, so after you change a password in `.env` you must delete the volume with `down -v`. If you change the password, export the same variable before you start the service (`PARKING_DB_PASSWORD`), because the `local` profile falls back to a default value.

## Build the Docker image

Run this in `backend/parking-system/`, because the parent `pom.xml` must be in the build context:

```
docker build -f parking-service/Dockerfile -t sep/parking-service .
```

The image runs as a normal user, not as root. Without the `local` profile the service needs TLS settings and the database password from the environment (next section).

## Configuration

Files in `parking-service/src/main/resources`:

- `application.yml`: the secure defaults, used in every environment.
- `application-local.yml`: values for a developer machine only (no TLS, local passwords, database on port 5433, PSP on port 8082).
- `db/migration/`: the database migrations.

Settings that differ per environment are read from environment variables:

| Variable | Meaning |
|---|---|
| `PARKING_DB_HOST`, `PARKING_DB_PORT`, `PARKING_DB_USER`, `PARKING_DB_PASSWORD` | Database of the service. The password has no default. |
| `SERVER_PORT` | HTTP port. Default 8081. |
| `SSL_KEY_STORE`, `SSL_KEY_STORE_PASSWORD` | PKCS12 file and password for TLS 1.3. Without them the service does not start outside the `local` profile. |
| `SSL_CLIENT_AUTH` | `need` turns on mTLS (both sides show a certificate). |
| `SMTP_HOST`, `SMTP_PORT` | Mail server for notifications. Default `localhost:1025` (Mailpit). |
| `PSP_BASE_URL`, `PSP_MERCHANT_ID`, `PSP_API_SECRET`, `PSP_WEBHOOK_SECRET` | How the service reaches the PSP and proves who it is. |
