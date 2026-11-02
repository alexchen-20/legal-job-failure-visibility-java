package com.example.legaljobs;

public record Matter(String matterId, String clientName, String signedDocumentId, long deadlineEpochSeconds) {}
