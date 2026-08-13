# RateShaper

A live, interactive rate-limiting algorithm playground. Pick an algorithm, configure its parameters, fire a simulated traffic burst, and watch in real time how each algorithm allows or blocks requests.

## What it does

RateShaper runs entirely in a single JVM with no external dependencies. The backend implements four classic rate-limiting strategies and exposes them through a REST API plus Server-Sent Events (SSE). The frontend is plain HTML/CSS/JS served directly by Spring Boot, so the entire app ships as one runnable JAR.

## The four algorithms

1. **Token Bucket** — A bucket holds up to `capacity` tokens and refills continuously at `refillRatePerSec` tokens per second. Bursts are absorbed smoothly until the bucket empties, after which traffic is throttled to the refill rate.

2. **Fixed Window Counter** — Time is sliced into fixed windows of `windowSizeMs` with a hard `requestLimit` per window. Fast and memory-efficient, but vulnerable to boundary bursts: a client can send `2 × requestLimit` requests by straddling the edge between two windows.

3. **Sliding Window Log** — Keeps a timestamp log of every request in the rolling window. Old entries are purged on each check. This is the most accurate algorithm because it tracks every single arrival, but it is also the most memory-intensive.

4. **Sliding Window Counter** — Approximates the sliding log using only two counters (`currentWindowCount` and `previousWindowCount`). The estimated load is `current + previous × overlapFraction`, where `overlapFraction` decreases as the current window progresses. This is the efficient middle ground: fairer than fixed window and far less memory than the log.

## How to run locally

Requires Java 17 and Maven 3.8+.

```bash
mvn clean package
java -jar target/rateshaper-1.0.0.jar
```

Or directly with the Spring Boot plugin:

```bash
mvn spring-boot:run
```

Then open http://localhost:8080 in your browser.

## Deploying to Render

1. Create a new **Web Service** on Render.
2. Connect your Git repository.
3. Set the build command to: `mvn clean package`
4. Set the start command to: `java -jar target/*.jar`
5. Render injects a `PORT` environment variable; the app reads it automatically via `server.port=${PORT:8080}`.

No Docker, no database, no extra services — the JAR serves both the API and the static frontend.
