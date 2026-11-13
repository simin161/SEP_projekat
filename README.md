# SEP_projekat
Fakultetski projekat iz predmenta "Sistemi elektronskog placanja"

# SEP 2026/27: Online parking system and PSP

This repository holds the course project: an online parking system, a Payment Service Provider (PSP, a middleman that handles payments for other applications), payment plug-ins and a bank. The requirements are in `SEP - Specifikacija 2026_2027.pdf`. The team design is in the `.drawio` files in this folder.

## Repository layout

```
SEP_dizajn_sistema.drawio      system design (architecture, deployment, state machines, Kafka)
SEP_model_podataka.drawio      database models
SEP_dijagrami_sekvenci.drawio  sequence diagrams
backend/                       all backend services (this README, section "Backend")
```

## Backend

The backend is **one Maven project with one module per service**. The parent `backend/pom.xml` holds the dependencies that every service needs. Each module adds only its own.

| Module | Purpose | Port | Database | Language |
|---|---|---|---|---|
| `parking-service` | Parking: zones, vehicles, orders, sessions, notifications | 8081 | `parking_db` | Java |
| `psp-core` | PSP: merchant API, checkout, payment state machine, plug-in registry | 8082 | `psp_db` | Java |
| `card-qr-plugin` | Plug-in for card and IPS QR payments | 8083 | `card_db` | Java |
| `paypal-plugin` | Plug-in for PayPal | 8084 | `paypal_db` | Java |
| `crypto-plugin` | Plug-in for cryptocurrency | 8085 | `crypto_db` | Kotlin |
| `bank-service` | Bank (acquirer): payment page, fund reservation, IPS QR | 8086 | `bank_db` | Java |

The three plug-ins (`card-qr-plugin`, `paypal-plugin`, `crypto-plugin`) live together in `backend/plugins/`. That folder has its own `pom.xml`, which holds the dependencies all plug-ins share (Kafka and the REST client). Every other module is directly in `backend/`.

The modules come from `SEP_dizajn_sistema.drawio` (pages 1 and 2). The dashboards, the web client and the mobile bank are not part of this backend project.

### Requirements

- JDK 25 (the design asks for Java 25; Spring Boot 4.1 supports Java 17 up to 26, see [system requirements](https://docs.spring.io/spring-boot//4.1-SNAPSHOT/system-requirements.html))
- Maven 3.9 or newer
- Docker with Docker Compose

### Run locally

All commands run in the `backend/` folder. The Spring Boot services run on your machine. Docker runs only what they depend on: PostgreSQL, Kafka, Redis and Mailpit (a fake mail server that shows the e-mails in a web page).

1. Create the settings file. It holds the local passwords.

   ```
   cp .env.example .env          # PowerShell: Copy-Item .env.example .env
   ```

2. Start the infrastructure.

   ```
   docker compose up -d
   docker compose ps             # wait until postgres, kafka and redis are "healthy"
   ```

   | Service | Address |
   |---|---|
   | PostgreSQL | `localhost:5432` (one database and one user per service) |
   | Kafka | `localhost:9092` (topics `psp.attempt-results`, `psp.registry-events` are created automatically) |
   | Redis | `localhost:6379` |
   | Mailpit | SMTP `localhost:1025`, web page <http://localhost:8025> |

3. Build everything once.

   ```
   mvn clean package
   ```

4. Start a service. Use the `local` profile; it turns TLS off and uses the passwords from step 1. Each service needs its own terminal.

   ```
   cd psp-core
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

   A plug-in is started the same way from its folder, for example `cd plugins/card-qr-plugin`.

5. Check that it works: <http://localhost:8082/actuator/health> (use the port from the table). Expected answer: `{"status":"UP"}`. Every other URL answers 403 until a feature opens it.

Stop the infrastructure with `docker compose down`. To also delete all data, use `docker compose down -v`. The databases are created only when the data volume is empty, so after you change a password in `.env` you must delete the volume with `down -v`.

If you change the passwords in `.env`, export the same variables before you start a service (for example `PSP_DB_PASSWORD`), because the `local` profile falls back to the default values.

### Run and deploy one module on its own

A module does not need any other module to build or start. Build one module:

```
cd psp-core
mvn clean package             # creates target/psp-core.jar
```

Build its Docker image. Run this in `backend/`, because the parent `pom.xml` must be in the build context:

```
docker build -f psp-core/Dockerfile -t sep/psp-core .
docker build -f plugins/card-qr-plugin/Dockerfile -t sep/card-qr-plugin .   # a plug-in
```

The image runs as a normal user, not as root. Without the `local` profile the service needs TLS settings and the database password from the environment (next section). The dev Kafka from `docker-compose.yml` announces itself as `localhost`, so a container cannot reach it. Run containers against the dev infrastructure only after you set up a Docker network for it; this is not done yet.

### Configuration

Each module has its own files in `src/main/resources`:

- `application.yml`: the secure defaults. Used in every environment.
- `application-local.yml`: values for a developer machine only (no TLS, local passwords).
- `db/migration/`: the database migrations of this module (see below).

Settings that differ per environment are read from environment variables. The most important ones:

| Variable | Meaning |
|---|---|
| `<PREFIX>_DB_HOST`, `_DB_PORT`, `_DB_USER`, `_DB_PASSWORD` | Database of the module. Prefixes: `PARKING`, `PSP`, `CARD`, `PAYPAL`, `CRYPTO`, `BANK`. The password has no default. |
| `SERVER_PORT` | HTTP port of the module. |
| `SSL_KEY_STORE`, `SSL_KEY_STORE_PASSWORD` | PKCS12 file and password for TLS 1.3. Without them a module without the `local` profile does not start. |
| `SSL_CLIENT_AUTH` | `need` turns on mTLS (both sides show a certificate) for service-to-service calls. |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka address. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis (only `psp-core`). |
| `PSP_BASE_URL`, `PSP_MERCHANT_ID`, `PSP_API_SECRET`, `PSP_WEBHOOK_SECRET` | How `parking-service` reaches the PSP. |

Every key a module reads is in its `application.yml`.