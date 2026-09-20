package predictbackend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The companies we track, as (source, handle) pairs.
 *
 * Defaults to a built-in list of boards verified to respond. Override by
 * dropping a watchlist.txt next to the database with one "source handle"
 * per line, so the set can change without a rebuild.
 */
public final class Watchlist {

    public record Entry(String source, String handle) {
    }

    private static final List<Entry> DEFAULTS = List.of(
            new Entry("greenhouse", "stripe"),
            new Entry("greenhouse", "databricks"),
            new Entry("greenhouse", "figma"),
            new Entry("greenhouse", "discord"),
            new Entry("greenhouse", "robinhood"),
            new Entry("lever", "palantir"));

    private Watchlist() {
    }

    public static List<Entry> load() {
        Path file = Path.of(System.getProperty("watchlist.path", "watchlist.txt"));
        if (!Files.exists(file)) {
            return DEFAULTS;
        }

        List<Entry> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    out.add(new Entry(parts[0].toLowerCase(), parts[1]));
                }
            }
        } catch (Exception e) {
            System.err.println("could not read " + file + ", using defaults: " + e.getMessage());
            return DEFAULTS;
        }

        return out.isEmpty() ? DEFAULTS : out;
    }
}
