# Helios — Secure Healthcare Appointment System (HelloDoctor)

A secure, containerized appointment management platform. Spring Boot REST API with JWT auth
and role-based access control, a React (Vite) frontend, MySQL, and asynchronous notifications.

## Tech stack

| Layer         | Technology                                   |
|---------------|----------------------------------------------|
| Backend       | Java 21, Spring Boot 3.4, Spring Security, JPA |
| Auth          | JWT (HMAC), BCrypt password hashing          |
| Database      | MySQL 8                                       |
| Frontend      | React 18, React Router, Vite, Axios          |
| Notifications | Async thread pool + retry (email/SMS)        |
| Deployment    | Docker + Docker Compose                       |

## Project layout

```
HelloDoctor/
├── backend/          Spring Boot API
├── frontend/         React SPA (Vite + Nginx)
├── docker-compose.yml
└── .env.example
```

## Roles & access

| Role    | Capabilities                                              |
|---------|-----------------------------------------------------------|
| PATIENT | Register/login, browse doctors, book & cancel appointments |
| DOCTOR  | View own appointments, approve/reject, mark complete       |
| ADMIN   | Manage users (enable/disable), view all appointments       |

A default admin is seeded on first run (configurable via env):
`admin@hellodoctor.local` / `Admin@12345` — **change this in production.**

### Sample data (seeded on first run)

When `SEED_SAMPLE_ENABLED=true` (default) and the database has no doctors yet, a set of demo
doctors, patients, and appointments is created. Disable with `SEED_SAMPLE_ENABLED=false`.

| Type    | Name             | Email                          | Password        | Specialty   |
|---------|------------------|--------------------------------|-----------------|-------------|
| Doctor  | Dr. Sarah Chen   | sarah.chen@hellodoctor.local   | `Doctor@12345`  | Cardiology  |
| Doctor  | Dr. Raj Patel    | raj.patel@hellodoctor.local    | `Doctor@12345`  | Dermatology |
| Doctor  | Dr. Emily Turner | emily.turner@hellodoctor.local | `Doctor@12345`  | Pediatrics  |
| Patient | John Doe         | john.doe@example.com           | `Patient@12345` | —           |
| Patient | Maria Garcia     | maria.garcia@example.com       | `Patient@12345` | —           |
| Patient | Liam Smith       | liam.smith@example.com         | `Patient@12345` | —           |

Six sample appointments are also created across these users, spanning PENDING, APPROVED,
COMPLETED, and CANCELLED statuses (mix of past and upcoming dates).

## Run with Docker (recommended)

Starts the database, backend, and frontend together.

```bash
cd HelloDoctor
cp .env.example .env      # optional: adjust secrets
docker compose up --build
```

- Frontend: http://localhost:8081
- API:      http://localhost:8080
- Health:   http://localhost:8080/actuator/health
- API docs: http://localhost:8080/swagger-ui.html (OpenAPI 3, JWT-aware)

Seeded admin login: `admin@hellodoctor.local` / `Admin@12345`.

Stop with `Ctrl+C`, or `docker compose down` (add `-v` to also wipe the database volume).

## Run locally (dev)

Use this for hot reload while developing.

**1. Start just the database** (or point the backend at your own MySQL):

```bash
cd HelloDoctor
docker compose up db
```

**2. Backend** (new terminal):

```bash
cd backend
DB_HOST=localhost DB_USERNAME=helios DB_PASSWORD=helios mvn spring-boot:run
```

**3. Frontend** (new terminal):

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxies /api to :8080)
```

## Inspecting the database

The MySQL container (`helios-db`) publishes port `3306`. Credentials come from your `.env`
(`helios` / `helios` / database `helios` by default).

**Open a MySQL shell inside the container:**

```bash
docker exec -it helios-db mysql -uhelios -phelios helios
```

Then run queries at the `mysql>` prompt:

```sql
SHOW TABLES;
SELECT id, full_name, email, role, phone FROM users;
SELECT id, patient_id, doctor_id, scheduled_at, status FROM appointments ORDER BY scheduled_at;
EXIT;
```

**Run one-off queries without entering the shell:**

```bash
docker exec -it helios-db mysql -uhelios -phelios helios -e "SELECT id, full_name, role FROM users;"
docker exec -it helios-db mysql -uhelios -phelios helios -e "SELECT status, COUNT(*) FROM appointments GROUP BY status;"
```

**Connect from a host `mysql` client** (port is published):

```bash
mysql -h 127.0.0.1 -P 3306 -uhelios -phelios helios
```

Quick checks that the seeded sample data landed:

```sql
SELECT role, COUNT(*) FROM users GROUP BY role;   -- ADMIN 1, DOCTOR 3, PATIENT 3
SELECT COUNT(*) FROM appointments;                -- 6
```

## Deploy on Kubernetes

Kubernetes manifests (Kustomize) live in [`k8s/`](k8s/) and deploy MySQL (StatefulSet), the
backend, and the frontend into a `helios` namespace. See [`k8s/README.md`](k8s/README.md) for the
full guide, operating commands, and troubleshooting.

```bash
# 1. Build the container images
docker build -t helios-backend:latest ./backend
docker build -t helios-frontend:latest ./frontend

# 2. Create a local cluster and load the images (kind)
kind create cluster --name helios --config k8s/kind-cluster.yaml
kind load docker-image helios-backend:latest helios-frontend:latest --name helios
#   minikube alternative:
#   minikube image load helios-backend:latest helios-frontend:latest

# 3. Deploy everything
kubectl apply -k k8s/

# 4. Watch it come up
kubectl -n helios get pods -w
#   expect: mysql-0 1/1, backend 2/2, frontend 2/2

# 5. Access the app (port-forward)
kubectl -n helios port-forward svc/frontend 8081:80   # → http://localhost:8081
#   admin@hellodoctor.local / Admin@12345
```

Tear down with `kubectl delete -k k8s/` (add `kubectl -n helios delete pvc --all` to wipe the DB).

## API summary

| Endpoint                       | Method | Auth            | Description                        |
|--------------------------------|--------|-----------------|------------------------------------|
| `/api/auth/register`           | POST   | Public          | Register (PATIENT/DOCTOR)          |
| `/api/auth/login`              | POST   | Public          | Login, returns JWT                 |
| `/api/doctors`                 | GET    | Authenticated   | List doctors                       |
| `/api/appointments`            | GET    | JWT             | List appointments (role-based)     |
| `/api/appointments`            | POST   | PATIENT         | Book appointment                   |
| `/api/appointments/{id}`       | PUT    | DOCTOR/ADMIN    | Approve/reject/update appointment  |
| `/api/appointments/{id}`       | DELETE | PATIENT/ADMIN   | Cancel appointment                 |
| `/api/users`                   | GET    | ADMIN           | List users                         |
| `/api/users/{id}/status`       | PUT    | ADMIN           | Enable/disable a user              |
| `/api/notifications`           | POST   | DOCTOR/ADMIN    | Send a notification (async)        |
| `/api/notifications`           | GET    | JWT             | List your notifications            |

## Security notes

- Passwords hashed with BCrypt; JWT signed with an HMAC secret (override `HELIOS_JWT_SECRET`).
- Stateless sessions; CORS restricted to configured origins.
- Errors return a consistent JSON shape with no stack traces or sensitive details.
- Self-registration cannot create ADMIN accounts (privilege-escalation guard).

## Notifications

Appointment events (request, approve, reject, cancel, complete) enqueue notifications that are
delivered asynchronously on a background thread pool with retry. With `MAIL_ENABLED=false`
(default) they are logged instead of sent, which is convenient for local development.

**Real email (SMTP):** set `MAIL_ENABLED=true` and provide `MAIL_HOST`, `MAIL_PORT`,
`MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, and `MAIL_FROM`.

**Real SMS (Twilio):** set `SMS_PROVIDER=twilio` and provide `TWILIO_ACCOUNT_SID`,
`TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER`. SMS is sent to the user's `phone` (captured
optionally at registration). The default `SMS_PROVIDER=log` just logs messages.

## Tests

```bash
cd backend && mvn test
```

Runs against an in-memory H2 database (MySQL-compatible mode): JWT unit tests plus MockMvc
integration tests covering the auth + booking flow and role-based access control.
