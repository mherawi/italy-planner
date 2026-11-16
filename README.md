# Three Days in Italy

This React trip planner uses a Java 21 and Spring Boot API. PostgreSQL stores the 103 records from the supplied [italy.json](https://storage.googleapis.com/interview-booking/italy.json), which is bundled with the application.

Spring Data JPA handles catalog reads and Hibernate maps the original JSON to PostgreSQL `jsonb`. The API filters the small catalog in memory. Schema setup creates the tables, and imports use a database lock through JPA so overlapping batches can safely update the same IDs.

## Run locally

```sh
docker compose up --build
```

Open http://localhost:8080 for the planner. Compose starts PostgreSQL and waits for it to be ready before starting the app. The database uses a named volume and is accessible to the app over the Compose network. The default database password is for local development.

For local development against an existing database, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`, then run `mvn spring-boot:run` with JDK 21. In a second terminal, run `npm ci` and `npm run dev` in `frontend`. Vite proxies API requests to Spring Boot on port 8080.

To build a single JAR, run `npm ci` and `npm run build` in `frontend`, then `mvn clean package` in the project root. Spring Boot includes the built React files from `frontend/dist` and serves the page and API on the same origin.

## API

Use **Account** in the planner to register, sign in, and sign out. Accounts are stored in PostgreSQL with bcrypt password hashes. Sign-in issues a signed JWT in an HttpOnly cookie that expires after eight hours. Signing out clears the browser cookie.

For hosted deployments, set `PLANNER_JWT_SECRET` to a cryptographically random secret of at least 32 bytes and set `PLANNER_JWT_COOKIE_SECURE=true` for HTTPS. The Render Blueprint generates a secret. Without a configured secret, the app generates a temporary key at startup and accounts must sign in again after each restart.

The planner saves your itinerary in your browser.

Set `OPENROUTESERVICE_API_KEY` in the app environment (or in the ignored Compose `.env` file) to enable walking directions. The itinerary estimates each consecutive same-city leg. If the key is absent or routing fails, the planner labels its straight-line estimate based on 4.5 km/h. Intercity legs prompt users to check transport.

- `GET /api/routes/walking?from=place_001&to=place_005` returns `minutes` and `source` for a same-city leg.

- `GET /api/places` lists the catalog. Optional case-insensitive filters are `city`, `region`, `type`, `tag`, and `q`. Filters combine with AND. The query matches a substring of the name, city, region, neighborhood, or description.
- `GET /api/places/place_001` returns the Colosseum. Unknown IDs return HTTP 404.
- `POST /api/places/import` accepts a nonempty JSON array in the same shape as `italy.json`. A successful import returns HTTP 200 with the number of records processed, for example `{"imported":103}`.

For example, use `/api/places?city=Florence&type=museum` to browse Florence museums.

To import a file, send its contents as the request body.

```sh
curl -X POST http://localhost:8080/api/places/import \
  -H "Content-Type: application/json" \
  --data-binary "@src/main/resources/italy.json"
```

Each entry requires a nonblank `id`, `name`, `type`, and `city`. Invalid entries and duplicate IDs in the same batch return HTTP 400 without writing any rows. Imports insert new IDs and replace records with matching IDs in one transaction. Send the complete record for every ID you want to update.

Startup fills in missing IDs from the bundled dataset and preserves existing records. Responses keep the source field names, nulls, hours, seasonal notes, booking flags, and any extra fields.

## Next milestone

The planner currently lets travelers assemble a trip manually, see visit totals, and reorder or remove stops. The draft is saved in browser storage. The next milestone is an itinerary generation endpoint that considers opening hours, travel time, and traveler preferences.
