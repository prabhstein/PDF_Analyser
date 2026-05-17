package com.prabhstein.pdfanalyser.document;

import com.prabhstein.pdfanalyser.ai.OpenAiClient;
import com.prabhstein.pdfanalyser.config.AppProperties;
import com.prabhstein.pdfanalyser.pdf.PdfProcessingException;
import com.prabhstein.pdfanalyser.pdf.PdfTextExtractor;
import com.prabhstein.pdfanalyser.pdf.TextChunker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final OpenAiClient openAiClient;
    private final Path storageDir;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker,
            OpenAiClient openAiClient,
            AppProperties properties
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.openAiClient = openAiClient;
        this.storageDir = Path.of(properties.storageDir());
    }

    @Transactional
    public DocumentRecord upload(MultipartFile file) {
        validate(file);
        try {
            Files.createDirectories(storageDir);
            String storedFilename = UUID.randomUUID() + ".pdf";
            Path storedPath = storageDir.resolve(storedFilename);
            file.transferTo(storedPath);

            DocumentRecord document = documentRepository.create(
                    cleanFilename(file.getOriginalFilename()),
                    storedFilename,
                    file.getContentType() == null ? "application/pdf" : file.getContentType(),
                    file.getSize()
            );
            process(document.id(), storedPath);
            return documentRepository.findById(document.id()).orElseThrow();
        } catch (IOException ex) {
            throw new PdfProcessingException("Could not store uploaded PDF.", ex);
        }
    }

    public List<DocumentRecord> findAll() {
        return documentRepository.findAll();
    }

    public DocumentRecord findById(long id) {
        return documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    public int countChunks(long documentId) {
        return chunkRepository.countByDocumentId(documentId);
    }

    private void process(long documentId, Path storedPath) {
        try {
            var extracted = pdfTextExtractor.extract(storedPath);
            var chunks = textChunker.chunk(extracted);
            List<DocumentChunkRepository.ChunkEmbedding> embeddings = chunks.stream()
                    .map(chunk -> new DocumentChunkRepository.ChunkEmbedding(
                            chunk.pageNumber(),
                            chunk.chunkIndex(),
                            chunk.content(),
                            openAiClient.embed(chunk.content())
                    ))
                    .toList();
            chunkRepository.saveAll(documentId, embeddings);
            documentRepository.markReady(documentId, extracted.pageCount());
        } catch (RuntimeException ex) {
            documentRepository.markFailed(documentId, ex.getMessage());
            throw ex;
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("Choose a PDF before uploading.");
        }
        String filename = cleanFilename(file.getOriginalFilename());
        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new PdfProcessingException("Only PDF files are supported.");
        }
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.pdf";
        }
        return Path.of(filename).getFileName().toString();
    }
}
