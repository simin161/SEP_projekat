# SEP_projekat
Fakultetski projekat iz predmenta "Sistemi elektronskog placanja"

# SEP 2026/27: Online parking system and PSP

This repository holds the course project: an online parking system, a Payment Service Provider (PSP, a middleman that handles payments for other applications), payment plug-ins and a bank. The requirements are in `SEP - Specifikacija 2026_2027.pdf`. The team design is in the `.drawio` files in this folder.

## Repository layout

```
SEP_dizajn_sistema.drawio      system design (architecture, deployment, state machines, Kafka)
SEP_model_podataka.drawio      database models
SEP_dijagrami_sekvenci.drawio  sequence diagrams
backend/
  parking-system/              online parking system
  psp/                         Payment Service Provider and its payment plug-ins
  bank/                        bank (acquirer)
```

## Backend

The backend is **three independent Maven projects**, as the specification describes three separate systems. Each project has its own `pom.xml`, `docker-compose.yml` (only the infrastructure it needs), `.env.example`, Dockerfiles and README. Build, run and deploy instructions are in those READMEs.

| Project | README | Services (port) |
|---|---|---|
| `backend/parking-system/` | [README](backend/parking-system/README.md) | `parking-service` (8081) |
| `backend/psp/` | [README](backend/psp/README.md) | `psp-core` (8082), `card-qr-plugin` (8083), `paypal-plugin` (8084), `crypto-plugin` (8085) |
| `backend/bank/` | [README](backend/bank/README.md) | `bank-service` (8086) |

The modules come from `SEP_dizajn_sistema.drawio` (pages 1 and 2). The dashboards, the web client and the mobile bank are not part of the backend.

### Run everything together

Each project starts its own infrastructure, on its own ports (no clashes):

| Infrastructure | Project | Address |
|---|---|---|
| PostgreSQL | `parking-system` | `localhost:5433` |
| PostgreSQL | `psp` | `localhost:5434` |
| PostgreSQL | `bank` | `localhost:5435` |
| Kafka, Redis | `psp` | `localhost:9092`, `localhost:6379` |
| Mailpit | `parking-system` | SMTP `localhost:1025`, web page <http://localhost:8025> |

Suggested start order: `bank`, then `psp` (infrastructure, `psp-core`, plug-ins), then `parking-system`. In the `local` profile the services find each other like this: parking to PSP on `http://localhost:8082`, card plug-in to bank on `http://localhost:8086`, bank to card plug-in (payment results) on `http://localhost:8083`.

In the IDE, open each project's `pom.xml` as its own Maven project.
