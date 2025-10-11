package com.nolimit35.springkit.trace;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Default implementation of TraceInfoProvider
 * Retrieves trace ID from MDC or request headers and generates Tencent CLS trace URLs
 */
@Slf4j
@Component
@ConditionalOnMissingBean(TraceInfoProvider.class)
public class DefaultTraceInfoProvider implements TraceInfoProvider {
    private final ExceptionNotifyProperties properties;

    public DefaultTraceInfoProvider(ExceptionNotifyProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getTraceId() {
        if (!properties.getTrace().isEnabled()) {
            return null;
        }

        try {
            // First try to get traceId from MDC
            String traceId = MDC.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                return traceId;
            }

            // If not found in MDC, try to get from request header
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                String headerName = properties.getTrace().getHeaderName();
                traceId = request.getHeader(headerName);

                if (traceId != null && !traceId.isEmpty()) {
                    return traceId;
                }
            }
        } catch (Exception e) {
            log.debug("Error getting trace ID", e);
        }

        return null;
    }

    @Override
    public String generateTraceUrl(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return null;
        }

        String region = properties.getTencentcls().getRegion();
        String topicId = properties.getTencentcls().getTopicId();
        
        if (region != null && !region.isEmpty() && topicId != null && !topicId.isEmpty()) {
            // Build query JSON
            String interactiveQuery = String.format(
                "{\"filters\":[{\"key\":\"traceId\",\"grammarName\":\"INCLUDE\",\"values\":[{\"values\":[{\"value\":\"%s\",\"isPartialEscape\":true}],\"isOpen\":false}],\"alias_name\":\"traceId\",\"cnName\":\"\"}],\"sql\":{\"quotas\":[],\"dimensions\":[],\"sequences\":[],\"limit\":1000,\"samplingRate\":1},\"sqlStr\":\"\"}",
                traceId
            );
            
            // Base64 encode query parameters
            String interactiveQueryBase64 = Base64.getEncoder().encodeToString(
                interactiveQuery.getBytes(StandardCharsets.UTF_8)
            );
            
            // Build complete URL
            return String.format(
                "https://console.cloud.tencent.com/cls/search?region=%s&topic_id=%s&interactiveQueryBase64=%s",
                region, topicId, interactiveQueryBase64
            ) + "&time=now%2Fd,now%2Fd";
        }
        
        return null;
    }
}
