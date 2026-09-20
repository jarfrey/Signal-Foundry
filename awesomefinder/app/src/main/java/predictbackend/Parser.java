package predictbackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Wraps a job-board JSON payload and hands back one object per posting.
 *
 * The two boards we read disagree on shape, so this normalises both:
 *   Greenhouse -> {"jobs": [ ... ], "meta": {...}}
 *   Lever      -> [ ... ]                (bare top-level array)
 */
public class Parser {

    private final JSONArray jobs;

    /** Parse a raw JSON payload, e.g. an HTTP response body. */
    public Parser(String rawJson) {
        this.jobs = extractJobs(rawJson);
    }

    /** Parse a JSON file from disk. Handy for offline demos and tests. */
    public static Parser fromFile(String filename) throws IOException {
        return new Parser(Files.readString(Path.of(filename)));
    }

    private static JSONArray extractJobs(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new JSONArray();
        }

        String trimmed = rawJson.trim();

        // Lever hands back a bare array.
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }

        JSONObject root = new JSONObject(trimmed);

        // Greenhouse nests under "jobs"; accept the usual alternatives too so a
        // new board can be dropped in without touching this class.
        for (String key : new String[] { "jobs", "results", "data", "postings" }) {
            if (root.has(key) && root.get(key) instanceof JSONArray array) {
                return array;
            }
        }

        throw new IllegalArgumentException(
                "no job array found in payload; top-level keys were " + root.keySet());
    }

    /** One JSONObject per posting. */
    public List<JSONObject> jobs() {
        List<JSONObject> out = new ArrayList<>(jobs.length());
        for (int i = 0; i < jobs.length(); i++) {
            out.add(jobs.getJSONObject(i));
        }
        return out;
    }

    public int count() {
        return jobs.length();
    }

    // ---- field helpers -------------------------------------------------
    // Boards leave fields out or set them to null, so every read is defensive:
    // a missing field yields "" rather than throwing or producing the string
    // "null", which is what the old getString(...) did.

    /** Read a string field, returning "" when absent or JSON null. */
    public static String str(JSONObject job, String key) {
        if (job == null || !job.has(key) || job.isNull(key)) {
            return "";
        }
        return job.get(key).toString().trim();
    }

    /** Read a field nested one level down, e.g. location.name. */
    public static String nested(JSONObject job, String outer, String key) {
        if (job == null || !job.has(outer) || job.isNull(outer)) {
            return "";
        }
        if (!(job.get(outer) instanceof JSONObject inner)) {
            return "";
        }
        return str(inner, key);
    }

    /** Read a long field, returning the fallback when absent or unparseable. */
    public static long num(JSONObject job, String key, long fallback) {
        String raw = str(job, key);
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Join the "name" of each entry in an array field, e.g. departments. */
    public static String namesOf(JSONObject job, String key) {
        if (job == null || !job.has(key) || job.isNull(key)) {
            return "";
        }
        if (!(job.get(key) instanceof JSONArray array)) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            if (array.get(i) instanceof JSONObject entry) {
                String name = str(entry, "name");
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return String.join(", ", names);
    }
}
