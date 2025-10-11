package com.nolimit35.springkit.notification;

import com.nolimit35.springkit.model.ExceptionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for notification providers
 */
@Slf4j
@Component
public class NotificationProviderManager {
    private final List<NotificationProvider> providers;

    public NotificationProviderManager(List<NotificationProvider> providers) {
        this.providers = providers;
        
        if (log.isInfoEnabled()) {
            log.info("Initialized NotificationProviderManager with {} provider(s)", providers.size());
            for (NotificationProvider provider : providers) {
                log.info("Found notification provider: {} (enabled: {})", 
                    provider.getClass().getSimpleName(), 
                    provider.isEnabled());
            }
        }
    }
    
    /**
     * Send notification through all enabled providers
     *
     * @param exceptionInfo the complete exception information
     * @return true if at least one provider sent the notification successfully
     */
    public boolean sendNotification(ExceptionInfo exceptionInfo) {
        if (providers.isEmpty()) {
            log.warn("No notification providers available");
            return false;
        }
        
        AtomicBoolean atLeastOneSent = new AtomicBoolean(false);
        
        // Try sending through all enabled providers
        providers.stream()
            .filter(NotificationProvider::isEnabled)
            .forEach(provider -> {
                try {
                    boolean sent = provider.sendNotification(exceptionInfo);
                    if (sent) {
                        atLeastOneSent.set(true);
                        log.info("Notification sent successfully through {}", 
                            provider.getClass().getSimpleName());
                    } else {
                        log.warn("Failed to send notification through {}", 
                            provider.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    log.error("Error sending notification through {}: {}", 
                        provider.getClass().getSimpleName(), e.getMessage(), e);
                }
            });
        
        return atLeastOneSent.get();
    }
    
    /**
     * Get all available providers
     *
     * @return list of notification providers
     */
    public List<NotificationProvider> getProviders() {
        return providers;
    }
} 