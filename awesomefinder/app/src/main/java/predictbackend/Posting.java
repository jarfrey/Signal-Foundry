// package predictbackend;

// public class Posting {

//     public String postingId;       // "greenhouse:stripe:12345"
//     public String source;          // "greenhouse" | "lever"
//     public String company;         // ATS handle, e.g. "stripe"
//     public String externalId;      // their id
//     public String title;
//     public String locationRaw;     // "Remote - US", "London, UK"
//     public String departmentRaw;
//     public String url;
//     public String postedAt;        // ISO-8601 string, or null

//     // label fields
//     public String seniority;
//     public String function;
//     public String country;
//     public String city;

//     public String toString() {
//         return String.format("[%s] %s | %s | %s",
//                 company, title, locationRaw, postedAt);
//     }

// }