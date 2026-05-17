package com.prabhstein.pdfanalyser.document;

public record DocumentChunk(
        long id,
        long documentId,
        int pageNumber,
        int chunkIndex,
        String content,
        int charCount,
        double similarity
) {
}
