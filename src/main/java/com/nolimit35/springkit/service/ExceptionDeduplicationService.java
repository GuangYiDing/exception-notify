package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.ExceptionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for exception deduplication
 * Prevents duplicate exception notifications within a configured time window
 */
@Slf4j
@Service
public class ExceptionDeduplicationService {
    private final ExceptionNotifyProperties properties;
    private final Map<String, LocalDateTime> exceptionCache = new ConcurrentHashMap<>();

    public ExceptionDeduplicationService(ExceptionNotifyProperties properties) {
        this.properties = properties;
    }

    /**
     * Check if the exception should be notified
     * Returns true if notification should be sent, false if it's a duplicate within time window
     *
     * @param exceptionInfo the exception information
     * @return true if should notify, false otherwise
     */
    public boolean shouldNotify(ExceptionInfo exceptionInfo) {
        ExceptionNotifyProperties.Notification.Deduplication deduplication =
            properties.getNotification().getDeduplication();

        // If deduplication is disabled, always notify
        if (!deduplication.isEnabled()) {
            return true;
        }

        // Generate unique key for the exception
        String exceptionKey = generateExceptionKey(exceptionInfo);
        LocalDateTime now = LocalDateTime.now();

        // Check if exception exists in cache
        LocalDateTime lastNotificationTime = exceptionCache.get(exceptionKey);

        if (lastNotificationTime != null) {
            // Calculate time difference in minutes
            long minutesSinceLastNotification = ChronoUnit.MINUTES.between(lastNotificationTime, now);

            // If within time window, don't notify
            if (minutesSinceLastNotification < deduplication.getTimeWindowMinutes()) {
                log.debug("Exception filtered by deduplication: {} (last notified {} minutes ago)",
                    exceptionInfo.getType(), minutesSinceLastNotification);
                return false;
            }
        }

        // Update cache with current time
        exceptionCache.put(exceptionKey, now);
        return true;
    }

    /**
     * Generate unique key for exception based on type, message, and location
     *
     * @param exceptionInfo the exception information
     * @return unique key
     */
    private String generateExceptionKey(ExceptionInfo exceptionInfo) {
        StringBuilder keyBuilder = new StringBuilder();

        // Include exception type
        if (exceptionInfo.getType() != null) {
            keyBuilder.append(exceptionInfo.getType());
        }
        keyBuilder.append("|");

        // Include exception message
        if (exceptionInfo.getMessage() != null) {
            keyBuilder.append(exceptionInfo.getMessage());
        }
        keyBuilder.append("|");

        // Include exception location
        if (exceptionInfo.getLocation() != null) {
            keyBuilder.append(exceptionInfo.getLocation());
        }

        // Generate hash to keep key size reasonable
        return generateHash(keyBuilder.toString());
    }

    /**
     * Generate SHA-256 hash of the input string
     *
     * @param input the input string
     * @return base64 encoded hash
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to the input string if hashing fails
            log.warn("Failed to generate hash, using raw key", e);
            return input;
        }
    }

    /**
     * Clean up expired entries from cache
     * Runs at configured interval to prevent memory leaks
     */
    @Scheduled(fixedRateString = "#{${exception.notify.notification.deduplication.cleanup-interval-minutes:60} * 60 * 1000}")
    public void cleanupExpiredEntries() {
        if (!properties.getNotification().getDeduplication().isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long timeWindowMinutes = properties.getNotification().getDeduplication().getTimeWindowMinutes();

        // Remove entries older than time window
        exceptionCache.entrySet().removeIf(entry -> {
            long minutesSinceLastNotification = ChronoUnit.MINUTES.between(entry.getValue(), now);
            return minutesSinceLastNotification >= timeWindowMinutes;
        });

        log.debug("Cleaned up expired exception cache entries. Current cache size: {}", exceptionCache.size());
    }

    /**
     * Clear all cache entries (useful for testing)
     */
    public void clearCache() {
        exceptionCache.clear();
        log.debug("Exception cache cleared");
    }

    /**
     * Get current cache size (useful for monitoring)
     *
     * @return cache size
     */
    public int getCacheSize() {
        return exceptionCache.size();
    }
}
