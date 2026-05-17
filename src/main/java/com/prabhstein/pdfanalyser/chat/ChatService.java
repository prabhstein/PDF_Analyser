package com.prabhstein.pdfanalyser.chat;

import com.prabhstein.pdfanalyser.ai.OpenAiClient;
import com.prabhstein.pdfanalyser.config.AppProperties;
import com.prabhstein.pdfanalyser.document.DocumentChunk;
import com.prabhstein.pdfanalyser.document.DocumentChunkRepository;
import com.prabhstein.pdfanalyser.document.DocumentRecord;
import com.prabhstein.pdfanalyser.document.DocumentService;
import com.prabhstein.pdfanalyser.document.DocumentStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final DocumentService documentService;
    private final DocumentChunkRepository chunkRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OpenAiClient openAiClient;
    private final AppProperties properties;

    public ChatService(
            DocumentService documentService,
            DocumentChunkRepository chunkRepository,
            ChatMessageRepository chatMessageRepository,
            OpenAiClient openAiClient,
            AppProperties properties
    ) {
        this.documentService = documentService;
        this.chunkRepository = chunkRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.openAiClient = openAiClient;
        this.properties = properties;
    }

    @Transactional
    public ChatMessage ask(long documentId, String question) {
        if (question == null || question.isBlank()) {
            throw new ChatValidationException("Ask a question before sending.");
        }
        DocumentRecord document = documentService.findById(documentId);
        if (document.status() != DocumentStatus.READY) {
            throw new ChatValidationException("This PDF is not ready for questions yet.");
        }
        List<Double> questionEmbedding = openAiClient.embed(question);
        List<DocumentChunk> chunks = chunkRepository.findNearest(
                        documentId,
                        questionEmbedding,
                        properties.retrieval().maxResults()
                ).stream()
                .filter(chunk -> chunk.similarity() >= properties.retrieval().minSimilarity())
                .toList();
        AnswerResult answer = openAiClient.answer(question, chunks);
        return chatMessageRepository.save(documentId, question.trim(), answer);
    }

    public List<ChatMessage> history(long documentId) {
        return chatMessageRepository.findByDocumentId(documentId);
    }
}
