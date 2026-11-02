package com.example.legaljobs;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class LegalJobServiceTest {
    public static void main(String[] args) {
        Clock now = Clock.fixed(Instant.ofEpochSecond(1_000), ZoneOffset.UTC);
        LegalJobService service = new LegalJobService(now);
        JobDecision accepted = service.followUp(new Matter("m-1", "Client", "doc-1", 1_001));
        JobDecision rejected = service.followUp(new Matter("m-2", "Client", "", 1_001));
        if (!accepted.accepted() || rejected.accepted()) throw new AssertionError("deadline decision is incorrect");
        System.out.println("legal job decision test passed");
    }
}
