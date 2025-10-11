package com.nolimit35.springkit.notification;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.ExceptionInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for notification providers
 * Makes it easier to implement custom notification providers
 */
@Slf4j
public abstract class AbstractNotificationProvider implements NotificationProvider {
    protected final ExceptionNotifyProperties properties;

    public AbstractNotificationProvider(ExceptionNotifyProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean sendNotification(ExceptionInfo exceptionInfo) {
        if (!isEnabled()) {
            log.debug("{} notification provider is not enabled", getProviderName());
            return false;
        }

        try {
            return doSendNotification(exceptionInfo);
        } catch (Exception e) {
            log.error("Error sending notification through {}: {}", 
                getProviderName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Implement actual notification sending logic in subclasses
     *
     * @param exceptionInfo the complete exception information
     * @return true if notification was sent successfully
     * @throws Exception if an error occurs during sending
     */
    protected abstract boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception;

    /**
     * Get provider name for logging purposes
     *
     * @return name of the provider
     */
    protected String getProviderName() {
        return this.getClass().getSimpleName();
    }
} 