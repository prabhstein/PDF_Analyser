package com.prabhstein.pdfanalyser.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prabhstein.pdfanalyser.chat.AnswerResult;
import com.prabhstein.pdfanalyser.chat.Citation;
import com.prabhstein.pdfanalyser.config.AppProperties;
import com.prabhstein.pdfanalyser.document.DocumentChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public OpenAiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.openai().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.openai().apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<Double> embed(String input) {
        ensureApiKey();
        Map<String, Object> request = Map.of(
                "model", properties.openai().embeddingModel(),
                "input", input
        );
        JsonNode response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (response == null || !response.has("data")) {
            throw new AiServiceException("OpenAI did not return an embedding.");
        }
        List<Double> embedding = new ArrayList<>();
        response.path("data").get(0).path("embedding").forEach(value -> embedding.add(value.asDouble()));
        return embedding;
    }

    public AnswerResult answer(String question, List<DocumentChunk> chunks) {
        ensureApiKey();
        if (chunks.isEmpty()) {
            return new AnswerResult("I could not find relevant text in this PDF to answer that question.", List.of(), true);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.openai().chatModel());
        request.put("input", buildPrompt(question, chunks));
        request.put("text", Map.of("format", jsonSchema()));

        JsonNode response = webClient.post()
                .uri("/responses")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        return parseAnswer(response);
    }

    String buildPrompt(String question, List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            context.append("[chunk_id=").append(chunk.id())
                    .append(", page=").append(chunk.pageNumber())
                    .append("]\n")
                    .append(chunk.content())
                    .append("\n\n");
        }
        return """
                You are a careful PDF analysis assistant. Answer the user's question using only the provided PDF context.
                If the context is insufficient, say so and set notAnswerable to true.
                Cite only chunk IDs and page numbers that appear in the context.

                PDF context:
                %s

                User question:
                %s
                """.formatted(context, question);
    }

    private AnswerResult parseAnswer(JsonNode response) {
        String outputText = outputText(response);
        try {
            JsonNode parsed = objectMapper.readTree(outputText);
            List<Citation> citations = new ArrayList<>();
            parsed.path("citations").forEach(citation -> citations.add(new Citation(
                    citation.path("chunkId").asLong(),
                    citation.path("pageNumber").asInt()
            )));
            return new AnswerResult(
                    parsed.path("answer").asText(),
                    citations,
                    parsed.path("notAnswerable").asBoolean(false)
            );
        } catch (Exception ex) {
            throw new AiServiceException("OpenAI returned an answer that could not be parsed.", ex);
        }
    }

    private String outputText(JsonNode response) {
        if (response == null) {
            throw new AiServiceException("OpenAI returned an empty response.");
        }
        if (response.hasNonNull("output_text")) {
            return response.path("output_text").asText();
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) {
                    return content.path("text").asText();
                }
            }
        }
        throw new AiServiceException("OpenAI response did not include output text.");
    }

    private Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "json_schema",
                "name", "pdf_answer",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("answer", "citations", "notAnswerable"),
                        "properties", Map.of(
                                "answer", Map.of("type", "string"),
                                "notAnswerable", Map.of("type", "boolean"),
                                "citations", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "additionalProperties", false,
                                                "required", List.of("chunkId", "pageNumber"),
                                                "properties", Map.of(
                                                        "chunkId", Map.of("type", "integer"),
                                                        "pageNumber", Map.of("type", "integer")
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private void ensureApiKey() {
        if (properties.openai().apiKey() == null || properties.openai().apiKey().isBlank()) {
            throw new AiServiceException("OPENAI_API_KEY is not configured.");
        }
    }
}
