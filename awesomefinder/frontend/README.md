# AwesomeFinder frontend

This is the React/Vite client for AwesomeFinder. It currently uses local mock jobs when the backend is unavailable, so UI work can continue while the Java ingestion and API layers are being repaired.

## Run locally

From this directory:

```bash
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`. Change that target in `vite.config.ts` when the backend uses another port.

## API contract

The first real endpoint expected by the client is:

```text
GET /api/jobs -> Job[]
```

See `src/types.ts` for the current response shape. The fallback in `src/api.ts` can be removed once the endpoint is available.