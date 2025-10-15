package com.nolimit35.springkit.service;

import com.fasterxml.jackson.databind.JsonNode;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Compresses exception payloads and generates analysis links.
 */
@Slf4j
@Service
public class CompressedPayloadAiAnalysisLinkService implements AiAnalysisLinkService {

    private static final String PAYLOAD_QUERY_PARAM = "payload";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final ExceptionNotifyProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public CompressedPayloadAiAnalysisLinkService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
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

            String shortCode = requestCompressedToken(baseUrl, encoded);
            if (!StringUtils.hasText(shortCode)) {
                return null;
            }

            return UriComponentsBuilder
                    .fromUriString(baseUrl)
                    .replaceQuery(null)
                    .replaceQueryParam(PAYLOAD_QUERY_PARAM, shortCode)
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

    private String requestCompressedToken(String baseUrl, String encodedPayload) {
        String compressUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/compress")
                .build(true)
                .toUriString();

        try {
            String requestBody = objectMapper.writeValueAsString(java.util.Collections.singletonMap(PAYLOAD_QUERY_PARAM, encodedPayload));
            Request request = new Request.Builder()
                    .url(compressUrl)
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to request AI compression endpoint: status {}", response.code());
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    log.error("AI compression endpoint returned empty body");
                    return null;
                }

                String responseBody = body.string();
                JsonNode root = objectMapper.readTree(responseBody);
                if (root.path("code").asInt(-1) != 0) {
                    log.error("AI compression endpoint returned error: {}", responseBody);
                    return null;
                }

                String token = root.path("data").asText(null);
                if (!StringUtils.hasText(token)) {
                    log.error("AI compression endpoint returned empty token: {}", responseBody);
                    return null;
                }

                return token;
            }
        } catch (IOException e) {
            log.error("Error calling AI compression endpoint", e);
            return null;
        }
    }
}
