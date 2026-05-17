package com.prabhstein.pdfanalyser.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prabhstein.pdfanalyser.config.AppProperties;
import com.prabhstein.pdfanalyser.document.DocumentChunk;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiClientTest {

    @Test
    void promptIncludesOnlyRetrievedChunksAsContext() {
        var properties = new AppProperties(
                "storage/uploads",
                new AppProperties.OpenAi("test", "https://api.openai.com/v1", "text-embedding-3-small", "gpt-5-mini"),
                new AppProperties.Retrieval(5, 0.18)
        );
        var client = new OpenAiClient(WebClient.builder(), new ObjectMapper(), properties);

        String prompt = client.buildPrompt("What is this about?", List.of(
                new DocumentChunk(10, 1, 2, 0, "Relevant paragraph", 18, 0.8)
        ));

        assertThat(prompt).contains("chunk_id=10, page=2");
        assertThat(prompt).contains("Relevant paragraph");
        assertThat(prompt).contains("What is this about?");
    }
}
