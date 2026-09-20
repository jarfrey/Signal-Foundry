package predictbackend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The pipeline: fetch every board on the watchlist, label each posting,
 * store the snapshot in SQLite, then run the cross-company detectors.
 *
 * Run it with `gradlew ingest`. Run it more than once over a few days and
 * the closed-role numbers become real rather than inferred.
 */
public class Ingest {

    public static void main(String[] args) throws Exception {
        try (Db db = Db.open()) {
            run(db);
        }
    }

    public static Summary run(Db db) throws Exception {
        List<Watchlist.Entry> watchlist = Watchlist.load();
        System.out.println("Ingesting " + watchlist.size() + " boards...");

        Greenhouse greenhouse = new Greenhouse();
        Lever lever = new Lever();

        int totalSeen = 0;
        int totalNew = 0;
        int failed = 0;

        for (Watchlist.Entry entry : watchlist) {
            int runId = db.startRun(entry.source(), entry.handle());
            try {
                List<Posting> postings = switch (entry.source()) {
                    case "greenhouse" -> greenhouse.fetch(entry.handle());
                    case "lever" -> lever.fetch(entry.handle());
                    default -> throw new IllegalArgumentException("unknown source: " + entry.source());
                };

                for (Posting p : postings) {
                    Labeler.label(p);
                }

                int companyId = db.upsertCompany(entry.handle(), null);
                int added = db.saveSnapshot(companyId, runId, entry.source(), entry.handle(), postings);

                db.finishRun(runId, "OK", postings.size(), added, null);
                totalSeen += postings.size();
                totalNew += added;

                System.out.printf("  %-12s %-14s %4d postings (%d new)%n",
                        entry.source(), entry.handle(), postings.size(), added);

            } catch (Exception e) {
                // One dead board must not take the whole run down.
                failed++;
                db.finishRun(runId, "ERROR", 0, 0, e.getMessage());
                System.err.printf("  %-12s %-14s FAILED: %s%n",
                        entry.source(), entry.handle(),
                        e.getMessage() == null ? e.toString() : e.getMessage());
                if (System.getProperty("ingest.trace") != null) {
                    e.printStackTrace();
                }
            }
        }

        // Cross-company rules need every company's postings at once, so this
        // runs after the whole watchlist is in.
        List<Posting> all = db.allOpenPostings();
        List<Signal> signals = Detector.detectAll(all);
        db.replaceSignals(signals);

        System.out.printf("%nStored %d postings (%d new), %d boards failed, %d signals detected.%n",
                totalSeen, totalNew, failed, signals.size());

        // Generate the written reads now rather than on the first page load.
        // These are cached in the insight table keyed by a hash of the stats,
        // so the API serves them instantly and only regenerates when the
        // numbers actually move.
        warmInsights(db, signals);

        if (totalSeen == 0) {
            System.err.println("No postings stored. Check network access to the job board APIs.");
        }

        return new Summary(totalSeen, totalNew, failed, signals.size());
    }

    public record Summary(int seen, int added, int failedBoards, int signals) {
    }

    /** Pre-generate each company's read so the API never blocks on the model. */
    static void warmInsights(Db db, List<Signal> signals) throws Exception {
        InsightGeneratorService generator = new InsightGeneratorService();
        System.out.println(generator.hasKey()
                ? "Generating insights with Nemotron..."
                : "No NEMOTRON_API_KEY set -- writing computed summaries instead.");

        // Compute every company's statistics up front: SQLite is single-writer
        // and these reads must not overlap the concurrent model calls below.
        Map<String, CompanyStats> statsBySlug = new LinkedHashMap<>();
        for (String slug : db.companySlugs()) {
            CompanyStats stats = CompanyStats.compute(db, slug);
            if (stats.openRoles > 0) {
                statsBySlug.put(slug, stats);
            }
        }

        // The calls are independent and each takes the better part of a
        // minute, so run them together rather than end to end.
        Map<String, InsightGeneratorService.Insight> results = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(6, Math.max(1, statsBySlug.size())));
        try {
            List<Future<?>> pending = new ArrayList<>();
            for (Map.Entry<String, CompanyStats> e : statsBySlug.entrySet()) {
                pending.add(pool.submit(() -> results.put(
                        e.getKey(),
                        generator.generate(e.getValue(), signalsFor(signals, e.getKey())))));
            }
            for (Future<?> f : pending) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }

        // Write the results back on this thread, one at a time.
        int fromModel = 0;
        for (Map.Entry<String, CompanyStats> e : statsBySlug.entrySet()) {
            CompanyStats stats = e.getValue();
            InsightGeneratorService.Insight insight = results.get(e.getKey());
            if (insight == null) {
                continue;
            }
            db.saveInsight(stats.companyId, insight.headline(), insight.toJson().toString(),
                    new org.json.JSONArray(insight.bullets()).toString(),
                    "nvidia/nemotron-3-ultra-550b-a55b", insight.source(), stats.hash());

            if ("nemotron".equals(insight.source())) {
                fromModel++;
            }
            System.out.printf("  %-12s %+5.0f%%  %-8s [%s] %s%n",
                    e.getKey(), stats.velocity, stats.trend,
                    "nemotron".equals(insight.source()) ? "AI" : "computed", insight.headline());
        }

        System.out.printf("%d of %d reads came from Nemotron.%n", fromModel, statsBySlug.size());
    }

    /** Signals for one company, refreshed from the current open postings. */
    public static List<Signal> signalsFor(List<Signal> all, String slug) {
        List<Signal> out = new ArrayList<>();
        for (Signal s : all) {
            if (s.company == null || s.company.isEmpty() || s.company.equals(slug)) {
                out.add(s);
            }
        }
        return out;
    }
}
