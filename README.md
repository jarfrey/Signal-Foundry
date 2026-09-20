# Signal Foundry | Steelhacks 2026
## Created by Team "Straight Up Hackin' It"

Every job a company posts is a public statement about where it is spending next.
Signal Foundry exists to reveal these trends.

Signal Foundry collects postings from public job boards, categorizes postings and works
out whether each company is hiring faster or slower than it was, asks Nemotron 
what that pattern means, and serves both readings to a website: open roles for applicants,
hiring signals for investors.

## Run it

Three commands, from `awesomefinder/`. You need JDK 17+ and Node 18+.

```bash
# 1. add your Nemotron key (optional -- see "Without a key" below)
cp .env.example .env        # then edit .env and paste your key

# 2. scrape the boards and generate the reads (~25s, needs internet)
./gradlew ingest

# 3. start the API on :8080
./gradlew run
```

Then in a second terminal:

```bash
cd awesomefinder/frontend
npm install
npm run dev                 # http://localhost:5173
```

Vite proxies `/api` to `localhost:8080`, so the frontend needs no configuration.

On Windows use `gradlew.bat` instead of `./gradlew`.

Ingestion also generates each company's written read and caches it, so the
site is instant once this finishes. Re-run `ingest` whenever you want fresh
data; everything else reads from `signals.db`.

### Without a key

The app runs fine with no `NEMOTRON_API_KEY`. Company reads fall back to
deterministic summaries built from the same statistics, and both the API
(`insightSource`) and the UI (a `Computed` vs `Nemotron` badge on each card)
say which you are looking at, so a fallback is never mistaken for model output.

### A gotcha worth knowing

Nemotron 3 is a **reasoning model**: `max_tokens` has to cover its internal
reasoning *as well as* the answer. At 700 the larger prompts spent the whole
budget reasoning and returned truncated or empty JSON — which surfaced as a
confusing `JSONObject text must begin with '{'` error. `MAX_TOKENS` in
`InsightGeneratorService` is now 3000, and a `finish_reason: length` is
reported as a truncation rather than a syntax error. If you enrich the prompt
further and reads start falling back, raise it again.

The six calls run concurrently, which took a full refresh from ~4 minutes to
~20 seconds.

## How it fits together

```
Greenhouse / Lever JSON
        |  Greenhouse.java, Lever.java      one Posting per job
        v
     Labeler.java                           seniority, function, country, city
        |
        v
      Db.java  ->  signals.db (SQLite)      first_seen / last_seen / is_open
        |
        +--> CompanyStats.java              last 30d vs prior 30d, mix, velocity
        +--> Detector.java                  cross-company rules
                |
                v
     InsightGeneratorService.java           stats -> Nemotron -> a written read
                |
                v
      InsightApiServer.java  :8080          /api/jobs, /api/trends, ...
                |
                v
        React frontend :5173                applicant view + investor view
```

### How "hiring more or less than normal" is measured

Each posting carries its own publish date from the board (`first_published` on
Greenhouse, `createdAt` on Lever). Velocity is roles published in the last 30
days against the 30 days before that, so a **single** scrape already yields a
trend; you do not have to run this for a month before it says anything.

Repeated scrapes then add what one snapshot cannot see: `first_seen_at` /
`last_seen_at` per posting means the second run onwards can tell which roles
came *down*, which is the difference between a company that is quietly filling
roles and one that is pulling them. Run `./gradlew ingest` again on later days
and `closedThisMonth` starts reporting real numbers.

Percentages on small numbers are noise, so a company needs at least 4 postings
across the two windows before it is called anything but `Steady`.

## Which companies are tracked

Six boards are built in (Stripe, Databricks, Figma, Discord, Robinhood on
Greenhouse; Palantir, LinkedIn, GoPuff on Lever). To change the set without rebuilding, create
`awesomefinder/watchlist.txt`:

```
greenhouse  stripe
greenhouse  airbnb
lever       palantir
```

Not every company has a public board on both platforms — a handle that 404s is
reported and skipped, and the rest of the run continues.

## Tests

```bash
cd awesomefinder && ./gradlew test
```

Covers the two boards' differing JSON shapes, date normalisation, the velocity
and trend arithmetic, and unwrapping the model response.

This projected was created using AI assistance. 
