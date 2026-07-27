# Helios – Behavioral & STAR Questions

Use the **STAR** method: **S**ituation → **T**ask → **A**ction → **R**esult. Below are ready-to-use
answers based on real work you did on this project. Adapt the wording to sound natural, and always
end on a measurable or concrete **Result**.

> Tip: Interviewers care less about the tech and more about *how you think, decide, and recover from
> problems*. Every story below is designed to show ownership, judgment, and learning.

---

## 1. "Tell me about a challenging technical problem you solved."

- **Situation:** In Helios, the admin dashboard needed to list every appointment with its patient and
  doctor. When I called that endpoint, it crashed with a 500 error.
- **Task:** Diagnose and fix it without degrading performance or leaking the fix into other layers.
- **Action:** I traced it to a `LazyInitializationException` — the appointment's patient/doctor
  associations were lazily loaded, and I was accessing them after the Hibernate session had closed
  during JSON serialization. I fixed it deliberately: I wrote `JOIN FETCH` repository queries to load
  both parties in a single query, and I disabled open-session-in-view (`open-in-view=false`) so this
  class of bug surfaces at development time instead of silently holding DB connections.
- **Result:** The admin view worked correctly and efficiently (no N+1 queries), and I came away with a
  much deeper understanding of JPA fetching strategies and why open-in-view is considered an
  anti-pattern.

---

## 2. "Tell me about a time you had to make a design trade-off."

- **Situation:** Appointment events needed to send email/SMS notifications, but calling an external
  provider inline would make the booking API slow and fragile.
- **Task:** Deliver notifications reliably without blocking the user's request.
- **Action:** I designed the notifications to be **persisted first as PENDING, then delivered
  asynchronously** on a background thread. I also handled a subtle correctness issue — I dispatch only
  *after* the database transaction commits (via a `TransactionSynchronization`), so the async worker
  can never query a notification row that isn't visible yet. I made the senders pluggable behind
  interfaces (Strategy pattern) with a logging stub for local dev.
- **Result:** Booking stayed fast and resilient to provider outages, and the notification record
  became the source of truth — auditable and retriable. I explicitly noted the trade-off (in-process
  async isn't crash-durable) and documented that a durable queue would be the next step at scale.

---

## 3. "Tell me about a time you dealt with an unexpected blocker / constraint."

- **Situation:** My build kept failing because the Lombok annotation processor wasn't compatible with
  the very new JDK in my environment.
- **Task:** Keep the project building without getting stuck fighting the toolchain.
- **Action:** Rather than downgrade the JDK or spend hours on workarounds, I made a pragmatic call to
  remove Lombok entirely and hand-write the getters/setters plus a static `Builder` inner class on the
  entities.
- **Result:** The build became fully portable and dependency-free, and I never lost momentum on
  feature work. It reinforced a habit of choosing the simplest reliable path over the "clever" one.

---

## 4. "Tell me about a time you prioritized security."

- **Situation:** Helios handles healthcare data, so authentication and authorization had to be solid.
- **Task:** Build a secure, role-aware API following industry best practices.
- **Action:** I implemented stateless JWT auth with HMAC-signed tokens and BCrypt-hashed passwords,
  enforced role-based access at two layers (URL rules plus per-resource ownership checks in services),
  restricted CORS to an allow-list, returned sanitized JSON errors with no stack traces, kept all
  secrets in environment variables (gitignored `.env`), and relied on parameterized JPA queries to
  avoid SQL injection.
- **Result:** The API defends against the common OWASP risks — e.g., a doctor literally cannot read or
  modify another doctor's appointments, and no sensitive data leaks through errors.

---

## 5. "Tell me about a time you handled a mistake or something going wrong."

- **Situation:** After pushing my repo to GitHub, I realized the commits were attributed to my company
  email instead of my personal identity.
- **Task:** Correct the authorship cleanly on a personal, public repo.
- **Action:** I set the correct local identity, rewrote the commit history to reattribute author and
  committer, force-pushed to replace the old commits, and set up a conditional git config so repos in
  my personal folder always use the right email going forward — preventing a repeat.
- **Result:** The history was clean and the root cause was fixed permanently, not just patched. It's a
  small example of my habit of fixing the underlying cause, not just the symptom.

---

## 6. "How do you ensure code quality?"

- **Action/Result:** Clear layering (controller/service/repository) so responsibilities are isolated
  and testable; DTOs to keep a stable, safe API contract; centralized validation and exception
  handling for consistency; a test suite (unit + MockMvc integration) running on in-memory H2; and a
  CI pipeline that builds and tests both backend and frontend on every push so regressions are caught
  immediately.

---

## 7. "Why did you build this? / What did you learn?"

- **Answer:** I wanted an end-to-end project that mirrors a real production system — not just CRUD, but
  authentication, authorization, async workflows, testing, containerization, and CI. The biggest
  lessons were around **JPA fetching and transaction boundaries** (the lazy-loading and after-commit
  dispatch issues) and the value of **making deliberate, documented trade-offs** rather than
  over-engineering up front.

---

## 8. "If you had more time, what would you improve?"

- **Answer:** Flyway/Liquibase migrations instead of Hibernate auto-DDL; refresh tokens plus a Redis
  denylist for token revocation; a durable message queue (or transactional outbox) for notifications;
  Testcontainers-based integration tests against real MySQL; frontend and end-to-end tests; and
  observability (metrics, tracing, structured logs). I'd also add a DB-level unique constraint to fully
  close the double-booking race window.

---

## 9. "Describe a time you worked independently / owned a project end-to-end."

- **Answer:** I owned Helios from architecture to deployment — designing the data model and API,
  implementing security, wiring async notifications, writing tests, containerizing with Docker Compose,
  setting up CI, and documenting it (README, OpenAPI/Swagger, architecture diagrams). Owning every
  layer forced me to make and defend real engineering decisions and understand how the pieces fit
  together.

---

### Behavioral quick-reference (weave these traits into any answer)
- **Ownership:** "I owned it end-to-end from design to deployment."
- **Judgment/trade-offs:** "I chose X for now and documented Y as the scale-up path."
- **Root-cause mindset:** "I fixed the underlying cause, not just the symptom."
- **Continuous learning:** "That bug taught me how JPA fetching / transactions really work."
- **Pragmatism:** "I picked the simplest reliable option over the clever one."
