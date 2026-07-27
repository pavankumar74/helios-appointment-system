# Helios – 1-Page Cheat Sheet

**Elevator pitch:** Full-stack healthcare appointment platform. Patients book, doctors approve,
admins oversee. Stateless Spring Boot REST API secured with JWT + role-based access, MySQL data,
React (Vite) SPA, async email/SMS notifications, Dockerized with CI.

---

### Stack
- **Backend:** Spring Boot 3.4, Java 21, Spring Security, Spring Data JPA/Hibernate
- **Auth:** JWT (jjwt, HMAC-signed), BCrypt passwords, stateless
- **DB:** MySQL 8 (prod) / H2 MySQL-mode (tests)
- **Frontend:** React 18 + Vite, Axios, React Router
- **Notifications:** async — SMTP email / Twilio SMS (log stub for dev)
- **Docs:** OpenAPI + Swagger UI (`/swagger-ui.html`)
- **DevOps:** Docker Compose (db + backend:8080 + frontend:8081), GitHub Actions CI

### Roles
`PATIENT` (book/cancel own) · `DOCTOR` (approve/reject own) · `ADMIN` (full oversight, `/api/users/**`)

### Appointment statuses
`PENDING → APPROVED / REJECTED / CANCELLED / COMPLETED`

---

### Request flow (memorize)
`React → Axios (Bearer JWT) → JwtAuthenticationFilter → Controller (@Valid) → Service (rules + @Transactional) → Repository → MySQL → DTO response`

### Layers
Controller (HTTP) → Service (business logic + transactions) → Repository (JPA) → Entity. DTOs isolate API from entities.

---

### Security talking points
- **JWT stateless** → scales horizontally, no session store
- **BCrypt** salted/adaptive password hashing
- **RBAC in 2 layers:** URL rules in `SecurityConfig` + business checks in services (own-resource only)
- **CSRF disabled** because token-based (no auth cookies)
- **CORS** origin allow-list, env-driven
- **Custom 401/403 handlers** return JSON, not HTML
- **OWASP:** input validation, no secrets/stack traces leaked, parameterized JPA queries, secrets via env

### Three "depth" stories
1. **LazyInitializationException fix** → `JOIN FETCH` queries + `open-in-view=false` (surface issues early)
2. **Async notify after-commit** → `TransactionSynchronization.afterCommit()` so the async thread sees committed data
3. **Double-booking prevention** → `existsByDoctorIdAndScheduledAtAndStatusIn([PENDING,APPROVED])` → 409 Conflict

---

### Testing
9 tests: context load + `JwtService` unit tests + MockMvc auth/booking flow. H2 in-memory, seeding off, deterministic secret.

### Design decisions (trade-offs)
JWT (no revocation → refresh tokens) · slot query (race → unique constraint) · in-process async (durability → queue) · `ddl-auto` (prod → Flyway) · hand-written entities (no Lombok, JDK compat)

### Scaling answers
LB + multiple stateless instances · MySQL read replicas + pooling · Redis cache · durable queue for notifications · CDN for frontend · refresh tokens + Redis denylist for revocation

---

### Rapid-fire facts
- Package: `com.hellodoctor.helios`
- No Lombok (JDK compatibility) — hand-written getters/setters + Builder
- Key endpoints: `/api/auth/{register,login}`, `/api/doctors`, `/api/appointments`, `/api/users` (admin), `/api/notifications`
- Patterns: Layered, DTO, Strategy (senders), Builder (entities)
- CI: backend `mvn verify` (JDK 21) + frontend `npm ci && build` (Node 20)
