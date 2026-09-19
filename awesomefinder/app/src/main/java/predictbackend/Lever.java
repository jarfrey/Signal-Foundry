// package predictbackend;

// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.time.Instant;
// import java.util.ArrayList;
// import java.util.List;


// public class Lever {

<<<<<<< HEAD
//     public static List<Posting> fetch(String handle) throws Exception {
=======
    public List<Posting> fetch(String handle) throws Exception {
>>>>>>> 299aa1af539cd2e049d3deae7975b16a68fe3fae

//         // 1. download
//         String url = "https://api.lever.co/v0/postings/" + handle + "?mode=json";

//         HttpClient client = HttpClient.newHttpClient();
//         HttpRequest request = HttpRequest.newBuilder()
//                 .uri(URI.create(url))
//                 .header("User-Agent", "steelhacks-project")
//                 .header("Accept", "application/json")
//                 .build();
//         HttpResponse<String> response =
//                 client.send(request, HttpResponse.BodyHandlers.ofString());

//         if (response.statusCode() != 200) {
//             throw new RuntimeException(handle + " gave HTTP " + response.statusCode());
//         }

<<<<<<< HEAD
//         // 2. one chunk per job

//         List<String> chunks = Parser.split(response.body(), "\"chunk\"");
=======
        // TODO: replace with path to json file, using the sample file I have for now
        Parser parse = new Parser(response.body());

        // 2. one chunk per job

        List<String> chunks = parse.split_String();
>>>>>>> 299aa1af539cd2e049d3deae7975b16a68fe3fae

//         // 3. pull the fields out of each chunk

//         List<Posting> postings = new ArrayList<>();

//         for (String chunk : chunks) {

//             Posting p = new Posting();

<<<<<<< HEAD
//             p.source      = "lever";
//             p.company     = handle;
//             p.title       = Parser.text(chunk, "\"text\":");
//             p.locationRaw = Parser.text(chunk, "\"location\":");
//             p.postedAt    = toDate(Parser.number(chunk, "\"createdAt\":"));
=======
            p.source      = "lever";
            p.company     = handle;
            p.title       = parse.getString(chunk, "title");
            p.locationRaw = parse.getString(chunk, "location");
            p.postedAt    = toDate(parse.getString(chunk, "first_published"));
>>>>>>> 299aa1af539cd2e049d3deae7975b16a68fe3fae

//             // the id sits right before hostedUrl, so grab it from the URL itself
//             p.url        = firstQuoted(chunk);
//             p.externalId = idFromUrl(p.url);
//             p.postingId  = "lever:" + handle + ":" + p.externalId;

//             if (!p.title.isEmpty()) {
//                 postings.add(p);
//             }

//         }

//         return postings;

//     }

//     private static String firstQuoted(String chunk) {
//         int open = chunk.indexOf('"', chunk.indexOf(':') + 1);
//         if (open < 0) return "";
//         int close = chunk.indexOf('"', open + 1);
//         if (close < 0) return "";
//         return chunk.substring(open + 1, close);
//     }

//     private static String idFromUrl(String url) {
//         if (url.isEmpty()) return "";
//         int slash = url.lastIndexOf('/');
//         return slash < 0 ? "" : url.substring(slash + 1);
//     }

//     private static String toDate(String millis) {
//         if (millis.isEmpty()) return null;
//         try {
//             long ms = Long.parseLong(millis);
//             return ms <= 0 ? null : Instant.ofEpochMilli(ms).toString();
//         } catch (NumberFormatException e) {
//             return null;
//         }
//     }

// }