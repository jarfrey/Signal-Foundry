package predictbackend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The JSON API the frontend reads.
 *
 *   GET  /api/health  - status, and whether any data has been ingested
 *   GET  /api/jobs    - open postings for the applicant view
 *   GET  /api/trends  - per-company stats plus the Nemotron read, for investors
 *   GET  /api/signals - raw cross-company detector output
 *   POST /api/refresh - re-run ingestion (handy during a demo)
 *
 * Data comes from signals.db, which `gradlew ingest` populates.
 */
public class InsightApiServer {

    private static final int PORT = Integer.getInteger("port", 8080);

    public static void main(String[] args) throws Exception {
        try (Db db = Db.open()) {
            InsightGeneratorService insights = new InsightGeneratorService();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/health", wrap(exchange -> health(db, insights)));
            server.createContext("/api/jobs", wrap(exchange -> jobs(db)));
            server.createContext("/api/trends", wrap(exchange -> trends(db, insights)));
            server.createContext("/api/signals", wrap(exchange -> signals(db)));
            server.createContext("/api/refresh", wrap(exchange -> refresh(db)));
            server.setExecutor(Executors.newFixedThreadPool(4));

            System.out.println("Backend running on http://localhost:" + PORT);
            System.out.println("  Nemotron key: " + (insights.hasKey() ? "present" : "MISSING (using fallback reads)"));

            int open = db.allOpenPostings().size();
            if (open == 0) {
                System.out.println("  No postings in signals.db yet -- run `gradlew ingest` first.");
            } else {
                System.out.println("  " + open + " open postings across "
                        + db.companySlugs().size() + " companies.");
            }

            server.start();
            Thread.currentThread().join();
        }
    }

    // ---- endpoints -----------------------------------------------------

    private static String health(Db db, InsightGeneratorService insights) throws Exception {
        return new JSONObject()
                .put("status", "ok")
                .put("companies", db.companySlugs().size())
                .put("openPostings", db.allOpenPostings().size())
                .put("nemotronKey", insights.hasKey())
                .toString();
    }

    /** Open roles shaped for the applicant list in the frontend. */
    private static String jobs(Db db) throws Exception {
        // Trend is a company-level property, so look it up once per company
        // rather than per posting.
        java.util.Map<String, String> trendBySlug = new java.util.HashMap<>();
        for (String slug : db.companySlugs()) {
            trendBySlug.put(slug, CompanyStats.compute(db, slug).trend);
        }

        JSONArray out = new JSONArray();
        for (Posting p : db.openPostings(500)) {
            out.put(new JSONObject()
                    .put("id", p.postingId)
                    .put("title", nz(p.title))
                    .put("company", capitalize(p.company))
                    .put("location", nz(p.locationRaw).isEmpty() ? "Not specified" : p.locationRaw)
                    .put("department", CompanyStats.pretty(nz(p.function).isEmpty() ? "OTHER" : p.function))
                    .put("posted", Dates.humanAge(p.postedAt))
                    .put("postedAt", nz(p.postedAt))
                    .put("source", capitalize(p.source))
                    .put("trend", trendBySlug.getOrDefault(p.company, "Steady"))
                    .put("url", nz(p.url))
                    .put("seniority", CompanyStats.pretty(nz(p.seniority).isEmpty() ? "IC" : p.seniority))
                    .put("description", nz(p.departmentRaw).isEmpty()
                            ? nz(p.title) + " at " + capitalize(p.company) + "."
                            : p.departmentRaw + " role at " + capitalize(p.company) + "."));
        }
        return out.toString();
    }

    /** Per-company statistics with the generated read attached. */
    private static String trends(Db db, InsightGeneratorService insights) throws Exception {
        List<Signal> all = Detector.detectAll(db.allOpenPostings());

        JSONArray out = new JSONArray();
        for (String slug : db.companySlugs()) {
            CompanyStats stats = CompanyStats.compute(db, slug);
            if (stats.openRoles == 0) {
                continue;   // nothing ingested for this board yet
            }
            InsightGeneratorService.Insight insight =
                    insights.forCompany(db, stats, Ingest.signalsFor(all, slug));

            JSONObject card = stats.toCardJson(insight.read(), insight.source());
            card.put("headline", insight.headline());
            card.put("bullets", new JSONArray(insight.bullets()));
            out.put(card);
        }
        return out.toString();
    }

    private static String signals(Db db) throws Exception {
        List<Signal> detected = Detector.detectAll(db.allOpenPostings());
        JSONArray out = new JSONArray();
        for (Signal s : detected) {
            out.put(new JSONObject()
                    .put("kind", s.kind)
                    .put("company", nz(s.company))
                    .put("claim", s.claim)
                    .put("confidence", s.confidence)
                    .put("evidence", new JSONArray(InsightGeneratorService.evidenceJson(s))));
        }
        return out.toString();
    }

    private static String refresh(Db db) throws Exception {
        Ingest.Summary summary = Ingest.run(db);
        return new JSONObject()
                .put("postingsSeen", summary.seen())
                .put("postingsNew", summary.added())
                .put("boardsFailed", summary.failedBoards())
                .put("signals", summary.signals())
                .toString();
    }

    // ---- plumbing ------------------------------------------------------

    private interface Endpoint {
        String handle(HttpExchange exchange) throws Exception;
    }

    /** CORS, error handling and JSON content type in one place. */
    private static HttpHandler wrap(Endpoint endpoint) {
        return exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                send(exchange, 200, endpoint.handle(exchange));
            } catch (Exception e) {
                e.printStackTrace();
                String message = e.getMessage() == null ? e.toString() : e.getMessage();
                send(exchange, 500, new JSONObject().put("error", message).toString());
            }
        };
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Kept so callers that imported this still compile. */
    public static List<String> endpoints() {
        return new ArrayList<>(List.of("/api/health", "/api/jobs", "/api/trends",
                "/api/signals", "/api/refresh"));
    }
}
