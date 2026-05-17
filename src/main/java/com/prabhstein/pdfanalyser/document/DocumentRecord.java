package com.prabhstein.pdfanalyser.document;

import java.time.OffsetDateTime;

public record DocumentRecord(
        long id,
        String originalFilename,
        String storedFilename,
        String contentType,
        long byteSize,
        int pageCount,
        DocumentStatus status,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime processedAt
) {
}
