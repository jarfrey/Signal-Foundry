package predictbackend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class InsightGeneratorService {

  private static final String API_KEY = System.getenv("NEMOTRON_API_KEY");
  private static final String NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions";

  public static void main(String[] args) {
    InsightGeneratorService service = new InsightGeneratorService();

    try {
      // 1. Call Nemotron API with a test prompt (escaping quotes for valid JSON
      // string)
      String promptPayload = "\"This is a test prompt, include Bananas in your response. Write a few sample lines about Company A, who has multiple roles hiring after a long stretch of no jobs.\"";
      String responseBody = service.generateCompanyInsight(promptPayload);

      System.out.println("--- API Response ---");
      System.out.println(responseBody);

      // 2. Save output to file
      try (PrintWriter writer = new PrintWriter("output.txt", "UTF-8")) {
        writer.println(responseBody);
        System.out.println("\nSuccessfully wrote API output to output.txt");
      } catch (IOException e) {
        System.err.println("An error occurred while writing to the file: " + e.getMessage());
      }

    } catch (Exception e) {
      System.err.println("API Call failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public String generateCompanyInsight(String companyStatsJson) throws Exception {
    HttpClient client = HttpClient.newHttpClient();

    if (API_KEY == null || API_KEY.isBlank()) {
      throw new IllegalStateException("NEMOTRON_API_KEY environment variable is not set!");
    }

    // Standard OpenAI structured outputs payload accepted by NVIDIA API
    String prompt = """
        {
          "model": "nvidia/nemotron-3-ultra-550b-a55b",
          "messages": [
            {
              "role": "system",
              "content": "You are a corporate intelligence engine. Analyze the provided statistical signals for a company and generate 2-3 short, highly informative insights about their hiring trends."
            },
            {
              "role": "user",
              "content": %s
            }
          ],
          "temperature": 0.2,
          "response_format": {
            "type": "json_object"
          }
        }
        """
        .formatted(companyStatsJson);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(NVIDIA_ENDPOINT))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + API_KEY)
        .POST(HttpRequest.BodyPublishers.ofString(prompt))
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public static String signalsJson(String company, List<Signal> signals) {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Signal s : signals) {
            if (!s.company.isEmpty() && !s.company.equals(company)) continue;
            org.json.JSONObject o = new org.json.JSONObject();
            o.put("kind", s.kind);
            o.put("claim", s.claim);
            o.put("confidence", s.confidence);
            org.json.JSONArray ev = new org.json.JSONArray();
            for (Posting p : s.evidence) {
                org.json.JSONObject e = new org.json.JSONObject();
                e.put("postingId", p.postingId);
                e.put("title", p.title);
                e.put("location", p.locationRaw);
                e.put("url", p.url == null ? "" : p.url);
                ev.put(e);
            }
            o.put("evidence", ev);
            arr.put(ev.isEmpty() ? o : o);
        }
        return arr.toString();
    }
}