package com.nolimit35.springkit.service;

import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * Abstract implementation of GitSourceControlService providing common functionality
 * for different git source control providers (GitHub, Gitee, etc.)
 */
@Slf4j
public abstract class AbstractGitSourceControlService implements GitSourceControlService {
    
    protected final ExceptionNotifyProperties properties;
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    protected AbstractGitSourceControlService(ExceptionNotifyProperties properties) {
        this.properties = properties;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Validates that the required configuration is present
     * 
     * @param token The API token
     * @param repoOwner The repository owner
     * @param repoName The repository name
     * @param serviceName The name of the service (for logging)
     * @return true if configuration is valid, false otherwise
     */
    protected boolean validateConfiguration(String token, String repoOwner, String repoName, String serviceName) {
        if (token == null || repoOwner == null || repoName == null) {
            log.warn("{} configuration is incomplete. Cannot fetch author information.", serviceName);
            return false;
        }
        return true;
    }
    
    /**
     * Abstract method to be implemented by concrete services to get author information
     * for a specific file and line
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @return author information or null if not found
     */
    @Override
    public abstract CodeAuthorInfo getAuthorInfo(String fileName, int lineNumber);
} 