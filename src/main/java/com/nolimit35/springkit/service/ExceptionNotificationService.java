package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.filter.ExceptionFilter;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.NotificationProviderManager;
import com.nolimit35.springkit.trace.TraceInfoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling exception notifications
 */
@Slf4j
@Service
public class ExceptionNotificationService {
    private final ExceptionNotifyProperties properties;
    private final ExceptionAnalyzerService analyzerService;
    private final NotificationProviderManager notificationManager;
    private final NotificationFormatter formatter;
    private final ExceptionFilter filter;
    private final EnvironmentProvider environmentProvider;
    private final TraceInfoProvider traceInfoProvider;
    private final ExceptionDeduplicationService deduplicationService;

    public ExceptionNotificationService(
            ExceptionNotifyProperties properties,
            ExceptionAnalyzerService analyzerService,
            NotificationProviderManager notificationManager,
            NotificationFormatter formatter,
            ExceptionFilter filter,
            EnvironmentProvider environmentProvider,
            TraceInfoProvider traceInfoProvider,
            ExceptionDeduplicationService deduplicationService) {
        this.properties = properties;
        this.analyzerService = analyzerService;
        this.notificationManager = notificationManager;
        this.formatter = formatter;
        this.filter = filter;
        this.environmentProvider = environmentProvider;
        this.traceInfoProvider = traceInfoProvider;
        this.deduplicationService = deduplicationService;
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
            // Get trace ID from provider
            String traceId = traceInfoProvider.getTraceId();

            // Analyze exception
            ExceptionInfo exceptionInfo = analyzerService.analyzeException(throwable, traceId);

            // Add current environment to exception info
            exceptionInfo.setEnvironment(currentEnvironment);

            // Check for duplicate exceptions
            if (!deduplicationService.shouldNotify(exceptionInfo)) {
                log.debug("Exception filtered by deduplication: {}", exceptionInfo.getType());
                return;
            }

            // Send notification via notification manager
            boolean notificationSent = notificationManager.sendNotification(exceptionInfo);

            if (notificationSent) {
                log.info("Exception notification sent for: {}", exceptionInfo.getType());
            } else {
                log.warn("No notification channels were successful for exception: {}", exceptionInfo.getType());
            }
        } catch (Exception e) {
            log.error("Error processing exception notification", e);
        }
    }



}