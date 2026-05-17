package com.prabhstein.pdfanalyser.pdf;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TextChunker {

    private static final int MAX_CHARS = 1_800;
    private static final int OVERLAP_CHARS = 180;

    public List<TextChunk> chunk(PdfTextExtractor.ExtractedPdf pdf) {
        List<TextChunk> chunks = new ArrayList<>();
        for (PdfTextExtractor.PageText page : pdf.pages()) {
            String normalized = normalize(page.text());
            if (normalized.isBlank()) {
                continue;
            }
            chunks.addAll(chunkPage(page.pageNumber(), normalized));
        }
        if (chunks.isEmpty()) {
            throw new PdfProcessingException("The PDF does not contain extractable text.");
        }
        return chunks;
    }

    private List<TextChunk> chunkPage(int pageNumber, String text) {
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHARS, text.length());
            int paragraphBreak = text.lastIndexOf("\n\n", end);
            if (paragraphBreak > start + MAX_CHARS / 2) {
                end = paragraphBreak;
            }
            String content = text.substring(start, end).trim();
            if (!content.isBlank()) {
                chunks.add(new TextChunk(pageNumber, index++, content));
            }
            if (end == text.length()) {
                break;
            }
            start = Math.max(end - OVERLAP_CHARS, start + 1);
        }
        return chunks;
    }

    private String normalize(String text) {
        return text == null ? "" : text
                .replace("\r\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record TextChunk(int pageNumber, int chunkIndex, String content) {
    }
}
