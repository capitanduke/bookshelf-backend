# Bookshelf Backend - Project Instructions

## About This Project
This is a **Bookshelf Social API** built with Spring Boot. It allows users to manage their personal bookshelves, discover books, write reviews, follow other readers, and track reading activity. The developer is actively learning — always explain what you are going to do and why before implementing anything.

---

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Persistence | Spring Data JPA |
| Database | H2 (in-memory, for development) |
| Security | Spring Security + JWT (jjwt) |
| Validation | Spring Validation (`@Valid`, `@NotBlank`, etc.) |
| Boilerplate | Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.) |
| Build | Maven |

---

## Domain Summary
| Domain | Description |
|---|---|
| **User** | Registration, login, profile, follow/unfollow other users |
| **Book** | Book catalog, search via Google Books API |
| **Bookshelf** | Personal bookshelves per user (e.g., "Want to Read", "Read") |
| **Review** | Users write reviews and like reviews from others |
| **Activity Feed** | Tracks user actions (added book, wrote review, followed user, etc.) |

---

## Package Structure
All code lives under `com.readersnetwork.bookshelf`. Follow this layered architecture strictly:

```
entity/        → JPA entities (database tables)
repository/    → Spring Data JPA interfaces (database queries)
service/       → Business logic
controller/    → REST endpoints (thin layer, delegates to service)
dto/request/   → Input objects (what the API receives)
dto/response/  → Output objects (what the API returns)
exception/     → Custom exceptions + GlobalExceptionHandler
security/      → JWT filter, token provider, security config
config/        → App configuration beans
```

---

## Coding Conventions
- **Never expose JPA entities directly in API responses.** Always use response DTOs.
- **Never accept JPA entities as API input.** Always use request DTOs.
- Use `@RequiredArgsConstructor` (Lombok) for constructor injection in services and controllers.
- Use `@Builder` on DTOs when mapping from entities.
- All custom exceptions must be registered in `GlobalExceptionHandler`.
- Keep controllers thin — no business logic, only delegation to the service layer.
- Use meaningful HTTP status codes (`201 Created`, `404 Not Found`, `403 Forbidden`, etc.).

---

## Security Rules (Always Apply — No Exceptions)

These rules must be followed on every task, not just security-specific ones:

1. **Never expose sensitive fields** in response DTOs — no passwords, no raw JWT tokens, no internal IDs that reveal system structure.
2. **Always validate input** — use `@Valid` on request bodies and define constraints on DTOs (`@NotBlank`, `@Email`, `@Size`, etc.).
3. **Always check authorization** — being authenticated is not enough. Verify that the logged-in user is *allowed* to perform the action on that specific resource (e.g., a user can only edit their own review).
4. **Never hardcode secrets** — no passwords, API keys, or JWT secrets in source code. Use `application.properties` or environment variables.
5. **Use DTOs as a security boundary** — they prevent mass assignment attacks and accidental data leakage from entities.
6. **Sanitize error messages** — never return stack traces or internal exception details to the client.

---

## Learning Preferences
- Always explain the plan before writing any code.
- Keep explanations simple and focused — one concept at a time.
- Point out which file is being changed and why.
- If there are multiple approaches, recommend the simplest one first.
