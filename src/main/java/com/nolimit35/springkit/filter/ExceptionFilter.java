package com.nolimit35.springkit.filter;

/**
 * Interface for filtering exceptions
 */
public interface ExceptionFilter {
    /**
     * Determine whether to notify for the given exception
     *
     * @param throwable the exception to check
     * @return true if notification should be sent, false otherwise
     */
    boolean shouldNotify(Throwable throwable);
} 