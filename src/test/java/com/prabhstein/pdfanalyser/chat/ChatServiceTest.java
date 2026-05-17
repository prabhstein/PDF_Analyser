package com.prabhstein.pdfanalyser.chat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.prabhstein.pdfanalyser.ai.OpenAiClient;
import com.prabhstein.pdfanalyser.config.AppProperties;
import com.prabhstein.pdfanalyser.document.DocumentChunkRepository;
import com.prabhstein.pdfanalyser.document.DocumentService;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    @Test
    void rejectsBlankQuestion() {
        var service = new ChatService(
                mock(DocumentService.class),
                mock(DocumentChunkRepository.class),
                mock(ChatMessageRepository.class),
                mock(OpenAiClient.class),
                new AppProperties("storage/uploads", new AppProperties.OpenAi("test", "http://localhost", "embed", "chat"), new AppProperties.Retrieval(5, 0.18))
        );

        assertThatThrownBy(() -> service.ask(1, " "))
                .isInstanceOf(ChatValidationException.class)
                .hasMessageContaining("question");
    }
}
