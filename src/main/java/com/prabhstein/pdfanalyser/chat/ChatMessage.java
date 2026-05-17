package com.prabhstein.pdfanalyser.chat;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatMessage(
        long id,
        long documentId,
        String question,
        String answer,
        boolean notAnswerable,
        List<Citation> citations,
        OffsetDateTime createdAt
) {
}
