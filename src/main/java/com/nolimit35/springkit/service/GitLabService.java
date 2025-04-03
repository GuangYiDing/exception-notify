package com.nolimit35.springkit.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service for interacting with GitLab API
 */
@Slf4j
@Service
public class GitLabService extends AbstractGitSourceControlService {
    private static final String API_FILE_BLAME = "%s/projects/%s/repository/files/%s/blame";
    
    public GitLabService(ExceptionNotifyProperties properties) {
        super(properties);
    }

    /**
     * Get author information for a specific file and line using GitLab API
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @return author information or null if not found
     */
    @Override
    public CodeAuthorInfo getAuthorInfo(String fileName, int lineNumber) {
        if (!validateConfiguration(
                properties.getGitlab().getToken(),
                properties.getGitlab().getProjectId(),
                properties.getGitlab().getBranch(),
                "GitLab")) {
            return null;
        }

        try {
            // URL encode the file path for GitLab API
            String encodedFilePath = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
            
            // Construct GitLab API URL for blame information
            String apiUrl = String.format(
                API_FILE_BLAME,
                properties.getGitlab().getBaseUrl(),
                properties.getGitlab().getProjectId(),
                encodedFilePath
            );
            
            // Add query parameter for branch
            apiUrl += "?ref=" + properties.getGitlab().getBranch();
            
            Request request = new Request.Builder()
                .url(apiUrl)
                .header("PRIVATE-TOKEN", properties.getGitlab().getToken())
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to get blame information from GitLab: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode blameData = objectMapper.readTree(responseBody);
                
                // Find the blame range that contains the line
                for (JsonNode blameRange : blameData) {
                    JsonNode lines = blameRange.get("lines");
                    if (lines == null || lines.isEmpty()) {
                        continue;
                    }
                    
                    // Get the line numbers in this range
                    int startLine = -1;
                    int endLine = -1;
                    
                    for (int i = 0; i < lines.size(); i++) {
                        if (startLine == -1) {
                            startLine = i + 1;  // Line numbers are 1-based
                        }
                        endLine = i + 1;
                    }
                    
                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        JsonNode commit = blameRange.get("commit");
                        
                        return CodeAuthorInfo.builder()
                            .name(commit.get("author_name").asText())
                            .email(commit.get("author_email").asText())
                            .lastCommitTime(LocalDateTime.parse(
                                commit.get("authored_date").asText(),
                                DateTimeFormatter.ISO_DATE_TIME))
                            .fileName(fileName)
                            .lineNumber(lineNumber)
                            .commitMessage(commit.get("message").asText())
                            .build();
                    }
                }
                
                log.warn("Could not find blame information for {}:{}", fileName, lineNumber);
            }
        } catch (IOException e) {
            log.error("Error fetching author information from GitLab", e);
        }
        
        return null;
    }
    
    /**
     * Validates that the required configuration is present
     * 
     * @param token The API token
     * @param projectId The project ID
     * @param branch The branch name
     * @param serviceName The name of the service (for logging)
     * @return true if configuration is valid, false otherwise
     */
    protected boolean validateConfiguration(String token, String projectId, String branch, String serviceName) {
        if (token == null || projectId == null || branch == null) {
            log.warn("{} configuration is incomplete. Cannot fetch author information.", serviceName);
            return false;
        }
        return true;
    }
} 