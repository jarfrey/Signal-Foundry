package predictbackend;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * Lever job board reader.
 *
 * Payload shape (verified against api.lever.co) is a bare top-level array:
 *   [{ "id": "ac978161-...",
 *      "text": "Administrative Business Partner",
 *      "hostedUrl": "https://jobs.lever.co/palantir/ac978161-...",
 *      "createdAt": 1711403416463,
 *      "country": "GB",
 *      "categories": {"location": "London, United Kingdom", "team": "Administrative"} }]
 *
 * Note Lever names its title field "text" and Greenhouse names its "title" --
 * these two were previously swapped between the readers.
 */
public class Lever {

    private static final String BASE = "https://api.lever.co/v0/postings/";

    public List<Posting> fetch(String handle) throws Exception {
        String body = Http.get(BASE + handle + "?mode=json");
        return parse(handle, body);
    }

    /** Split out from fetch() so the parsing can be tested without the network. */
    public List<Posting> parse(String handle, String body) {
        List<Posting> postings = new ArrayList<>();

        for (JSONObject job : new Parser(body).jobs()) {
            Posting p = new Posting();

            p.source = "lever";
            p.company = handle;
            p.externalId = Parser.str(job, "id");
            p.postingId = "lever:" + handle + ":" + p.externalId;
            p.title = Parser.str(job, "text");
            p.url = Parser.str(job, "hostedUrl");
            p.locationRaw = Parser.nested(job, "categories", "location");
            p.departmentRaw = Parser.nested(job, "categories", "team");
            p.postedAt = Dates.fromEpochMillis(Parser.num(job, "createdAt", 0));
            p.updatedAt = p.postedAt;

            // Lever already resolves an ISO country code for most postings, so
            // prefer it over guessing from the location string.
            p.countryHint = Parser.str(job, "country");

            if (!p.title.isEmpty() && !p.externalId.isEmpty()) {
                postings.add(p);
            }
        }

        return postings;
    }
}
