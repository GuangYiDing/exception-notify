package com.nolimit35.springkit.notification;

import com.nolimit35.springkit.model.ExceptionInfo;

/**
 * Interface for notification providers
 */
public interface NotificationProvider {
    /**
     * Send a notification
     *
     * @param exceptionInfo the complete exception information object
     * @return true if notification was sent successfully, false otherwise
     */
    boolean sendNotification(ExceptionInfo exceptionInfo);
    
    /**
     * Check if this provider is enabled
     * 
     * @return true if the provider is enabled, false otherwise
     */
    boolean isEnabled();
} 