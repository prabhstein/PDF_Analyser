package com.prabhstein.pdfanalyser.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String storageDir,
        @Valid OpenAi openai,
        @Valid Retrieval retrieval
) {
    public record OpenAi(
            String apiKey,
            @NotBlank String baseUrl,
            @NotBlank String embeddingModel,
            @NotBlank String chatModel
    ) {
    }

    public record Retrieval(
            @Positive int maxResults,
            double minSimilarity
    ) {
    }
}
