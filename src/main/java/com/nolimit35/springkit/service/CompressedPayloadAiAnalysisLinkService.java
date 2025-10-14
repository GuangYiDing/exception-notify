package com.nolimit35.springkit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.AiAnalysisPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses exception payloads and generates analysis links.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "exception.notify.ai", name = "enabled", havingValue = "true")
public class CompressedPayloadAiAnalysisLinkService implements AiAnalysisLinkService {

    private final ExceptionNotifyProperties properties;
    private final ObjectMapper objectMapper;

    public CompressedPayloadAiAnalysisLinkService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String buildAnalysisLink(AiAnalysisPayload payload) {
        if (!isAvailable() || payload == null) {
            return null;
        }

        try {
            ExceptionNotifyProperties.AI aiConfig = properties.getAi();
            String baseUrl = aiConfig.getAnalysisPageUrl();
            if (!StringUtils.hasText(baseUrl)) {
                return null;
            }

            String json = objectMapper.writeValueAsString(payload);
            byte[] compressed = compress(json.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(compressed);

            return UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .queryParam(aiConfig.getPayloadParam(), encoded)
                    .build(true)
                    .toUriString();
        } catch (Exception e) {
            log.error("Failed to build AI analysis link", e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        ExceptionNotifyProperties.AI aiConfig = properties.getAi();
        return aiConfig != null
                && aiConfig.isEnabled()
                && StringUtils.hasText(aiConfig.getAnalysisPageUrl());
    }

    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
            gzip.finish();
            return baos.toByteArray();
        }
    }
}
