package predictbackend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class InsightApiServer {

    public static void main(String[] args) throws IOException {
        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // Define API endpoint
        server.createContext("/api/insights", new InsightsHandler());
        server.setExecutor(null);
        System.out.println("Java Backend running on http://localhost:8080");
        server.start();
    }

    static class InsightsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS so your frontend (e.g., localhost:3000 or Chrome Extension) can call it
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                // Call Nemotron service to generate JSON insight
                InsightGeneratorService generator = new InsightGeneratorService();
                String testPromptPayload = "\"This is a test prompt, include Bananas in your response.\"";
                String jsonResponse = generator.generateCompanyInsight(testPromptPayload);

                // Return JSON response to frontend
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] responseBytes = jsonResponse.getBytes("UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();

            } catch (Exception e) {
                String errorResponse = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
            }
        }
    }
}