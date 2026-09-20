package predictbackend;

public class Posting {

    public String postingId;       // "greenhouse:stripe:12345"
    public String source;          // "greenhouse" | "lever"
    public String company;         // ATS handle, e.g. "stripe"
    public String externalId;      // their id
    public String title;
    public String locationRaw;     // "Remote - US", "London, UK"
    public String departmentRaw;
    public String url;
    public String postedAt;        // ISO-8601 UTC instant, or null
    public String updatedAt;       // ISO-8601 UTC instant, or null

    /** ISO country code supplied by the board itself (Lever does this). */
    public String countryHint;

    // label fields, filled in by Labeler
    public String seniority;
    public String function;
    public String country;
    public String city;

    public String toString() {
        return String.format("[%s] %s | %s | %s",
                company, title, locationRaw, postedAt);
    }
}
