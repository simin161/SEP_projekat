# Payment Service Provider (PSP)

The PSP is one of the two main systems of the SEP 2026/27 project (see `SEP - Specifikacija 2026_2027.pdf` in the repository root). It is a middleman: applications such as the parking system send it payment requests, and it handles the payment with the bank, PayPal or a cryptocurrency network. This project holds the PSP core and its payment plug-ins. It builds, starts and deploys without the other projects.

| Module | Purpose | Port | Database | Language |
|---|---|---|---|---|
| `psp-core` | Merchant API, checkout, payment state machine, plug-in registry, webhooks | 8082 | `psp_db` | Java |
| `card-qr-plugin` | Plug-in for card and IPS QR payments (talks to the bank) | 8083 | `card_db` | Java |
| `paypal-plugin` | Plug-in for PayPal | 8084 | `paypal_db` | Java |
| `crypto-plugin` | Plug-in for cryptocurrency | 8085 | `crypto_db` | Kotlin |

The three plug-ins live together in `plugins/`. That folder has its own `pom.xml` for the dependencies all plug-ins share (Kafka and the REST client). Each plug-in is still its own service. The team design (`SEP_dizajn_sistema.drawio`, page 1) shows the plug-ins as part of the PSP. The dashboards (client and admin portal) are not part of this project.

## Requirements

- JDK 25 (Spring Boot 4.1 supports Java 17 up to 26, see [system requirements](https://docs.spring.io/spring-boot//4.1-SNAPSHOT/system-requirements.html))
- Maven 3.9 or newer
- Docker with Docker Compose

## Run locally

All commands run in this folder (`backend/psp/`). The Spring Boot services run on your machine. Docker runs only what they depend on: PostgreSQL, Kafka and Redis.

1. Create the settings file. It holds the local passwords.

   ```
   cp .env.example .env          # PowerShell: Copy-Item .env.example .env
   ```

2. Start the infrastructure and wait until postgres, kafka and redis are "healthy".

   ```
   docker compose up -d
   docker compose ps
   ```

   | Service | Address |
   |---|---|
   | PostgreSQL | `localhost:5434` (one database and one user per service; differs from the other projects, so all three can run at the same time) |
   | Kafka | `localhost:9092` (topics `psp.attempt-results`, `psp.registry-events` are created automatically) |
   | Redis | `localhost:6379` |

3. Build everything once.

   ```
   mvn clean package
   ```

4. Start a service with the `local` profile. It turns TLS off and uses the passwords from step 1. Each service needs its own terminal.

   ```
   cd psp-core
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   A plug-in is started the same way from its folder, for example `cd plugins/card-qr-plugin`.

5. Check it: <http://localhost:8082/actuator/health> (use the port from the table). Expected answer: `{"status":"UP"}`. Every other URL answers 403 until a feature opens it.

The card plug-in talks to the bank (`../bank/`, `http://localhost:8086` in the `local` profile). The parking system (`../parking-system/`) calls `psp-core`.

Stop with `docker compose down`. To also delete the data, use `docker compose down -v`. The databases are created only when the data volume is empty, so after you change a password in `.env` you must delete the volume with `down -v`. If you change the passwords, export the same variables before you start a service (for example `PSP_DB_PASSWORD`), because the `local` profile falls back to default values.

## Run and deploy one module on its own

A module does not need any other module to build or start. Build one module:

```
cd psp-core
mvn clean package             # creates target/psp-core.jar
```

Build its Docker image. Run this in `backend/psp/`, because the parent `pom.xml` must be in the build context:

```
docker build -f psp-core/Dockerfile -t sep/psp-core .
docker build -f plugins/card-qr-plugin/Dockerfile -t sep/card-qr-plugin .   # a plug-in
```

The image runs as a normal user, not as root. Without the `local` profile the service needs TLS settings and the database password from the environment (next section). The dev Kafka from `docker-compose.yml` announces itself as `localhost`, so a container cannot reach it. Run containers against the dev infrastructure only after you set up a Docker network for it; this is not done yet.

## Configuration

Each module has its own files in `src/main/resources`:

- `application.yml`: the secure defaults, used in every environment.
- `application-local.yml`: values for a developer machine only (no TLS, local passwords, database on port 5434).
- `db/migration/`: the database migrations of this module.

Settings that differ per environment are read from environment variables:

| Variable | Meaning |
|---|---|
| `<PREFIX>_DB_HOST`, `_DB_PORT`, `_DB_USER`, `_DB_PASSWORD` | Database of the module. Prefixes: `PSP`, `CARD`, `PAYPAL`, `CRYPTO`. The password has no default. |
| `SERVER_PORT` | HTTP port of the module. |
| `SSL_KEY_STORE`, `SSL_KEY_STORE_PASSWORD` | PKCS12 file and password for TLS 1.3. Without them a module without the `local` profile does not start. |
| `SSL_CLIENT_AUTH` | `need` turns on mTLS (both sides show a certificate) for service-to-service calls. |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka address. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis (only `psp-core`). |
| `BANK_BASE_URL`, `BANK_MERCHANT_ID`, `BANK_API_SECRET` | How `card-qr-plugin` reaches the bank. |
| `PAYPAL_BASE_URL` | PayPal API address for `paypal-plugin` (default: the PayPal sandbox). |

Every key a module reads is in its `application.yml`.
