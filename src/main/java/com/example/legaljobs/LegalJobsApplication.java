package com.example.legaljobs;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class LegalJobsApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        Matter matter = new Matter("matter-104", "Northstar Finance", "signed-doc-104", System.currentTimeMillis() / 1000 + 3600);
        LegalJobService service = new LegalJobService(Clock.systemUTC());
        InfraiClient infrai = new InfraiClient(HttpClient.newHttpClient(), key);
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                JobDecision decision = service.followUp(matter);
                if (!decision.accepted()) infrai.capture("deadline follow-up rejected", decision.reason(), decision.reason(), matter.matterId(), decision.jobName());
                System.out.println(decision);
            } catch (Exception error) {
                try { infrai.capture("scheduled legal job failed", error.getMessage(), error.toString(), matter.matterId(), "deadline-follow-up"); }
                catch (Exception captureError) { captureError.printStackTrace(); }
            }
        }, 0, TimeUnit.SECONDS);
        scheduler.shutdown();
    }
}
