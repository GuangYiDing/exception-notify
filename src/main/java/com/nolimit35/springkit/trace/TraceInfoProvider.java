package com.nolimit35.springkit.trace;

/**
 * Interface for providing trace information
 * This interface allows customization of how trace IDs are retrieved and trace URLs are generated
 */
public interface TraceInfoProvider {
    /**
     * Get trace ID from the current context
     *
     * @return trace ID or null if not available
     */
    String getTraceId();

    /**
     * Generate trace URL for the given trace ID
     *
     * @param traceId the trace ID
     * @return trace URL or null if not available
     */
    String generateTraceUrl(String traceId);
}
