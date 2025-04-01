package com.nolimit35.springfast.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nolimit35.springfast.config.ExceptionNotifyProperties;
import com.nolimit35.springfast.model.CodeAuthorInfo;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service for interacting with GitHub API
 */
@Slf4j
@Service
public class GitHubService extends AbstractGitSourceControlService {
    public GitHubService(ExceptionNotifyProperties properties) {
        super(properties);
    }

    /**
     * Get author information for a specific file and line
     *
     * @param fileName the file name
     * @param lineNumber the line number
     * @return author information or null if not found
     */
    @Override
    public CodeAuthorInfo getAuthorInfo(String fileName, int lineNumber) {
        if (!validateConfiguration(
                properties.getGithub().getToken(),
                properties.getGithub().getRepoOwner(),
                properties.getGithub().getRepoName(),
                "GitHub")) {
            return null;
        }

        try {
            String url = String.format(
                "https://api.github.com/repos/%s/%s/blame/%s/%s",
                properties.getGithub().getRepoOwner(),
                properties.getGithub().getRepoName(),
                properties.getGithub().getBranch(),
                fileName
            );

            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + properties.getGithub().getToken())
                .header("Accept", "application/vnd.github.v3+json")
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to get blame information: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode blameData = objectMapper.readTree(responseBody);

                // Find the blame range that contains the line
                for (JsonNode range : blameData) {
                    JsonNode lines = range.get("lines");
                    int startLine = range.get("startingLine").asInt();
                    int linesCount = lines.size();
                    
                    if (lineNumber >= startLine && lineNumber < startLine + linesCount) {
                        JsonNode commit = range.get("commit");
                        JsonNode author = commit.get("author");
                        
                        return CodeAuthorInfo.builder()
                            .name(author.get("name").asText())
                            .email(author.get("email").asText())
                            .lastCommitTime(LocalDateTime.parse(commit.get("committer").get("date").asText(), this.DATE_FORMAT))
                            .fileName(fileName)
                            .lineNumber(lineNumber)
                            .build();
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error fetching author information from GitHub", e);
        }
        
        return null;
    }
} 