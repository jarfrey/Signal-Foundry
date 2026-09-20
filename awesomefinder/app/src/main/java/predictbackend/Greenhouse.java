package predictbackend;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * Greenhouse job board reader.
 *
 * Payload shape (verified against boards-api.greenhouse.io):
 * {"jobs": [{ "id": 8172487,
 * "title": "Abuse Investigator",
 * "absolute_url": "https://...",
 * "location": {"name": "Dublin"},
 * "first_published": "2026-09-03T13:30:34-04:00",
 * "updated_at": "2026-09-12T13:25:25-04:00",
 * "departments": [{"name": "Risk"}] }], "meta": {...}}
 */
public class Greenhouse {

    private static final String BASE = "https://boards-api.greenhouse.io/v1/boards/";

    /** Download one company's job board and return the postings. */
    public List<Posting> fetch(String handle) throws Exception {
        String body = Http.get(BASE + handle + "/jobs");
        return parse(handle, body);
    }

    /** Split out from fetch() so the parsing can be tested without the network. */
    public List<Posting> parse(String handle, String body) {
        List<Posting> postings = new ArrayList<>();

        for (JSONObject job : new Parser(body).jobs()) {
            Posting p = new Posting();

            p.source = "greenhouse";
            p.company = handle;
            p.externalId = Parser.str(job, "id");
            p.postingId = "greenhouse:" + handle + ":" + p.externalId;
            p.title = Parser.str(job, "title");
            p.url = Parser.str(job, "absolute_url");
            p.locationRaw = Parser.nested(job, "location", "name");
            p.departmentRaw = Parser.namesOf(job, "departments");

            // first_published is the real posting date. Boards that omit it
            // still give updated_at, which is a usable floor for recency.
            p.postedAt = Dates.toIso(Parser.str(job, "first_published"));
            p.updatedAt = Dates.toIso(Parser.str(job, "updated_at"));
            if (p.postedAt == null) {
                p.postedAt = p.updatedAt;
            }

            if (!p.title.isEmpty() && !p.externalId.isEmpty()) {
                postings.add(p);
            }
        }

        return postings;
    }
}
