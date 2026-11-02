package com.example.legaljobs;

import java.time.Clock;

public final class LegalJobService {
    private final Clock clock;

    public LegalJobService(Clock clock) {
        this.clock = clock;
    }

    public JobDecision followUp(Matter matter) {
        if (matter.signedDocumentId() == null || matter.signedDocumentId().isBlank()) {
            return new JobDecision("deadline-follow-up", false, "signed document is required");
        }
        long remaining = matter.deadlineEpochSeconds() - clock.instant().getEpochSecond();
        if (remaining < 0) {
            return new JobDecision("deadline-follow-up", false, "matter deadline has passed");
        }
        return new JobDecision("deadline-follow-up", true, "follow-up queued for " + matter.matterId());
    }

    public JobDecision deliverSignedDocument(Matter matter) {
        boolean accepted = matter.signedDocumentId() != null && !matter.signedDocumentId().isBlank();
        return new JobDecision("signed-document-delivery", accepted,
                accepted ? "delivery recorded for " + matter.matterId() : "signed document is required");
    }
}
