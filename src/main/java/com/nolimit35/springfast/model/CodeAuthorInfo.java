package com.nolimit35.springfast.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Code author information model
 */
@Data
@Builder
public class CodeAuthorInfo {
    /**
     * Author name
     */
    private String name;

    /**
     * Author email
     */
    private String email;

    /**
     * Last commit time
     */
    private LocalDateTime lastCommitTime;

    /**
     * Source file name
     */
    private String fileName;

    /**
     * Source line number
     */
    private int lineNumber;

    /**
     * Commit message
     */
    private String commitMessage;
} 