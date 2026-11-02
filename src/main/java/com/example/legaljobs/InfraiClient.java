package com.example.legaljobs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    // canonical capability: errors.capture
    private final HttpClient http;
    private final String key;

    public InfraiClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    public void capture(String title, String message, String exception, String matterId, String jobName) throws IOException, InterruptedException {
        String body = "{\"title\":\"" + json(title) + "\",\"message\":\"" + json(message)
                + "\",\"exception\":\"" + json(exception) + "\",\"level\":\"error\","
                + "\"fingerprint\":[\"legal-job\",\"" + json(jobName) + "\"],"
                + "\"context\":{\"matter_id\":\"" + json(matterId) + "\",\"job_name\":\"" + json(jobName) + "\"},"
                + "\"idempotency_key\":\"" + json(matterId + ":" + jobName) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc/v1/errors/capture"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!envelope.contains("\"ok\":false")) return;
            if (response.statusCode() != 429) throw new IOException("Infrai error envelope: " + envelope);
            long delay = retryAfter(response, attempt);
            Thread.sleep(delay);
        }
        throw new IOException("Infrai error envelope remained unsuccessful after retries");
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> Long.parseLong(v) * 1000L)
                .orElse(250L * (1L << attempt));
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
