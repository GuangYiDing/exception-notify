package com.nolimit35.springkit.model;

import lombok.Builder;
import lombok.Data;

/**
 * Payload passed to the AI analysis web application.
 */
@Data
@Builder
public class AiAnalysisPayload {
    /**
     * Application name where the exception occurred.
     */
    private String appName;

    /**
     * Active environment (e.g. dev/test/prod).
     */
    private String environment;

    /**
     * Exception occurrence time formatted as ISO string.
     */
    private String occurrenceTime;

    /**
     * Exception fully qualified class name.
     */
    private String exceptionType;

    /**
     * Exception message.
     */
    private String exceptionMessage;

    /**
     * Location of the exception (class, method, file, line).
     */
    private String location;

    /**
     * Full stack trace text.
     */
    private String stacktrace;

    /**
     * Code context captured around the failure point.
     */
    private String codeContext;

    /**
     * Trace identifier when available.
     */
    private String traceId;

    /**
     * Trace URL when available.
     */
    private String traceUrl;

    /**
     * Author information for the related source file.
     */
    private Author author;

    /**
     * Nested payload representing author metadata.
     */
    @Data
    @Builder
    public static class Author {
        private String name;
        private String email;
        private String lastCommitTime;
        private String fileName;
        private Integer lineNumber;
        private String commitMessage;
    }
}
