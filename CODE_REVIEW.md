# Ranger Survivor code review

Reviewed 6/24/2026. Spring Boot 3.3.4, Java 17, single `Score` table, H2 in dev and Postgres in prod, plus a plain-JS Canvas game served as static files. Build is green (1 test, 0 failures in the last surefire run). I could not do a clean rebuild here (sandbox has Java 11 and no Maven Central), so the findings below are from reading the code and the build artifacts, not a fresh compile.

## What's solid

- Package layout is conventional and easy to follow. Controller, model, repository, service. Nothing surprising.
- Input is validated where it enters: `@NotBlank`, `@Size(max=24)`, `@Min(0)` on the entity, and `limit` is clamped to 1..100 on both read endpoints.
- XSS is handled. `escapeHtml` runs on every leaderboard name before it hits the DOM, and names are profanity-filtered server-side.
- `localStorage` reads and writes are wrapped in try/catch, so a locked-down browser won't break startup.
- The game loop is wrapped in try/catch so one bad frame logs and keeps ticking instead of freezing the canvas.
- The Dockerfile is a proper multi-stage build with a separate dependency-cache layer and `MaxRAMPercentage` for container memory limits.
- The prod profile disables the H2 console, switches to the Postgres dialect, and pulls secrets from env with `sync: false`. Right instincts.
- Map generation is the most interesting code in the repo and it's careful: BFS reachability from the player to every spawn, corridor-carving fallback when a fort gets boxed in, and an iterative water-adjacency cleanup. That's real problem-solving, not filler.

## High

**1. The leaderboard trusts client-submitted scores.** `POST /api/scores` accepts whatever `kills` and `durationSeconds` the client sends and persists them. Nothing links the two, nothing caps them, and there's no rate limit. Anyone can `curl` a score of 999999 and own the board. For a public portfolio demo this matters, because the board gets junked the first time someone looks at the network tab. Options, cheapest first: cap `kills` and `durationSeconds` at sane maxima, reject `kills` that exceed what the duration could physically produce, and add a simple per-IP rate limit. If you'd rather accept the risk for a demo, that's defensible, but right now the tradeoff is silent. Closing it is also a good interview talking point because it shows you think about trust boundaries.

**2. `ProfanityFilter` has no HTTP timeout.** `RestClient.builder().build()` uses the JDK default with no read timeout, and `submit()` blocks on that call synchronously. If PurgoMalum hangs, the request thread hangs with it, and on the free tier's small thread pool a slow upstream can stall submissions. Set a connect and read timeout (something like 2s and 3s) on the request factory. The fail-open catch already exists, so a timeout just trips the path you've already written.

## Medium

**3. The test hits the real PurgoMalum API.** `ScoreControllerTest` submits "ryan", which runs the live `ProfanityFilter` against the external API on every test run. It passes offline only because the filter fails open. That's the external-dependency-in-tests problem: slow, flaky, and it isn't actually asserting anything about profanity. Mock the filter with `@MockBean` and add a real test for the rejection path. The profanity feature currently has zero test coverage, and the rejection branch is exactly the behavior worth pinning down.

**4. `ddl-auto: update` is still on in prod.** The prod profile lets Hibernate mutate the live schema on boot. That's the "fix it in v2" category of risk. The `spring-starter` scaffold already uses Flyway, so the pattern is one you have. For the Postgres deployment, use a versioned migration and set `ddl-auto` to `validate` or `none`. The schema is one table, so this is low effort.

**5. The JPA entity is also the API contract.** `Score` is the `@Entity`, the `@RequestBody`, and the response body. `submittedAt` is overwritten server-side and `id` has no setter, so neither can be injected by default Jackson, meaning it isn't exploitable today. The smell is the coupling: adding a column to the entity silently changes the wire format, and vice versa. A small `ScoreRequest`/`ScoreResponse` DTO pair breaks that link. Optional for a demo, standard at work.

## Low / polish

**6. `GET /api/scores/me` is dead code.** No frontend caller, no test. Either wire it to a "your recent runs" panel or delete it.

**7. The health-check wiring is split.** `render.yaml` points its check at `/api/scores/top?limit=1`, which runs a DB query, while the purpose-built `/api/health` endpoint (fast 200 plus async DB warm-up) goes unused by Render. If you want fast liveness, point Render at `/api/health`. If you want the check to verify the DB every time, the comment on `HealthController` is misleading about who calls it. Pick one and reconcile.

**8. Enemy count is unbounded.** Spawn interval floors at 500ms and enemies only leave on a kill, so a long run accumulates them, each running an A* on every move tick. The 16x16 grid keeps A* cheap so it isn't urgent, but a cap or off-screen culling would protect frame rate on long survivals.

**9. A* scans the open list linearly** to find the lowest-f node, O(n) per pop. Fine for 256 cells. Only worth a binary heap if the map ever grows. Noting it so the choice is on the record, not as a fix.

**10. The `sprites` object literal lists three sheets** (`archer`, `terrain`, `skeleton`) but `loadSprites` also sets `fort` and `grass`. It works because JS objects are dynamic, but the literal reads as if those are the only sheets. List all five or drop the initializer.

**11. Comment density.** Some comments restate well-named code (`fireArrow`, `queueMove` read fine on their own). The map-gen and sprite-alignment comments earn their place because the geometry isn't obvious. By your own rule, names and structure should carry intent. Partly justified here since a recruiter reads this, so this is a light flag, not a demand.

**12. README drift.** The structure block omits `service/` and `HealthController`, and says "drop the sprite pack here" though the sprites are committed.

## Portfolio framing

This is solid working-proficiency Spring Boot and it backs up the claims you make for it: persistence, REST endpoints, JPA, and a third-party moderation integration. The two changes that would most strengthen it as an interview piece are both small: server-side score validation (finding 1) and the profanity-path test (finding 3). One shows you guard trust boundaries, the other shows the testing discipline you lead with. Neither changes the honest framing that this is a personal project at working proficiency, not a production-scale system.

If you want, I can fix findings 1 through 3 in a branch and add the missing tests.
