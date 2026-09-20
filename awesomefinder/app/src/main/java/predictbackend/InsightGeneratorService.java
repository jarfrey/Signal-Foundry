package predictbackend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Turns a company's computed statistics into a short analyst read, using
 * NVIDIA-hosted Nemotron.
 *
 * The model is only ever shown numbers we derived ourselves from the job
 * boards -- it is doing interpretation, not arithmetic and not retrieval.
 * If the key is missing or the call fails we fall back to a deterministic
 * sentence built from the same numbers, and label it as such so the UI can
 * be honest about which reads are model-generated.
 */
public class InsightGeneratorService {

    private static final String NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL = "nvidia/nemotron-3-ultra-550b-a55b";

    /**
     * Nemotron 3 is a reasoning model: max_tokens has to cover its internal
     * reasoning as well as the answer. At 700 the larger prompts spent the
     * whole budget reasoning and the JSON came back truncated or empty.
     */
    private static final int MAX_TOKENS = 3000;

    /** Cap on how many detected patterns go into one prompt. */
    private static final int MAX_PATTERNS = 6;

    private static final String SYSTEM_PROMPT =
            "You are a corporate intelligence engine. You are given statistics derived from a "
            + "company's public job postings: how many roles are open, how many were posted in the "
            + "last 30 days versus the 30 days before, how many came down, and the mix by function, "
            + "seniority and country. Explain what the hiring pattern suggests about the company's "
            + "current priorities. Be concrete and cite the numbers you were given. Do not invent "
            + "funding rounds, revenue, headcount or any fact not present in the input. If the "
            + "sample is small, say the signal is weak. "
            + "Respond with JSON only, in exactly this shape: "
            + "{\"headline\": \"<=10 words\", \"read\": \"2-3 sentences\", "
            + "\"bullets\": [\"<=20 words\", \"<=20 words\", \"<=20 words\"]}";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final String apiKey;

    public InsightGeneratorService() {
        this(System.getenv("NEMOTRON_API_KEY"));
    }

    public InsightGeneratorService(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** What we hand back to callers: the prose plus where it came from. */
    public record Insight(String headline, String read, List<String> bullets, String source) {

        public JSONObject toJson() {
            return new JSONObject()
                    .put("headline", headline)
                    .put("read", read)
                    .put("bullets", new JSONArray(bullets))
                    .put("source", source);
        }
    }

    /**
     * Generate the read for one company, reusing the cached copy when the
     * underlying numbers have not moved.
     */
    public Insight forCompany(Db db, CompanyStats stats, List<Signal> signals) {
        String hash = stats.hash();

        try {
            String[] cached = db.cachedInsight(stats.companyId);
            boolean fresh = cached != null && hash.equals(cached[2]) && cached[0] != null;
            // A cached fallback was written because the model was unavailable
            // at the time. Once a key is present, retry rather than serving
            // the computed summary forever -- the stats hash alone would
            // never invalidate it.
            boolean staleFallback = fresh && "fallback".equals(cached[1]) && hasKey();
            if (fresh && !staleFallback) {
                return fromJson(new JSONObject(cached[0]), cached[1]);
            }
        } catch (Exception e) {
            System.err.println("  insight cache read failed for " + stats.slug + ": " + e.getMessage());
        }

        Insight insight = generate(stats, signals);

        try {
            db.saveInsight(stats.companyId, insight.headline(), insight.toJson().toString(),
                    new JSONArray(insight.bullets()).toString(), MODEL, insight.source(), hash);
        } catch (Exception e) {
            System.err.println("  insight cache write failed for " + stats.slug + ": " + e.getMessage());
        }

        return insight;
    }

    /** Call the model, falling back to the rule-based read on any failure. */
    public Insight generate(CompanyStats stats, List<Signal> signals) {
        if (!hasKey()) {
            return fallback(stats, "no NEMOTRON_API_KEY set");
        }
        try {
            String content = callNemotron(buildUserPayload(stats, signals));
            JSONObject parsed = new JSONObject(stripFence(content));
            return fromJson(parsed, "nemotron");
        } catch (Exception e) {
            System.err.println("  Nemotron call failed for " + stats.slug + ": " + e.getMessage());
            return fallback(stats, e.getMessage());
        }
    }

    /** The statistics block the model is asked to interpret. */
    private String buildUserPayload(CompanyStats stats, List<Signal> signals) {
        JSONObject payload = stats.toJson();

        // Only the strongest few patterns. Convergence fires once per peer
        // company, so an unfiltered list is dozens of near-identical claims
        // that crowd out the statistics and inflate reasoning length.
        List<Signal> relevant = new java.util.ArrayList<>();
        java.util.Set<String> seenClaims = new java.util.HashSet<>();
        for (Signal s : signals) {
            boolean mine = s.company == null || s.company.isEmpty() || s.company.equals(stats.slug);
            if (mine && seenClaims.add(s.kind + "|" + s.claim)) {
                relevant.add(s);
            }
        }
        relevant.sort((a, b) -> Double.compare(b.confidence, a.confidence));

        JSONArray detected = new JSONArray();
        for (Signal s : relevant.subList(0, Math.min(MAX_PATTERNS, relevant.size()))) {
            detected.put(new JSONObject()
                    .put("kind", s.kind)
                    .put("claim", s.claim)
                    .put("confidence", s.confidence));
        }
        payload.put("detectedPatterns", detected);

        JSONArray sampleTitles = new JSONArray();
        for (Signal s : signals) {
            for (Posting p : s.evidence) {
                if (sampleTitles.length() < 8 && stats.slug.equals(p.company)) {
                    sampleTitles.put(p.title);
                }
            }
        }
        payload.put("sampleOpenRoles", sampleTitles);

        return payload.toString();
    }

    private String callNemotron(String statsJson) throws Exception {
        // Built with org.json so the stats payload is escaped properly rather
        // than string-formatted into the request body.
        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("temperature", 0.2)
                .put("max_tokens", MAX_TOKENS)
                .put("response_format", new JSONObject().put("type", "json_object"))
                .put("messages", new JSONArray()
                        .put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                        .put(new JSONObject().put("role", "user").put("content", statsJson)));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NVIDIA_ENDPOINT))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": "
                    + response.body().substring(0, Math.min(300, response.body().length())));
        }
        return extractContent(response.body());
    }

    /**
     * Pull the assistant message out of the OpenAI-style envelope NVIDIA
     * returns. The previous version handed the whole envelope to the frontend.
     */
    static String extractContent(String envelope) {
        JSONObject root = new JSONObject(envelope);

        if (root.has("error") && !root.isNull("error")) {
            throw new RuntimeException("API error: " + root.get("error"));
        }

        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("no choices in response");
        }
        JSONObject choice = choices.getJSONObject(0);
        String content = choice.getJSONObject("message").optString("content", "");
        String finish = choice.optString("finish_reason", "");

        if (content.isBlank()) {
            throw new RuntimeException("length".equals(finish)
                    ? "ran out of tokens before writing an answer (raise MAX_TOKENS)"
                    : "empty content in response (finish_reason=" + finish + ")");
        }
        if ("length".equals(finish)) {
            throw new RuntimeException("answer truncated at the token limit (raise MAX_TOKENS)");
        }
        return content;
    }

    /** Models sometimes wrap JSON in a markdown fence even when told not to. */
    static String stripFence(String content) {
        String t = content.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                t = t.substring(firstNewline + 1, lastFence).trim();
            }
        }
        // Be forgiving about leading prose before the object.
        int brace = t.indexOf('{');
        int close = t.lastIndexOf('}');
        if (brace > 0 && close > brace) {
            t = t.substring(brace, close + 1);
        }
        return t;
    }

    private static Insight fromJson(JSONObject o, String source) {
        JSONArray bulletsJson = o.optJSONArray("bullets");
        List<String> bullets = new java.util.ArrayList<>();
        if (bulletsJson != null) {
            for (int i = 0; i < bulletsJson.length(); i++) {
                bullets.add(bulletsJson.getString(i));
            }
        }
        // Accept either key; the model occasionally answers with "insights".
        String read = o.optString("read", o.optString("body", ""));
        if (read.isBlank() && !bullets.isEmpty()) {
            read = String.join(" ", bullets);
        }
        return new Insight(o.optString("headline", ""), read, bullets, source);
    }

    /**
     * Deterministic read from the same numbers, used when the model is
     * unavailable. Always labelled "fallback" so the UI can mark it.
     */
    static Insight fallback(CompanyStats s, String why) {
        String direction = switch (s.trend) {
            case "Growing" -> "accelerating";
            case "Cooling" -> "pulling back";
            default -> "holding steady";
        };

        String headline = s.display() + " is " + direction;

        StringBuilder read = new StringBuilder();
        read.append(s.display()).append(" has ").append(s.openRoles)
            .append(" open role").append(s.openRoles == 1 ? "" : "s")
            .append(", ").append(s.newLast30).append(" posted in the last 30 days against ")
            .append(s.prev30).append(" in the 30 days before that (")
            .append(s.velocity > 0 ? "+" : "").append((long) s.velocity).append("%). ");

        if (!s.functionMix.isEmpty()) {
            read.append("Hiring is weighted toward ")
                .append(CompanyStats.pretty(s.topFunction))
                .append(", concentrated in ").append(s.topLocation).append(". ");
        }
        if (s.closedLast30 > 0) {
            read.append(s.closedLast30).append(" role")
                .append(s.closedLast30 == 1 ? " came" : "s came")
                .append(" down in the same window. ");
        }
        if (s.newLast30 + s.prev30 < 4) {
            read.append("Too few recent postings to read much into the direction.");
        }

        List<String> bullets = List.of(
                s.openRoles + " open roles, " + s.newLast30 + " new in 30 days",
                "Largest function: " + CompanyStats.pretty(s.topFunction),
                "Concentrated in " + s.topLocation);

        System.err.println("  using fallback read for " + s.slug + " (" + why + ")");
        return new Insight(headline, read.toString().trim(), bullets, "fallback");
    }

    /** Evidence postings for a signal, as JSON. Used when persisting signals. */
    public static String evidenceJson(Signal s) {
        JSONArray ev = new JSONArray();
        for (Posting p : s.evidence) {
            ev.put(new JSONObject()
                    .put("postingId", p.postingId)
                    .put("title", p.title == null ? "" : p.title)
                    .put("location", p.locationRaw == null ? "" : p.locationRaw)
                    .put("url", p.url == null ? "" : p.url));
        }
        return ev.toString();
    }

    /** All signals for one company, as JSON. */
    public static String signalsJson(String company, List<Signal> signals) {
        JSONArray arr = new JSONArray();
        for (Signal s : signals) {
            if (s.company != null && !s.company.isEmpty() && !s.company.equals(company)) {
                continue;
            }
            arr.put(new JSONObject()
                    .put("kind", s.kind)
                    .put("claim", s.claim)
                    .put("confidence", s.confidence)
                    .put("evidence", new JSONArray(evidenceJson(s))));
        }
        return arr.toString();
    }
}
