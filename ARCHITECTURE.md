# Helios (HelloDoctor) — Architecture

A secure, full-stack healthcare appointment system: a stateless Spring Boot REST API with
JWT auth and role-based access control, a React (Vite) single-page app, MySQL persistence,
and asynchronous email/SMS notifications — all containerized with Docker Compose.

---

## 1. High-level system architecture

```mermaid
flowchart TB
    subgraph Client["Client Layer"]
        Browser["React SPA (Vite)<br/>Patient / Doctor / Admin dashboards"]
    end

    subgraph Edge["Delivery"]
        Nginx["Nginx<br/>Serves static build + proxies /api"]
    end

    subgraph Backend["Spring Boot REST API (port 8080)"]
        direction TB
        Filter["JWT Auth Filter<br/>+ Spring Security chain"]
        Controllers["Controllers<br/>Auth / Appointment / User / Notification"]
        Services["Services (business logic)<br/>Auth / Appointment / Notification"]
        Repos["Spring Data JPA Repositories"]
        Async["Async Notification Dispatcher<br/>(thread pool + retry)"]
        Senders["EmailSender / SmsSender<br/>(SMTP - Twilio - Log)"]
    end

    subgraph Data["Persistence"]
        MySQL[("MySQL 8<br/>users - appointments - notifications")]
    end

    subgraph External["External Providers"]
        SMTP["SMTP server"]
        Twilio["Twilio SMS API"]
    end

    Browser -->|HTTPS / REST + JWT| Nginx
    Nginx -->|/api| Filter
    Filter --> Controllers --> Services --> Repos --> MySQL
    Services --> Async --> Senders
    Senders --> SMTP
    Senders --> Twilio
```

---

## 2. Layered (clean) architecture inside the backend

```mermaid
flowchart LR
    A["Controller Layer<br/>HTTP, validation, DTO mapping"] --> B["Service Layer<br/>business rules, @Transactional"]
    B --> C["Repository Layer<br/>Spring Data JPA"]
    C --> D["Entity/Model Layer<br/>User, Appointment, Notification"]
    B -.->|DTOs| A
    E["Security<br/>JwtService, SecurityUser, Filter"] -.-> A
    F["Config<br/>SecurityConfig, OpenApi, DataInitializer"] -.-> B
```

Each layer has a single responsibility and only talks to the layer directly below it
(Controller → Service → Repository → Entity). This is the classic **layered / n-tier** pattern.

---

## 3. Authentication flow (JWT)

```mermaid
sequenceDiagram
    participant U as User (React)
    participant A as AuthController
    participant M as AuthenticationManager
    participant J as JwtService
    participant P as Protected API

    U->>A: POST /api/auth/login (email, password)
    A->>M: authenticate(credentials)
    M-->>A: authenticated (BCrypt verified)
    A->>J: generateToken(user)
    J-->>A: signed JWT (HMAC)
    A-->>U: { token, role, ... }
    U->>P: GET /api/appointments + Bearer token
    P->>J: validate token, extract user + role
    J-->>P: authenticated principal
    P-->>U: role-scoped data
```

---

## 4. Appointment booking flow (with async notifications)

```mermaid
sequenceDiagram
    participant Pt as Patient (React)
    participant C as AppointmentController
    participant S as AppointmentService
    participant R as AppointmentRepository
    participant N as NotificationService
    participant D as Async Dispatcher

    Pt->>C: POST /api/appointments (doctorId, time)
    C->>S: book(principal, request)
    S->>S: validate role, future time, slot conflict
    S->>R: save(appointment) [PENDING]
    R-->>S: saved
    S->>N: enqueue notifications (doctor + patient)
    S-->>C: AppointmentResponse (DTO)
    C-->>Pt: 201 Created
    Note over N,D: after DB commit
    N->>D: dispatch (async)
    D->>D: send via Email/SMS with retry
```

---

## 5. Domain model

```mermaid
erDiagram
    USER ||--o{ APPOINTMENT : "books (patient)"
    USER ||--o{ APPOINTMENT : "attends (doctor)"
    USER ||--o{ NOTIFICATION : receives

    USER {
        Long id
        String name
        String email
        String password "BCrypt"
        Role role "PATIENT/DOCTOR/ADMIN"
        UserStatus status
        String specialty
        String phone
    }
    APPOINTMENT {
        Long id
        Long patient_id
        Long doctor_id
        LocalDateTime scheduledAt
        AppointmentStatus status "PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED"
        String notes
    }
    NOTIFICATION {
        Long id
        Long userId
        String recipient
        NotificationType type "EMAIL/SMS"
        NotificationStatus status
        int attempts
    }
```

---

## 6. Tools & technologies

| Area | Technology | Why it's used |
|------|-----------|----------------|
| Language | Java 21 | LTS; records, switch expressions, sealed types |
| Framework | Spring Boot 3.4 | Auto-configuration, embedded Tomcat, production starters |
| Web | Spring MVC (REST) | Stateless JSON REST endpoints |
| Security | Spring Security + JWT (jjwt) | Stateless auth, role-based access control |
| Passwords | BCrypt | Salted one-way hashing |
| Data access | Spring Data JPA / Hibernate | ORM, repositories, `JOIN FETCH` for eager loading |
| Database | MySQL 8 | Relational integrity for users and appointments |
| Validation | Jakarta Bean Validation | `@Valid` request DTOs at the API boundary |
| Notifications | Spring `@Async` + JavaMailSender + Twilio REST | Non-blocking email/SMS with retry; pluggable |
| API docs | springdoc OpenAPI / Swagger UI | Self-documenting, JWT-aware API explorer |
| Monitoring | Spring Boot Actuator | Health/info endpoints |
| Frontend | React 18 + Vite | Component SPA with fast tooling |
| Routing/HTTP | React Router + Axios | Client routing; Axios interceptor attaches JWT |
| Build | Maven (backend), npm (frontend) | Dependency management, reproducible builds |
| Testing | JUnit 5 + Spring Test + MockMvc + H2 | Unit + integration tests on in-memory DB |
| Containerization | Docker + Docker Compose | One-command spin-up of db + backend + frontend |
| Web server | Nginx | Serves the SPA, reverse-proxies `/api` |

---

## 7. Key design decisions

- **Stateless JWT auth** — no server-side sessions, so the API scales horizontally. The token
  carries the user id + role; every request is independently authenticated by a filter.
- **Role-Based Access Control (RBAC)** — three roles enforced at the URL level (`SecurityConfig`)
  and method level. Only PATIENT can book, only ADMIN can list users. Self-registration cannot
  create an ADMIN (privilege-escalation guard).
- **Layered architecture + DTOs** — entities never leak to the API; responses use dedicated DTO
  records. Mapping happens inside the transaction (or via `JOIN FETCH`) to avoid
  `LazyInitializationException` with `open-in-view=false`.
- **Asynchronous notifications** — appointment events enqueue notifications delivered on a
  background thread pool with **retry**, firing only **after the DB transaction commits** (no race
  conditions). Providers are pluggable: log (dev), SMTP, or Twilio via config.
- **Externalized configuration** — all secrets/URLs come from environment variables (12-factor),
  so the same image runs in dev and prod.
- **Security hygiene** — BCrypt hashing, CORS restricted to known origins, CSRF disabled (safe for
  a stateless token API), and error responses never expose stack traces.
- **Testability** — integration tests run against in-memory H2 in MySQL mode, with
  `open-in-view=false` to catch persistence bugs early.

---

## 8. Request/response summary

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/register` | POST | Public | Register (PATIENT/DOCTOR) |
| `/api/auth/login` | POST | Public | Login, returns JWT |
| `/api/doctors` | GET | Authenticated | List doctors |
| `/api/appointments` | GET | JWT | List appointments (role-based) |
| `/api/appointments` | POST | PATIENT | Book appointment |
| `/api/appointments/{id}` | PUT | DOCTOR/ADMIN | Approve/reject/update |
| `/api/appointments/{id}` | DELETE | PATIENT/ADMIN | Cancel appointment |
| `/api/users` | GET | ADMIN | List users |
| `/api/users/{id}/status` | PUT | ADMIN | Enable/disable a user |
| `/api/notifications` | POST | DOCTOR/ADMIN | Send a notification (async) |
| `/api/notifications` | GET | JWT | List your notifications |

---

## 9. Elevator pitch

> Helios is a full-stack healthcare appointment system. The backend is a stateless Spring Boot
> REST API secured with JWT and role-based access control, backed by MySQL through Spring Data
> JPA. It follows a clean layered architecture — controllers, services, repositories — with DTOs
> at the boundary. Appointment events trigger asynchronous email/SMS notifications with retry
> through pluggable providers. The React + Vite frontend consumes the API with Axios, and the
> whole stack — database, backend, and Nginx-served frontend — runs with a single
> `docker compose up`. It's documented with Swagger and covered by JUnit/MockMvc integration tests.
