package com.nolimit35.springkit.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Exception information model
 */
@Data
@Builder
public class ExceptionInfo {
    /**
     * Exception time
     */
    private LocalDateTime time;

    /**
     * Exception type
     */
    private String type;

    /**
     * Exception message
     */
    private String message;

    /**
     * Exception location (file and line number)
     */
    private String location;

    /**
     * Exception stacktrace
     */
    private String stacktrace;

    /**
     * Trace ID
     */
    private String traceId;

    /**
     * Application name
     */
    private String appName;

    /**
     * Current environment (e.g., dev, test, prod)
     */
    private String environment;

    /**
     * Code author information
     */
    private CodeAuthorInfo authorInfo;
    
    /**
     * Trace URL
     */
    private String traceUrl;
} 