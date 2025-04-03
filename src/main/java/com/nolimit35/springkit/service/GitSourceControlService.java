package com.nolimit35.springkit.service;

import com.nolimit35.springkit.model.CodeAuthorInfo;

/**
 * Interface for Git source control services (GitHub, Gitee, etc.)
 * Provides common operations for interacting with git repositories
 */
public interface GitSourceControlService {
    
    /**
     * Get author information for a specific file and line
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @return author information or null if not found
     */
    CodeAuthorInfo getAuthorInfo(String fileName, int lineNumber);
} 