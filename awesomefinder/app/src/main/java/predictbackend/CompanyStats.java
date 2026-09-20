package predictbackend;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The per-company numbers that answer "is this company hiring more or less
 * than normal", derived entirely from SQL over the posting table.
 *
 * The comparison is last 30 days of postings against the 30 days before that.
 * Using each posting's own publish date means this works off a single scrape,
 * so the very first run already produces a trend; repeated scrapes then
 * sharpen it by filling in closedLast30 (roles taken down), which a one-off
 * snapshot cannot see.
 */
public class CompanyStats {

    public String slug;
    public int companyId;

    public int openRoles;
    public int newLast30;
    public int prev30;
    public int closedLast30;

    /** Percent change in new postings vs the prior 30 days. */
    public double velocity;

    /** "Growing" | "Steady" | "Cooling" */
    public String trend;

    public String topLocation;
    public String topFunction;

    /** How many successful scrapes back these numbers. */
    public int runs;

    public Map<String, Integer> functionMix = new LinkedHashMap<>();
    public Map<String, Integer> seniorityMix = new LinkedHashMap<>();
    public Map<String, Integer> countryMix = new LinkedHashMap<>();

    public static CompanyStats compute(Db db, String slug) throws SQLException {
        CompanyStats s = new CompanyStats();
        s.slug = slug;
        s.companyId = db.companyId(slug);

        s.openRoles = db.countOpen(slug);
        s.newLast30 = db.countPostedBetween(slug, 30, 0);
        s.prev30 = db.countPostedBetween(slug, 60, 30);
        s.closedLast30 = db.countClosedSince(slug, 30);
        s.runs = db.runCount(slug);

        s.functionMix = db.breakdown(slug, "function");
        s.seniorityMix = db.breakdown(slug, "seniority");
        s.countryMix = db.breakdown(slug, "country");
        s.topLocation = firstKey(db.breakdown(slug, "city"), "Distributed");
        s.topFunction = firstKey(s.functionMix, "OTHER");

        s.velocity = velocity(s.newLast30, s.prev30);
        s.trend = trend(s.velocity, s.newLast30, s.prev30);

        return s;
    }

    /**
     * Percent change, rounded to a whole number.
     *
     * Growth from a zero baseline has no defined percentage, so rather than
     * divide by zero we report a flat +100% per new role capped at 999 --
     * enough to sort a genuine restart to the top without inventing precision.
     */
    static double velocity(int now, int before) {
        if (before == 0) {
            return now == 0 ? 0 : Math.min(999, now * 100.0);
        }
        return Math.round(((now - before) / (double) before) * 100.0);
    }

    /**
     * Small absolute numbers make percentages noisy -- 1 role becoming 3 is
     * +200% but means little. Require a few postings before calling a trend.
     */
    static String trend(double velocity, int now, int before) {
        if (now + before < 4) {
            return "Steady";
        }
        if (velocity >= 20) {
            return "Growing";
        }
        if (velocity <= -20) {
            return "Cooling";
        }
        return "Steady";
    }

    private static String firstKey(Map<String, Integer> map, String fallback) {
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!"UNKNOWN".equals(e.getKey()) && !e.getKey().isBlank()) {
                return e.getKey();
            }
        }
        return fallback;
    }

    /** Top N buckets as share-of-total, which is what the investor bars show. */
    public List<Map.Entry<String, Double>> topMix(int n) {
        int total = functionMix.values().stream().mapToInt(Integer::intValue).sum();
        List<Map.Entry<String, Double>> out = new ArrayList<>();
        if (total == 0) {
            return out;
        }
        for (Map.Entry<String, Integer> e : functionMix.entrySet()) {
            if (out.size() >= n) {
                break;
            }
            out.add(Map.entry(pretty(e.getKey()), e.getValue() / (double) total));
        }
        return out;
    }

    /** ENGINEERING -> Engineering, for display. */
    public static String pretty(String label) {
        if (label == null || label.isEmpty()) {
            return "Other";
        }
        return label.charAt(0) + label.substring(1).toLowerCase();
    }

    /** Compact JSON of just the numbers -- this is what Nemotron is asked to read. */
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("company", slug);
        o.put("openRoles", openRoles);
        o.put("postedLast30Days", newLast30);
        o.put("postedPrior30Days", prev30);
        o.put("closedLast30Days", closedLast30);
        o.put("velocityPercent", velocity);
        o.put("trend", trend);
        o.put("topLocation", topLocation);
        o.put("scrapesOnFile", runs);
        o.put("functionMix", new JSONObject(functionMix));
        o.put("seniorityMix", new JSONObject(seniorityMix));
        o.put("countryMix", new JSONObject(countryMix));
        return o;
    }

    /** Stable fingerprint of the numbers, so a cached insight can be invalidated. */
    public String hash() {
        return Integer.toHexString(
                (slug + openRoles + newLast30 + prev30 + closedLast30 + topFunction).hashCode());
    }

    /** The investor-card payload the frontend consumes. */
    public JSONObject toCardJson(String read, String insightSource) {
        JSONObject o = new JSONObject();
        o.put("id", slug);
        o.put("company", display());
        o.put("sector", pretty(topFunction) + "-led");
        o.put("velocity", velocity);
        o.put("openRoles", openRoles);
        o.put("newThisMonth", newLast30);
        o.put("closedThisMonth", closedLast30);
        o.put("topLocation", topLocation);
        o.put("trend", trend);
        o.put("read", read);
        o.put("insightSource", insightSource);

        JSONArray mix = new JSONArray();
        for (Map.Entry<String, Double> e : topMix(4)) {
            mix.put(new JSONObject().put("label", e.getKey()).put("share", e.getValue()));
        }
        o.put("mix", mix);
        return o;
    }

    /** "stripe" -> "Stripe" */
    public String display() {
        return slug == null || slug.isEmpty()
                ? "?"
                : Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
    }
}
