package predictbackend;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence. One file, signals.db, in the working directory.
 *
 * Why a database at all: a single scrape only tells you what is open right
 * now. Storing every scrape gives first_seen_at / last_seen_at per posting,
 * which is what lets us say a role is new, still open, or closed -- and
 * therefore whether a company is hiring more or less than normal.
 */
public class Db implements AutoCloseable {

    private final Connection conn;

    public Db(String path) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    public static Db open() throws SQLException {
        Db db = new Db(System.getProperty("db.path", "signals.db"));
        db.migrate();
        return db;
    }

    /** Run schema.sql. Every statement in it is idempotent. */
    public void migrate() throws SQLException {
        String sql;
        try (InputStream in = Db.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql missing from resources");
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SQLException("could not read schema.sql", e);
        }

        // Strip comments, then split on ';' -- the driver runs one statement
        // per execute() call.
        StringBuilder cleaned = new StringBuilder();
        for (String line : sql.split("\n")) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }

        try (Statement st = conn.createStatement()) {
            for (String statement : cleaned.toString().split(";")) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
        }
    }

    // ---- writes --------------------------------------------------------

    /** Insert the company if we have not seen its slug before; return its id. */
    public int upsertCompany(String slug, String displayName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO company (slug, display_name, created_at) VALUES (?, ?, ?) "
                        + "ON CONFLICT(slug) DO UPDATE SET "
                        + "display_name = COALESCE(excluded.display_name, display_name)")) {
            ps.setString(1, slug);
            ps.setString(2, displayName);
            ps.setString(3, Dates.nowIso());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT company_id FROM company WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int startRun(String source, String handle) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO scrape_run (source, handle, started_at, status) VALUES (?, ?, ?, 'RUNNING')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, source);
            ps.setString(2, handle);
            ps.setString(3, Dates.nowIso());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void finishRun(int runId, String status, int seen, int added, String error) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE scrape_run SET finished_at = ?, status = ?, postings_seen = ?, "
                        + "postings_new = ?, error_msg = ? WHERE run_id = ?")) {
            ps.setString(1, Dates.nowIso());
            ps.setString(2, status);
            ps.setInt(3, seen);
            ps.setInt(4, added);
            ps.setString(5, error);
            ps.setInt(6, runId);
            ps.executeUpdate();
        }
    }

    /**
     * Write one scrape's postings.
     *
     * A posting already on file keeps its original first_seen_at and only has
     * last_seen_at bumped -- that preserved date is what makes the historical
     * comparison possible. Anything belonging to this board that we did NOT
     * see this run is marked closed.
     *
     * @return how many postings were new this run
     */
    public int saveSnapshot(int companyId, int runId, String source, String handle,
                            List<Posting> postings) throws SQLException {
        String now = Dates.nowIso();
        int added = 0;

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO posting (posting_id, source, handle, external_id, title, location_raw, "
                            + "department_raw, url, posted_at, updated_at, first_seen_at, last_seen_at, "
                            + "is_open, closed_at, content_hash, company_id, first_run_id, "
                            + "seniority, function, country, city) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1,NULL,?,?,?,?,?,?,?) "
                            + "ON CONFLICT(posting_id) DO UPDATE SET "
                            + "  title = excluded.title, location_raw = excluded.location_raw, "
                            + "  department_raw = excluded.department_raw, url = excluded.url, "
                            + "  updated_at = excluded.updated_at, last_seen_at = excluded.last_seen_at, "
                            + "  is_open = 1, closed_at = NULL, content_hash = excluded.content_hash, "
                            + "  seniority = excluded.seniority, function = excluded.function, "
                            + "  country = excluded.country, city = excluded.city")) {

                for (Posting p : postings) {
                    boolean isNew = !exists(p.postingId);

                    ps.setString(1, p.postingId);
                    ps.setString(2, p.source);
                    ps.setString(3, p.company);
                    ps.setString(4, p.externalId);
                    ps.setString(5, p.title);
                    ps.setString(6, p.locationRaw);
                    ps.setString(7, p.departmentRaw);
                    ps.setString(8, p.url);
                    ps.setString(9, p.postedAt);
                    ps.setString(10, p.updatedAt);
                    ps.setString(11, now);   // first_seen_at, left alone on conflict
                    ps.setString(12, now);   // last_seen_at
                    ps.setString(13, Integer.toHexString((p.title + "|" + p.locationRaw).hashCode()));
                    ps.setInt(14, companyId);
                    ps.setInt(15, runId);
                    ps.setString(16, p.seniority);
                    ps.setString(17, p.function);
                    ps.setString(18, p.country);
                    ps.setString(19, p.city);
                    ps.addBatch();

                    if (isNew) {
                        added++;
                    }
                }
                ps.executeBatch();
            }

            // Anything from this board not in this snapshot has come down.
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE posting SET is_open = 0, closed_at = ? "
                            + "WHERE source = ? AND handle = ? AND is_open = 1 AND last_seen_at < ?")) {
                ps.setString(1, now);
                ps.setString(2, source);
                ps.setString(3, handle);
                ps.setString(4, now);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }

        return added;
    }

    private boolean exists(String postingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM posting WHERE posting_id = ?")) {
            ps.setString(1, postingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void saveInsight(int companyId, String headline, String body, String bulletsJson,
                            String model, String source, String statsHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO insight (company_id, headline, body, bullets_json, model, source, "
                        + "stats_hash, generated_at) VALUES (?,?,?,?,?,?,?,?) "
                        + "ON CONFLICT(company_id) DO UPDATE SET headline = excluded.headline, "
                        + "body = excluded.body, bullets_json = excluded.bullets_json, "
                        + "model = excluded.model, source = excluded.source, "
                        + "stats_hash = excluded.stats_hash, generated_at = excluded.generated_at")) {
            ps.setInt(1, companyId);
            ps.setString(2, headline);
            ps.setString(3, body);
            ps.setString(4, bulletsJson);
            ps.setString(5, model);
            ps.setString(6, source);
            ps.setString(7, statsHash);
            ps.setString(8, Dates.nowIso());
            ps.executeUpdate();
        }
    }

    /** Cached insight for a company, or null. Index 0 body, 1 source, 2 stats_hash. */
    public String[] cachedInsight(int companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT body, source, stats_hash FROM insight WHERE company_id = ?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new String[] { rs.getString(1), rs.getString(2), rs.getString(3) };
            }
        }
    }

    public void replaceSignals(List<Signal> signals) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM signal");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO signal (kind, company_slug, claim, confidence, evidence_json, created_at) "
                        + "VALUES (?,?,?,?,?,?)")) {
            for (Signal s : signals) {
                ps.setString(1, s.kind);
                ps.setString(2, s.company);
                ps.setString(3, s.claim);
                ps.setDouble(4, s.confidence);
                ps.setString(5, InsightGeneratorService.evidenceJson(s));
                ps.setString(6, Dates.nowIso());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---- reads ---------------------------------------------------------

    public List<String> companySlugs() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT slug FROM company ORDER BY slug")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** Open postings, newest first. Backs GET /api/jobs. */
    public List<Posting> openPostings(int limit) throws SQLException {
        List<Posting> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT posting_id, source, handle, external_id, title, location_raw, department_raw, "
                        + "url, posted_at, updated_at, seniority, function, country, city "
                        + "FROM posting WHERE is_open = 1 "
                        + "ORDER BY COALESCE(posted_at, first_seen_at) DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Posting p = new Posting();
                    p.postingId = rs.getString(1);
                    p.source = rs.getString(2);
                    p.company = rs.getString(3);
                    p.externalId = rs.getString(4);
                    p.title = rs.getString(5);
                    p.locationRaw = rs.getString(6);
                    p.departmentRaw = rs.getString(7);
                    p.url = rs.getString(8);
                    p.postedAt = rs.getString(9);
                    p.updatedAt = rs.getString(10);
                    p.seniority = rs.getString(11);
                    p.function = rs.getString(12);
                    p.country = rs.getString(13);
                    p.city = rs.getString(14);
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** Every open posting, used to feed the cross-company Detector rules. */
    public List<Posting> allOpenPostings() throws SQLException {
        return openPostings(Integer.MAX_VALUE);
    }

    public int companyId(String slug) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT company_id FROM company WHERE slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public int countOpen(String slug) throws SQLException {
        return scalar("SELECT COUNT(*) FROM posting p JOIN company c USING (company_id) "
                + "WHERE c.slug = ? AND p.is_open = 1", slug);
    }

    /**
     * Postings for a company first published inside a window measured in days
     * back from now. This is the core "is hiring up or down" measurement:
     * countPostedBetween(slug, 30, 0) against countPostedBetween(slug, 60, 30).
     */
    public int countPostedBetween(String slug, int fromDaysAgo, int toDaysAgo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM posting p JOIN company c USING (company_id) "
                        + "WHERE c.slug = ? AND COALESCE(p.posted_at, p.first_seen_at) >= ? "
                        + "AND COALESCE(p.posted_at, p.first_seen_at) < ?")) {
            ps.setString(1, slug);
            ps.setString(2, Dates.daysAgo(fromDaysAgo));
            ps.setString(3, Dates.daysAgo(toDaysAgo));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Roles this company has taken down in the last N days -- the cooling signal. */
    public int countClosedSince(String slug, int days) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM posting p JOIN company c USING (company_id) "
                        + "WHERE c.slug = ? AND p.is_open = 0 AND p.closed_at >= ?")) {
            ps.setString(1, slug);
            ps.setString(2, Dates.daysAgo(days));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Open-role counts grouped by a label column, biggest first. */
    public Map<String, Integer> breakdown(String slug, String column) throws SQLException {
        if (!List.of("function", "seniority", "country", "city", "source").contains(column)) {
            throw new IllegalArgumentException("not a groupable column: " + column);
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(NULLIF(p." + column + ", ''), 'UNKNOWN') AS bucket, COUNT(*) AS n "
                        + "FROM posting p JOIN company c USING (company_id) "
                        + "WHERE c.slug = ? AND p.is_open = 1 GROUP BY bucket ORDER BY n DESC")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return out;
    }

    /** Successful scrapes on file for a company -- how much history backs a claim. */
    public int runCount(String slug) throws SQLException {
        return scalar("SELECT COUNT(*) FROM scrape_run WHERE handle = ? AND status = 'OK'", slug);
    }

    private int scalar(String sql, String arg) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
