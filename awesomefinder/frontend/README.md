# Signal Foundry frontend

React + Vite client. Two views off a hash router: `#/applicant` lists open
roles and can rank them against a pasted resume, `#/investor` shows per-company
hiring velocity with the generated read.

## Run locally

```bash
npm install
npm run dev
```

The dev server proxies `/api` to `http://localhost:8080`, so start the Java
backend first (see the root README). Change the target in `vite.config.ts` if
the backend runs elsewhere.

If the backend is not up, both pages fall back to a handful of sample rows and
show a `SAMPLE DATA` notice — the UI never presents mock numbers as real ones.

## API contract

| Endpoint | Shape |
|---|---|
| `GET /api/jobs` | `Job[]` |
| `GET /api/trends` | `CompanyTrend[]` |

Both are defined in `src/types.ts`. `CompanyTrend.insightSource` is
`'nemotron'` when the read came from the model and `'fallback'` when it was
computed from the statistics, and each card badges which.

## Resume matching

`rankJobs` in `src/api.ts` scores postings by term overlap with the resume,
weighting title matches over body matches and normalising by the posting's own
vocabulary so short ads are not favoured. It runs entirely in the browser — the
resume is never uploaded.

## Colour

Colours that carry meaning live in `:root` in `src/styles.css`. The trend and
velocity colours were checked for WCAG contrast against the surface each sits
on, and every one is rendered next to its own word, so colour never carries the
meaning by itself. The mix bars use a single hue on purpose: each bar is
directly labelled, so the colour encodes magnitude rather than which function
it is.
