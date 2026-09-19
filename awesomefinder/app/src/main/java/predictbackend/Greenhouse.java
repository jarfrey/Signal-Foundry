package predictbackend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Greenhouse {

    /** Download one company's job board and return the postings. */
    public static List<Posting> fetch(String handle) throws Exception {

        // 1. download
        String url = "https://boards-api.greenhouse.io/v1/boards/" + handle + "/jobs";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "steelhacks-project")
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(handle + " gave HTTP " + response.statusCode());
        }

        // 2. one chunk per job
        List<String> chunks = Parser.split(response.body(), "\"chunk\"");

        // 3. pull the fields out of each chunk
        List<Posting> postings = new ArrayList<>();

        for (String chunk : chunks) {
            Posting p = new Posting();

            p.source      = "greenhouse";
            p.company     = handle;
            p.externalId  = Parser.number(chunk, "\"id\":");
            p.postingId   = "greenhouse:" + handle + ":" + p.externalId;
            p.title       = Parser.text(chunk, "\"title\":");
            p.url         = Parser.text(chunk, "\"absolute_url\":");
            p.locationRaw = Parser.text(chunk, "\"name\":");
            p.postedAt    = Parser.text(chunk, "\"updated_at\":"); //this is updated not posted

            if (!p.title.isEmpty()) {
                postings.add(p);
            }
        }

        return postings;
    }
}