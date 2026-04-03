package com.example.demo.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.google.genai.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    EmbeddingModel embeddingModel(
        GoogleGenAiEmbeddingConnectionDetails connectionDetails,
        GoogleGenAiTextEmbeddingProperties embeddingProperties,
        RetryTemplate retryTemplate,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<EmbeddingModelObservationConvention> observationConvention
    ) {
        GoogleGenAiTextEmbeddingModel embeddingModel = new GoogleGenAiTextEmbeddingModel(
            connectionDetails,
            embeddingProperties.getOptions(),
            retryTemplate,
            observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP)
        );

        observationConvention.ifAvailable(embeddingModel::setObservationConvention);
        return embeddingModel;
    }
}
