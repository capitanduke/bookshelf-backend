# Spring Boot Skill — Bookshelf Backend

This skill provides step-by-step checklists for common Spring Boot tasks in this project. Always follow these patterns to keep the codebase consistent.

---

## Checklist 1: Add a New Feature (Full Slice)

Use this when adding something completely new (e.g., a new domain concept like "Reading Goals").

**Step 1 — Entity**
- [ ] Create the JPA entity class in `entity/`
- [ ] Annotate with `@Entity`, `@Table(name = "...")`
- [ ] Add `@Id` and `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- [ ] Use Lombok: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- [ ] Define relationships with `@ManyToOne`, `@OneToMany`, etc.
- [ ] **Security:** Do not include sensitive or internal fields that should never leave the backend

**Step 2 — Repository**
- [ ] Create an interface in `repository/` extending `JpaRepository<Entity, Long>`
- [ ] Add only the custom query methods you actually need (avoid over-engineering)

**Step 3 — DTOs**
- [ ] Create a request DTO in `dto/request/` for API input
- [ ] Create a response DTO in `dto/response/` for API output
- [ ] Add validation annotations on the request DTO (`@NotBlank`, `@Email`, `@Size`, `@NotNull`)
- [ ] **Security:** Response DTO must never include passwords, tokens, or fields not needed by the client
- [ ] Use Lombok `@Data` / `@Builder` on DTOs

**Step 4 — Service**
- [ ] Create a service class in `service/` annotated with `@Service`
- [ ] Inject the repository using `@RequiredArgsConstructor` (constructor injection)
- [ ] Implement business logic here — not in the controller
- [ ] **Security:** Verify the logged-in user is authorized to perform the action (not just authenticated)
- [ ] Map entity → response DTO before returning from service methods
- [ ] Throw custom exceptions (e.g., `ResourceNotFoundException`) instead of returning null

**Step 5 — Controller**
- [ ] Create a controller in `controller/` annotated with `@RestController` and `@RequestMapping("/api/...")`
- [ ] Inject the service using `@RequiredArgsConstructor`
- [ ] Keep methods thin: validate input with `@Valid`, delegate to service, return `ResponseEntity`
- [ ] Use correct HTTP methods: `GET` (read), `POST` (create), `PUT/PATCH` (update), `DELETE` (delete)
- [ ] Use correct status codes: `200 OK`, `201 Created`, `204 No Content`, `404 Not Found`, `403 Forbidden`
- [ ] **Security:** Add `@PreAuthorize` or check JWT principal where access must be restricted

**Step 6 — Exception Handling**
- [ ] Add any new custom exceptions to `GlobalExceptionHandler`
- [ ] Return a clean error message and appropriate HTTP status — never a stack trace

---

## Checklist 2: Add a New Endpoint to an Existing Controller

Use this when adding a new action to an existing feature (e.g., adding "unlike review" to the reviews feature).

- [ ] Identify the correct controller in `controller/`
- [ ] Add the new method with the right HTTP annotation (`@GetMapping`, `@PostMapping`, etc.)
- [ ] Accept input via `@RequestBody` (with `@Valid`) or `@PathVariable` / `@RequestParam`
- [ ] Delegate all logic to the service layer — no logic in the controller
- [ ] Return `ResponseEntity<YourResponseDto>`
- [ ] **Security:** Ask yourself — should this endpoint be public or require authentication? Should only certain users access it?
- [ ] Add any new DTO if the input/output shape is different from existing ones
- [ ] Test with Bruno collection (add a request to the relevant folder)

---

## Checklist 3: Add a Custom Exception

Use this when a specific error case needs a clear, descriptive exception (e.g., `BookNotFoundException`).

- [ ] Create the exception class in `exception/`
- [ ] Extend `RuntimeException`
- [ ] Add a constructor that accepts a `String message` and passes it to `super(message)`
- [ ] Go to `GlobalExceptionHandler` and add a new `@ExceptionHandler` method for it
- [ ] Return a meaningful HTTP status (e.g., `404 Not Found` for "not found" errors, `403 Forbidden` for auth errors)
- [ ] **Security:** The error message returned to the client should be informative but never reveal internal details (e.g., no SQL errors, no stack traces)

Example structure:
```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }
}
```

---

## Checklist 4: Security — Adding or Modifying Auth/Access Control

Use this whenever touching security configuration, JWT, or access rules.

- [ ] **New endpoint visibility:** Decide if the endpoint is public or requires authentication. Update `SecurityConfig` if needed.
- [ ] **Role/ownership check:** If only the resource owner can perform an action (e.g., edit their own review), retrieve the current user from the JWT principal and compare with the resource owner.
- [ ] **JWT secret:** Must live in `application.properties` as `app.jwt.secret` (or similar) — never hardcoded in Java code.
- [ ] **Token expiry:** Confirm expiry is set. Short-lived tokens are safer.
- [ ] **Password handling:** Always use `PasswordEncoder` (BCrypt). Never store or log plain-text passwords.
- [ ] **Sensitive response fields:** Double-check that no password hash, internal token, or secret leaks through a response DTO.
- [ ] **Input validation:** Any endpoint that accepts user data must use `@Valid` — this prevents malformed or malicious input from reaching the service layer.
- [ ] **Error responses:** Return `401 Unauthorized` (not authenticated) vs `403 Forbidden` (authenticated but not allowed) — use them correctly.

---

## Quick Reference — Common Annotations

| Annotation | Where | Purpose |
|---|---|---|
| `@RestController` | Controller | Marks class as REST controller |
| `@RequestMapping` | Controller | Base URL path |
| `@Service` | Service | Marks class as a service bean |
| `@Repository` | Repository | Marks class as a repository bean (usually inferred) |
| `@Entity` | Entity | Marks class as a JPA entity |
| `@Valid` | Controller param | Triggers DTO validation |
| `@NotBlank`, `@Email`, `@Size` | DTO fields | Validation constraints |
| `@PreAuthorize` | Controller method | Method-level security expression |
| `@RequiredArgsConstructor` | Service/Controller | Lombok constructor injection |
| `@Builder` | DTO / Entity | Lombok builder pattern |
