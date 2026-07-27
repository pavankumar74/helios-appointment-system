# Helios – Interview Preparation Guide

A structured Q&A to help you confidently discuss the **Helios – Secure Healthcare Appointment System**
(a.k.a. HelloDoctor) in interviews. Answers are grounded in the actual code you built.

> **Elevator pitch (memorize this):**
> "Helios is a full-stack healthcare appointment platform. Patients register, browse doctors, and
> book appointments; doctors approve or reject them; and admins oversee the whole system. The
> backend is a stateless Spring Boot REST API secured with JWT and role-based access control, backed
> by MySQL. The frontend is a React (Vite) SPA. Appointment events trigger asynchronous email/SMS
> notifications. Everything runs locally or via Docker Compose, and there's a CI pipeline plus a test
> suite covering the auth and booking flows."

---

## 1. Project Overview & Architecture

**Q: Give me a high-level overview of the project.**
Helios is a three-tier system:
- **Presentation:** React 18 + Vite SPA (Axios for HTTP, React Router for routing, a small AuthContext for session state).
- **Application/API:** Spring Boot 3.4 REST API — controllers → services → repositories, secured with Spring Security + JWT.
- **Data:** MySQL 8 in production, H2 (MySQL mode) for tests, accessed through Spring Data JPA/Hibernate.

Cross-cutting concerns: JWT authentication, role-based authorization (PATIENT/DOCTOR/ADMIN),
centralized exception handling, and asynchronous notifications (email via SMTP, SMS via Twilio or a
log stub).

**Q: Why did you choose this architecture (layered / controller-service-repository)?**
Separation of concerns and testability. Controllers only handle HTTP concerns (validation, status
codes); services hold business rules and transaction boundaries; repositories handle persistence.
This makes each layer independently testable and keeps business logic out of the web layer.

**Q: Walk me through what happens when a patient books an appointment.**
1. React sends `POST /api/appointments` with a JWT in the `Authorization: Bearer` header.
2. `JwtAuthenticationFilter` validates the token and sets the `SecurityContext`.
3. `AppointmentController` validates the request body (`@Valid`) and delegates to `AppointmentService.book()`.
4. The service enforces business rules: only PATIENT can book, the time must be in the future, the
   target must actually be a DOCTOR, and the slot must be free (`existsByDoctorIdAndScheduledAtAndStatusIn`).
5. It persists the appointment as `PENDING`, then enqueues notifications to both doctor and patient.
6. The response DTO is returned as JSON.

---

## 2. Backend / Spring Boot

**Q: How is the code organized?**
By technical layer under `com.hellodoctor.helios`: `controller`, `service`, `repository`, `model`
(JPA entities), `dto`, `security`, `config`, `notification`, and `exception`. DTOs isolate the API
contract from entities so I never leak the database model over the wire.

**Q: Why DTOs instead of returning entities directly?**
Three reasons: (1) security — I don't expose password hashes or internal fields; (2) stability — the
API contract can stay stable even if the entity changes; (3) avoiding serialization pitfalls with
lazy-loaded JPA relationships.

**Q: How do you handle validation?**
Jakarta Bean Validation annotations on the request DTOs (`@NotNull`, `@Email`, `@Size`, `@Future`),
triggered by `@Valid` in controllers. Validation failures are turned into a clean JSON error by the
global exception handler.

**Q: How is error handling centralized?**
A `@RestControllerAdvice` (`GlobalExceptionHandler`) maps custom exceptions to HTTP statuses and a
consistent `ApiError` JSON shape — e.g., `ResourceNotFoundException` → 404, `BadRequestException` →
400, `ConflictException` → 409, `ForbiddenActionException` → 403, validation errors → 400. No stack
traces or sensitive details ever reach the client.

**Q: How do you manage transactions?**
Service methods are annotated with `@Transactional`; read-only queries use
`@Transactional(readOnly = true)`. This keeps the unit of work at the service layer and gives
consistent commit/rollback semantics.

**Q: You mentioned you don't use Lombok — why, and what did you do instead?**
The environment runs a very new JDK where the Lombok annotation processor wasn't compatible, so
builds failed. Rather than fight the toolchain, I hand-wrote getters/setters and used a static
`Builder` inner class on the entities. It's more verbose but fully portable and dependency-free.

---

## 3. Security (this is the heart of the project — expect deep questions)

**Q: How does authentication work end-to-end?**
It's stateless JWT:
1. User logs in via `POST /api/auth/login`. `AuthService` authenticates the credentials through
   Spring Security's `AuthenticationManager` (`DaoAuthenticationProvider` + BCrypt).
2. On success, `JwtService.generateToken()` issues an HMAC-signed JWT containing the subject (email),
   `uid`, and `role` claims, with an expiry.
3. The client stores the token and sends it as `Authorization: Bearer <token>` on every request.
4. `JwtAuthenticationFilter` runs once per request, validates the signature/expiry, loads the user,
   and populates the `SecurityContext`. No server-side session is kept.

**Q: Why JWT / stateless sessions instead of server sessions?**
Scalability and simplicity: no session store, any instance can serve any request, which fits a
containerized, horizontally-scalable deployment. It also cleanly decouples the SPA from the backend.

**Q: How are passwords stored?**
Hashed with **BCrypt** (`BCryptPasswordEncoder`), which is adaptive and salted. I never store or log
plaintext passwords.

**Q: How is the JWT signed and validated?**
HMAC-SHA via jjwt (`Keys.hmacShaKeyFor`). The secret comes from config (`helios.security.jwt.secret`,
overridable by env var). The key must be at least 256 bits; my `decodeSecret` accepts a base64 secret
or falls back to raw bytes. Validation calls `Jwts.parser().verifyWith(key).parseSignedClaims()`, and
any `JwtException` (bad signature, expired, malformed) is caught and treated as invalid.

**Q: How does role-based authorization work?**
Two layers: (1) URL-level rules in `SecurityConfig` — e.g., `/api/users/**` requires `ROLE_ADMIN`,
`/api/auth/**` and Swagger/actuator health are public, everything else requires authentication; and
(2) business-level checks inside services — for example a DOCTOR can only update their **own**
appointments, and only the booking PATIENT or an ADMIN can cancel. `@EnableMethodSecurity` is on so
method-level annotations can also be used.

**Q: How do you handle CORS?**
A `CorsConfigurationSource` bean restricts origins to a configured allow-list
(`helios.security.cors.allowed-origins`), permits the needed methods/headers, and allows credentials.
It's environment-driven so dev and prod origins differ without code changes.

**Q: Why is CSRF disabled?**
Because the API is stateless and token-based (no cookies used for auth), the classic CSRF vector
doesn't apply — the JWT must be explicitly attached to each request. CSRF protection is mainly for
cookie-based session auth.

**Q: What happens on auth failure vs. authorization failure?**
`RestAuthErrorHandlers` provides a custom `AuthenticationEntryPoint` (401 for missing/invalid token)
and `AccessDeniedHandler` (403 for insufficient role), both returning consistent JSON instead of the
default HTML error pages.

**Q: What security best practices did you follow (OWASP)?**
BCrypt hashing, signed JWTs with expiry, least-privilege role checks at URL and service level,
input validation, no sensitive data in error responses, CORS allow-list, secrets via env vars (not
committed — `.env` is gitignored), and parameterized JPA queries (no string-concatenated SQL, so no
SQL injection).

---

## 4. Data Model & JPA

**Q: What are the core entities?**
`User` (with a `Role` enum: PATIENT/DOCTOR/ADMIN, plus a `UserStatus`), `Appointment` (with
`AppointmentStatus`: PENDING/APPROVED/REJECTED/CANCELLED/COMPLETED), and `Notification` (with
`NotificationType` EMAIL/SMS and `NotificationStatus`). An appointment has `@ManyToOne` links to a
patient `User` and a doctor `User`.

**Q: You hit a `LazyInitializationException` — what happened and how did you fix it?**
Appointment→patient/doctor are lazily loaded. When the admin listed all appointments, serialization
happened after the transaction/session closed, so accessing the lazy associations threw. I fixed it
two ways: I added `JOIN FETCH` queries in `AppointmentRepository` (`findAllWithParties`,
`findByIdWithParties`, and the per-user finders) to eagerly load both parties in one query, and I set
`spring.jpa.open-in-view=false`. Disabling open-in-view is a deliberate best practice — it surfaces
these problems at development time instead of silently holding DB connections during view rendering.

**Q: How do you prevent double-booking?**
Before saving, the service checks `existsByDoctorIdAndScheduledAtAndStatusIn(doctorId, time, [PENDING, APPROVED])`.
If an active appointment already occupies that slot, it throws a `ConflictException` (409). (In a
high-concurrency scenario I'd back this with a unique constraint or optimistic locking to close the
race window.)

**Q: How is the database schema created / seeded?**
Hibernate manages the schema (`ddl-auto`), and a `DataInitializer` (`CommandLineRunner`) seeds an
admin account and optional sample data (3 doctors, 3 patients, 6 appointments across statuses) on
first run — guarded by config flags and skipped if data already exists, so it's idempotent.

---

## 5. Asynchronous Notifications (a great "depth" topic)

**Q: How do notifications work?**
When appointment events occur, the service calls `NotificationService.enqueue()`, which persists a
`PENDING` notification and then hands delivery to an **async** `NotificationDispatcher`. Delivery goes
through a pluggable `EmailSender` (SMTP) or `SmsSender` (Twilio, or a logging stub for local dev),
and the notification's status is updated to SENT/FAILED.

**Q: There's a subtle transaction detail there — explain it.**
I dispatch **after commit**, not inline. `enqueue()` registers a `TransactionSynchronization` and
fires `dispatcher.dispatch(id)` in `afterCommit()`. This guarantees the background thread can't run
before the notification row is actually committed — otherwise the async thread might query a row that
isn't visible yet and find nothing. If no transaction is active, it dispatches immediately.

**Q: Why make delivery asynchronous?**
Sending email/SMS is slow and can fail; I don't want the user's booking request to block on an
external provider. Persisting first + async delivery means the API stays fast and the notification
record is the source of truth (retriable, auditable).

**Q: How is async configured?**
An `AsyncConfig` enables Spring's async support with a task executor; the dispatcher method runs on
that pool.

**Q: How did you make notification providers pluggable?**
`EmailSender` and `SmsSender` are interfaces. `SmsSender` has a `TwilioSmsSender` and a
`LoggingSmsSender`, selected by config (`helios.notifications.sms.provider`). This is the Strategy
pattern — I can swap providers without touching business logic, and local dev uses the log stub so no
real messages are sent.

---

## 6. Frontend / React

**Q: Describe the frontend.**
A Vite-powered React 18 SPA. `AuthContext` holds the JWT and current user; a shared Axios `client`
attaches the token and handles auth errors centrally; `ProtectedRoute` guards role-specific pages.
There are dedicated dashboards for Patient, Doctor, and Admin, plus Login/Register pages.

**Q: How does the frontend talk to the backend and handle auth?**
Axios base client with an interceptor that injects `Authorization: Bearer <token>`. In dev, Vite
proxies `/api` to `localhost:8080` to avoid CORS during development; in Docker the origins are
explicitly allow-listed.

**Q: How do you restrict pages by role?**
`ProtectedRoute` checks the authenticated user's role from `AuthContext` and redirects unauthorized
users. The real enforcement is still server-side — the frontend guard is just UX.

---

## 7. Testing

**Q: What does your test suite cover?**
Nine tests: a context-load smoke test, unit tests for `JwtService` (token generation/validation), and
MockMvc integration tests for the full auth + appointment flow (register, login, book, authorization
rules). They run against in-memory **H2 in MySQL mode**, with seeding disabled and a deterministic
JWT secret so tests are hermetic and repeatable.

**Q: Why H2 for tests instead of MySQL?**
Speed and isolation — no external DB needed, tests run anywhere including CI. MySQL-compatibility mode
keeps the SQL dialect close enough to production.

**Q: How would you improve test coverage?**
Add service-layer unit tests with Mockito for edge cases (double-booking, forbidden actions), add
Testcontainers to run integration tests against real MySQL, and add frontend tests (React Testing
Library) plus an end-to-end test (Playwright/Cypress).

---

## 8. DevOps / Build / CI

**Q: How is the app packaged and run?**
Docker Compose orchestrates three services: MySQL, the Spring Boot backend (port 8080), and the React
frontend (served on 8081). Config is entirely env-driven (`.env`), and `.env.example` documents the
variables. Locally you can also run the backend with `mvn spring-boot:run` and the frontend with
`npm run dev`.

**Q: Describe your CI pipeline.**
A GitHub Actions workflow runs on push/PR to `main` with two jobs: a **backend** job (Temurin JDK 21,
`mvn verify` to compile + run tests, uploads surefire reports) and a **frontend** job (Node 20,
`npm ci` + `npm run build`). It gives fast feedback that both halves build and tests pass.

**Q: How do you keep secrets out of the repo?**
`.env` is gitignored; only `.env.example` (with placeholders) is committed. All secrets — DB password,
JWT secret, Twilio/SMTP creds — come from environment variables, overridable per environment.

**Q: How is API documentation handled?**
springdoc-openapi generates an OpenAPI spec and Swagger UI at `/swagger-ui.html`, so the API is
self-documenting and easy to explore/test manually.

---

## 9. Design Decisions & Trade-offs (be ready to defend these)

| Decision | Why | Trade-off / what I'd change at scale |
|----------|-----|--------------------------------------|
| Stateless JWT | Scalable, no session store | No easy server-side revocation → add short expiry + refresh tokens / denylist |
| Slot check via query | Simple, readable | Race window under high concurrency → DB unique constraint / optimistic lock |
| Async notifications in-process | Simple, no infra | Lost on crash → move to a durable queue (RabbitMQ/Kafka/outbox) |
| Hibernate `ddl-auto` | Fast iteration | Risky for prod → use Flyway/Liquibase migrations |
| DTOs by hand | Explicit, no magic | Boilerplate → MapStruct |
| No Lombok | JDK-compat, portable | Verbose entities |

---

## 10. Likely "Scaling & Beyond" Questions

**Q: How would you scale this to thousands of users?**
Run multiple stateless backend instances behind a load balancer (JWT makes this trivial), add
read replicas / connection pooling for MySQL, cache doctor directory lookups (Redis), move
notifications to a durable message queue with workers, and put the frontend behind a CDN.

**Q: How would you add appointment reminders (e.g., 24h before)?**
A scheduled job (`@Scheduled`) or a queue with delayed delivery scans upcoming appointments and
enqueues reminder notifications — reusing the existing notification pipeline.

**Q: How would you handle token revocation / logout?**
Short-lived access tokens + refresh tokens, and a server-side denylist (Redis) for immediate
revocation when needed.

**Q: What would you add for production readiness?**
Flyway migrations, structured logging + correlation IDs, metrics/tracing (Actuator + Prometheus/
Grafana), rate limiting on auth endpoints, refresh tokens, and Testcontainers-based integration tests.

**Q: What was the hardest problem you solved?**
The `LazyInitializationException` on the admin view. It pushed me to understand JPA fetching, the
open-session-in-view anti-pattern, and how to fetch associations deliberately with `JOIN FETCH` — and
the after-commit async dispatch, which taught me how transaction visibility interacts with async
threads.

---

## 11. Rapid-Fire (know these cold)

- **Spring Boot version:** 3.4.x, Java 21 target.
- **Auth:** JWT (jjwt), HMAC-signed, stateless, BCrypt passwords.
- **Roles:** PATIENT, DOCTOR, ADMIN.
- **DB:** MySQL 8 (prod), H2 (tests), Spring Data JPA/Hibernate.
- **Frontend:** React 18 + Vite, Axios, React Router.
- **Notifications:** async, EMAIL (SMTP) / SMS (Twilio or log), persisted with status.
- **Docs:** OpenAPI/Swagger UI.
- **CI:** GitHub Actions (backend `mvn verify`, frontend `npm ci && build`).
- **Deploy:** Docker Compose (db + backend + frontend).
- **Key patterns:** layered architecture, DTO, Strategy (senders), Builder (entities).
