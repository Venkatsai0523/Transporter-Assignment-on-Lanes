# Transporter Assignment & Cost Optimization System

Production-style Spring Boot backend for assigning exactly one eligible transporter to every delivery lane while maximizing the number of distinct transporters used and, secondarily, minimizing total cost.

## Optimization policy

The objectives are lexicographic:

1. Maximize the number of distinct transporters actually assigned, without exceeding `maxTransporters`.
2. Among assignments using that maximum feasible number, minimize the exact `BigDecimal` total cost.
3. Assign every lane exactly once.
4. Every selected transporter must receive at least one lane.

If the requested limit exceeds the number of feasible transporters, the largest feasible number is used. Equal quote values are resolved by the smallest transporter ID, and assignments are sorted by lane ID. The sample amount of 29000 is not guaranteed from the supplied quotation data; the worked sample below uses the actual quotations.

## Stack

- Java 21
- Spring Boot 3.5.x
- PostgreSQL 16
- Spring Data JPA / Hibernate
- Flyway
- Bean Validation
- Springdoc OpenAPI
- JUnit 5, Mockito, MockMvc
- Testcontainers
- Docker / Docker Compose
- JaCoCo

## Run locally

### Docker

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`. Swagger is at `http://localhost:8080/swagger-ui.html`.

Copy [.env.example](.env.example) to `.env` and set a local database password. Never commit `.env`.

### Maven

```bash
./mvnw.cmd spring-boot:run
```

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, or use PostgreSQL defaults from `application.properties`.

## API

| Method | Endpoint | Success |
|---|---|---|
| POST | `/api/v1/transporters/input` | 201 |
| POST | `/api/v1/transporters/assignment` | 200 |
| GET | `/actuator/health` | 200 |
| GET | `/swagger-ui.html` | 200 |
| GET | `/v3/api-docs` | 200 |

### Submit input

```json
{
  "lanes": [
    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
    {"id": 2, "origin": "Delhi", "destination": "Bangalore"}
  ],
  "transporters": [
    {"id": 1, "name": "T1", "laneQuotes": [
      {"laneId": 1, "quote": 5000},
      {"laneId": 2, "quote": 6000}
    ]},
    {"id": 2, "name": "T2", "laneQuotes": [
      {"laneId": 2, "quote": 4500}
    ]}
  ]
}
```

Repeated submissions replace the active dataset.

### Optimize

```json
{"maxTransporters": 2}
```

Response:

```json
{
  "status": "success",
  "totalCost": 9500,
  "assignments": [
    {"laneId": 1, "transporterId": 1},
    {"laneId": 2, "transporterId": 2}
  ],
  "selectedTransporters": [1, 2]
}
```

## Algorithm

The engine enumerates feasible transporter combinations up to `maxTransporters`. For every combination it selects the cheapest quote for each lane, rejects combinations that leave a lane uncovered or any selected transporter unused, and compares the valid solutions. It uses exact decimal arithmetic and deterministic ID-based tie breaking.

Exhaustive search is intentionally bounded to 100 lanes, 20 transporters, and 500,000 combinations. Inputs beyond those limits return HTTP 400 rather than silently returning a potentially incorrect result.

## Database

Flyway creates `lanes`, `transporters`, and `lane_quotes`. Quotes have positive-value database constraints, foreign keys, and unique lane/transporter pairs.

## Tests

```bash
./mvnw.cmd test
```

JaCoCo reports are generated at `target/site/jacoco/index.html`.

## Postman

Import `postman/TransporterAssignment.postman_collection.json`. Set the `baseUrl` environment variable to `http://localhost:8080` or the deployed HTTPS URL.

## Deployment

Build and run the Docker image with a hosted PostgreSQL database:

```bash
docker build -t transporter-assignment .
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://HOST:5432/DATABASE' \
  -e DB_USERNAME='USERNAME' \
  -e DB_PASSWORD='PASSWORD' \
  -e PORT=8080 transporter-assignment
```

For Render, use `render.yaml` or create a Docker web service and configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the dashboard. Set HTTPS and health checks through the hosting provider. No live URL is hardcoded.
