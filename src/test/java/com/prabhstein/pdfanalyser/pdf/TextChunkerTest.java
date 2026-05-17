package com.prabhstein.pdfanalyser.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void chunksPageText() {
        var pdf = new PdfTextExtractor.ExtractedPdf(1, List.of(new PdfTextExtractor.PageText(1, "Hello PDF world.")));

        var chunks = chunker.chunk(pdf);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().pageNumber()).isEqualTo(1);
        assertThat(chunks.getFirst().content()).isEqualTo("Hello PDF world.");
    }

    @Test
    void rejectsPdfWithoutExtractableText() {
        var pdf = new PdfTextExtractor.ExtractedPdf(1, List.of(new PdfTextExtractor.PageText(1, "   ")));

        assertThatThrownBy(() -> chunker.chunk(pdf))
                .isInstanceOf(PdfProcessingException.class)
                .hasMessageContaining("extractable text");
    }

    @Test
    void splitsLongText() {
        var longText = "Section text. ".repeat(400);
        var pdf = new PdfTextExtractor.ExtractedPdf(1, List.of(new PdfTextExtractor.PageText(1, longText)));

        var chunks = chunker.chunk(pdf);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.content().length() <= 1_800);
    }
}
