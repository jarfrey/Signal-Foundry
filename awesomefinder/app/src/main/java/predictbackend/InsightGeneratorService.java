package predictbackend;// jack

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InsightGeneratorService {

    private static final String API_KEY = System.getenv("NVIDIA_API_KEY");
    private static final String NVIDIA_ENDPOINT = "https://integrate.api.nvidia.com/v1/chat/completions";

    public String generateCompanyInsight(String companyStatsJson) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // System prompt instructing Nemotron to generate short, bulleted summaries
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
                  "nvext": {
                    "guided_json": {
                      "type": "object",
                      "properties": {
                        "headline": { "type": "string" },
                        "key_takeaways": {
                          "type": "array",
                          "items": { "type": "string" }
                        },
                        "trend_label": { "type": "string", "enum": ["Agressively Hiring", "Hiring Freeze", "Re-Emerging", "Stable"] }
                      },
                      "required": ["headline", "key_takeaways", "trend_label"]
                    }
                  }
                }
                """
                .formatted(companyStatsJson);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NVIDIA_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY) // add API key
                .POST(HttpRequest.BodyPublishers.ofString(prompt))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}