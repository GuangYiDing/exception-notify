package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.filter.ExceptionFilter;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.ExceptionInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Service for handling exception notifications
 */
@Slf4j
@Service
public class ExceptionNotificationService {
    private final ExceptionNotifyProperties properties;
    private final ExceptionAnalyzerService analyzerService;
    private final DingTalkService dingTalkService;
    private final WeChatWorkService weChatWorkService;
    private final NotificationFormatter formatter;
    private final ExceptionFilter filter;
    private final EnvironmentProvider environmentProvider;

    public ExceptionNotificationService(
            ExceptionNotifyProperties properties,
            ExceptionAnalyzerService analyzerService,
            DingTalkService dingTalkService,
            WeChatWorkService weChatWorkService,
            NotificationFormatter formatter,
            ExceptionFilter filter,
            EnvironmentProvider environmentProvider) {
        this.properties = properties;
        this.analyzerService = analyzerService;
        this.dingTalkService = dingTalkService;
        this.weChatWorkService = weChatWorkService;
        this.formatter = formatter;
        this.filter = filter;
        this.environmentProvider = environmentProvider;
    }

    /**
     * Process exception and send notification if needed
     *
     * @param throwable the exception to process
     */
    public void processException(Throwable throwable) {
        if (!properties.isEnabled()) {
            log.debug("Exception notification is disabled");
            return;
        }

        // Get current environment from Spring profiles
        String currentEnvironment = environmentProvider.getCurrentEnvironment();
        
        // Update the current environment in properties
        properties.getEnvironment().setCurrent(currentEnvironment);
        
        // Check if we should report from the current environment
        if (!properties.getEnvironment().shouldReportFromCurrentEnvironment()) {
            log.debug("Exception notification is disabled for the current environment: {}", currentEnvironment);
            return;
        }

        if (!filter.shouldNotify(throwable)) {
            log.debug("Exception filtered out: {}", throwable.getClass().getName());
            return;
        }

        try {
            // Get trace ID from request if available
            String traceId = getTraceId();
            
            // Analyze exception
            ExceptionInfo exceptionInfo = analyzerService.analyzeException(throwable, traceId);
            
            // Add current environment to exception info
            exceptionInfo.setEnvironment(currentEnvironment);
            
            // Format notification
            String content = formatter.format(exceptionInfo);
            
            // Extract title from the first line of content
            String title = content.split("\n")[0];
            
            // Send notification to DingTalk if configured
            boolean dingTalkSent = false;
            if (properties.getDingtalk().getWebhook() != null && !properties.getDingtalk().getWebhook().isEmpty()) {
                dingTalkSent = dingTalkService.sendNotification(title, content);
                if (dingTalkSent) {
                    log.info("Exception notification sent to DingTalk for: {}", exceptionInfo.getType());
                } else {
                    log.error("Failed to send exception notification to DingTalk for: {}", exceptionInfo.getType());
                }
            }
            
            // Send notification to WeChat Work if configured
            boolean weChatWorkSent = false;
            if (properties.getWechatwork().getWebhook() != null && !properties.getWechatwork().getWebhook().isEmpty()) {
                weChatWorkSent = weChatWorkService.sendNotification(title, content);
                if (weChatWorkSent) {
                    log.info("Exception notification sent to WeChat Work for: {}", exceptionInfo.getType());
                } else {
                    log.error("Failed to send exception notification to WeChat Work for: {}", exceptionInfo.getType());
                }
            }
            
            if (!dingTalkSent && !weChatWorkSent) {
                log.warn("No notification channels were configured or successful for exception: {}", exceptionInfo.getType());
            }
        } catch (Exception e) {
            log.error("Error processing exception notification", e);
        }
    }

    /**
     * Get trace ID from current request
     *
     * @return trace ID or null if not available
     */
    private String getTraceId() {
        if (!properties.getTrace().isEnabled()) {
            return null;
        }

        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                String headerName = properties.getTrace().getHeaderName();
                String traceId = request.getHeader(headerName);
                
                if (traceId != null && !traceId.isEmpty()) {
                    return MDC.get("traceId");
                }
            }
        } catch (Exception e) {
            log.debug("Error getting trace ID", e);
        }
        
        return MDC.get("traceId");
    }
} 