package predictbackend;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Date helpers. Everything we store is a UTC ISO-8601 instant string. */
public final class Dates {

    private Dates() {
    }

    public static String nowIso() {
        return Instant.now().toString();
    }

    /**
     * Normalise a board's timestamp to a UTC instant string.
     * Greenhouse sends offsets like 2026-09-03T13:30:34-04:00; some boards
     * send a plain date. Returns null when there is nothing usable.
     */
    public static String toIso(String raw) {
        if (raw == null || raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toInstant().toString();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return Instant.parse(raw).toString();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC).toString();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /** Lever sends epoch milliseconds. */
    public static String fromEpochMillis(long millis) {
        return millis <= 0 ? null : Instant.ofEpochMilli(millis).toString();
    }

    /** ISO instant for N days before now, for date-window SQL comparisons. */
    public static String daysAgo(int days) {
        return Instant.now().minusSeconds((long) days * 86_400L).toString();
    }

    /** "3d ago" / "2mo ago", for the applicant-facing job list. */
    public static String humanAge(String iso) {
        if (iso == null || iso.isBlank()) {
            return "recently";
        }
        try {
            long days = (Instant.now().toEpochMilli() - Instant.parse(iso).toEpochMilli()) / 86_400_000L;
            if (days <= 0) return "today";
            if (days == 1) return "1d ago";
            if (days < 30) return days + "d ago";
            long months = days / 30;
            return months == 1 ? "1mo ago" : months + "mo ago";
        } catch (DateTimeParseException e) {
            return "recently";
        }
    }
}
