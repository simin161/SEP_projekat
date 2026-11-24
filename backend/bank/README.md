# Bank (acquirer)

The bank is the third system of the SEP 2026/27 project, next to the online parking system and the PSP (see `SEP - Specifikacija 2026_2027.pdf` in the repository root). It holds the payment page, reserves funds and answers status requests. It is its own Maven project: it builds, starts and deploys without the other two.

| Module | Purpose | Port | Database | Language |
|---|---|---|---|---|
| `bank-service` | Payment page, fund reservation, status by STAN, IPS QR generator and validator | 8086 | `bank_db` | Java |

The bank is called by the card plug-in of the PSP (`../psp/`). It reports payment results to the address in `PSP_CALLBACK_URL`.

## Requirements

- JDK 25 (Spring Boot 4.1 supports Java 17 up to 26, see [system requirements](https://docs.spring.io/spring-boot//4.1-SNAPSHOT/system-requirements.html))
- Maven 3.9 or newer
- Docker with Docker Compose

## Run locally

All commands run in this folder (`backend/bank/`). The service runs on your machine. Docker runs only its database.

1. Create the settings file. It holds the local passwords.

   ```
   cp .env.example .env          # PowerShell: Copy-Item .env.example .env
   ```

2. Start the database and wait until it is "healthy".

   ```
   docker compose up -d
   docker compose ps
   ```

   PostgreSQL listens on `localhost:5435`. The port differs from the other projects, so all three can run at the same time.

3. Build once.

   ```
   mvn clean package
   ```

4. Start the service with the `local` profile. It turns TLS off and uses the passwords from step 1.

   ```
   cd bank-service
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

5. Check it: <http://localhost:8086/actuator/health>. Expected answer: `{"status":"UP"}`. Every other URL answers 403 until a feature opens it.

Stop with `docker compose down`. To also delete the data, use `docker compose down -v`. The database is created only when the data volume is empty, so after you change a password in `.env` you must delete the volume with `down -v`. If you change the password, export the same variable before you start the service (`BANK_DB_PASSWORD`), because the `local` profile falls back to a default value.

## Build the Docker image

Run this in `backend/bank/`, because the parent `pom.xml` must be in the build context:

```
docker build -f bank-service/Dockerfile -t sep/bank-service .
```

The image runs as a normal user, not as root. Without the `local` profile the service needs TLS settings and the database password from the environment (next section).

## Configuration

Files in `bank-service/src/main/resources`:

- `application.yml`: the secure defaults, used in every environment.
- `application-local.yml`: values for a developer machine only (no TLS, local passwords, database on port 5435).
- `db/migration/`: the database migrations.

Settings that differ per environment are read from environment variables:

| Variable | Meaning |
|---|---|
| `BANK_DB_HOST`, `BANK_DB_PORT`, `BANK_DB_USER`, `BANK_DB_PASSWORD` | Database of the service. The password has no default. |
| `SERVER_PORT` | HTTP port. Default 8086. |
| `SSL_KEY_STORE`, `SSL_KEY_STORE_PASSWORD` | PKCS12 file and password for TLS 1.3. Without them the service does not start outside the `local` profile. |
| `SSL_CLIENT_AUTH` | `need` turns on mTLS (both sides show a certificate). |
| `PSP_CALLBACK_URL` | Where the bank sends payment results. Default `https://localhost:8083` (`http://localhost:8083` in the `local` profile). |
