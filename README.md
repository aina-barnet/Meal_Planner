# Planificador d'àpats — Weekly Family Meal Planner

Plans a family's week of lunches and dinners — including the ones that go to work in a lunchbox — and turns it into an aggregated shopping list, entirely on your own computer.

![Menu board: a generated week with lunchbox badges and kcal fit per meal](docs/menu-board.png)

## Start and stop with a double click (Windows)

For people who don't use a terminal. Docker Desktop must be installed once.

- **`Engegar.bat`** — starts Docker Desktop if needed, starts the app and opens it in the browser. The first time takes a few minutes because it builds the app.
- **`Aturar.bat`** — stops the app. The data is kept for the next time.

Tip: right-click each file → *Send to* → *Desktop (create shortcut)* to have them on the desktop.

## Quick start

```sh
docker compose up
```

Then open <http://localhost:3000>. The first launch creates the database, runs the migrations and seeds the recipe catalogue. All data lives in the `pgdata` Docker volume, so it survives `docker compose down` and restarts (only `docker compose down -v` deletes it).

| Service | URL |
| --- | --- |
| Web app | <http://localhost:3000> |
| API + OpenAPI UI | <http://localhost:8080/api/docs> |
| PostgreSQL | `localhost:5432`, database/user/password `mealplanner` |

## Development workflow

Run only the database in Docker, the API from your IDE and the frontend with Vite's dev server, for hot reload and debugging:

```sh
# 1. Database (same named volume as the full stack)
docker compose up db

# 2. API on :8080 — or run ApiApplication from the IDE. Defaults point at localhost:5432.
cd api
./gradlew bootRun

# 3. Frontend on :5173 with hot reload; /api is proxied to :8080
cd web
npm install
npm run dev
```

Tests:

```sh
cd api && ./gradlew test     # domain unit tests + Testcontainers integration tests (needs Docker)
# Without Docker, point the integration tests at an existing PostgreSQL (they wipe it):
TEST_DB_URL=jdbc:postgresql://localhost:5432/mealplanner_test ./gradlew test
cd web && npm run lint && npm run build
```

## What it does

1. **Setup wizard** — the household and its members: diet, allergies and intolerances, dislikes, and an orientative energy estimate (Mifflin-St Jeor × activity) that can always be overridden by hand.
2. **Week configuration** — which lunches and dinners to plan and, for each one, who eats at home and who takes a lunchbox.
3. **Menu board** — a generated, balanced week that can be rearranged by dragging dishes, replaced from a filtered picker, or locked before regenerating. Dishes that do not survive a lunchbox are never placed in a meal with lunchbox diners, and the API rejects such assignments too.
4. **Shopping list** — scaled, converted and aggregated across the week, minus the pantry, rounded up to whole packages and grouped by aisle, with a print stylesheet. Leftovers of each package go back into the pantry for the following week.

## Architecture

```
/api   Spring Boot 4.1 (Java 21), PostgreSQL, Flyway, springdoc-openapi
/web   React + TypeScript + Vite, TanStack Query, dnd-kit, Tailwind CSS
/docs  Design notes and decision log
```

All domain logic (nutritional requirements, portion scaling, unit conversion, ingredient aggregation, purchase rounding and the menu generator) lives in plain Java classes under `app.mealplanner.domain`, with no Spring or JPA. They take data in and return data out, and are unit-tested without a Spring context or a database; `DomainPurityTest` fails the build if a domain class imports Spring, JPA or the application layers. Controllers, services and repositories (`web`, `application`, `persistence`) are a thin layer that maps HTTP and persistence onto that domain.

The menu generator (`domain.generator.MenuGenerator`) is rules plus scoring: hard constraints filter the catalogue per meal (lunchbox, allergies, diet), survivors are scored on kcal fit, protein rotation, variety and recency, and one of the top five is drawn at random. The scoring is explained at the top of the class.

The API is documented with OpenAPI at `/api/docs`. Errors are RFC 9457 problem details with a machine-readable `code` and, for business rule violations, human-readable `reasons`.

See [docs/decisions.md](docs/decisions.md) for the decisions taken along the way.
