package com.prabhstein.pdfanalyser.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractor {

    public ExtractedPdf extract(Path pdfPath) {
        try (var document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).trim();
                pages.add(new PageText(page, text));
            }
            return new ExtractedPdf(document.getNumberOfPages(), pages);
        } catch (IOException ex) {
            throw new PdfProcessingException("Could not read PDF text", ex);
        }
    }

    public record ExtractedPdf(int pageCount, List<PageText> pages) {
    }

    public record PageText(int pageNumber, String text) {
    }
}
