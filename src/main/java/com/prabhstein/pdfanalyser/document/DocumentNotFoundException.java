package com.prabhstein.pdfanalyser.document;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(long documentId) {
        super("Document %d was not found.".formatted(documentId));
    }
}
