package com.nolimit35.springkit.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service for interacting with Gitee API
 */
@Slf4j
@Service
public class GiteeService extends AbstractGitSourceControlService {

    public GiteeService(ExceptionNotifyProperties properties) {
        super(properties);
    }

    /**
     * Get the full file path in the repository based on a filename
     *
     * @param fileName the file name or partial path
     * @return the full path of the file in the repository, or null if not found
     */
    private String getFilePathFromName(String fileName) {
        if (!validateConfiguration(
                properties.getGitee().getToken(),
                properties.getGitee().getRepoOwner(),
                properties.getGitee().getRepoName(),
                "Gitee")) {
            return null;
        }

        // Check if fileName is null or empty
        if (fileName == null || fileName.trim().isEmpty()) {
            log.error("File name is empty or null");
            return null;
        }

        // Log if fileName already appears to be a path
        boolean isLikelyPath = fileName.contains("/") && !fileName.startsWith("/");
        if (isLikelyPath) {
            log.debug("File name already appears to be a path: {}", fileName);
        }

        try {
            String url = String.format(
                    "https://gitee.com/api/v5/repos/%s/%s/git/trees/%s?access_token=%s&recursive=1",
                    properties.getGitee().getRepoOwner(),
                    properties.getGitee().getRepoName(),
                    properties.getGitee().getBranch(),
                    properties.getGitee().getToken()
            );

            log.debug("Fetching repository tree from: {}", url.replaceAll("access_token=[^&]+", "access_token=***"));

            Request request = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to get repository tree from Gitee: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode treeData = objectMapper.readTree(responseBody);
                JsonNode tree = treeData.get("tree");

                if (tree == null || tree.isEmpty()) {
                    log.warn("Repository tree is empty or not available");
                    return null;
                }

                log.debug("Repository tree contains {} items", tree.size());

                // First check for exact match
                for (JsonNode item : tree) {
                    String path = item.get("path").asText();
                    String type = item.get("type").asText();

                    // Check if path exactly matches the fileName (for full paths)
                    if ("blob".equals(type) && path.equals(fileName)) {
                        log.info("Found exact file path match: {}", path);
                        return path;
                    }
                }

                // If exact match not found, try other matching strategies
                String simpleFileName = getSimpleFileName(fileName);
                log.debug("Simple file name extracted: {}", simpleFileName);

                // Try to find the best match using different strategies
                String bestMatch = null;
                int bestMatchScore = 0;

                for (JsonNode item : tree) {
                    String path = item.get("path").asText();
                    String type = item.get("type").asText();

                    if (!"blob".equals(type)) {
                        continue; // Skip directories
                    }

                    int score = 0;

                    // Strategy 1: Path ends with the simple file name
                    if (path.endsWith("/" + simpleFileName) || path.equals(simpleFileName)) {
                        score += 10;
                    }

                    // Strategy 2: For paths, check if path contains the full fileName
                    if (isLikelyPath && path.contains(fileName)) {
                        score += 20;
                    }

                    // Strategy 3: Check if the file name components match in order
                    if (isLikelyPath) {
                        String[] fileNameParts = fileName.split("/");
                        String[] pathParts = path.split("/");
                        int matchingParts = 0;

                        // Check matching parts from the end
                        for (int i = 0; i < Math.min(fileNameParts.length, pathParts.length); i++) {
                            if (fileNameParts[fileNameParts.length - 1 - i].equals(
                                    pathParts[pathParts.length - 1 - i])) {
                                matchingParts++;
                            } else {
                                break;
                            }
                        }

                        score += matchingParts * 5;
                    }

                    // Update best match if this path has a higher score
                    if (score > bestMatchScore) {
                        bestMatch = path;
                        bestMatchScore = score;
                    }
                }

                if (bestMatch != null) {
                    log.info("Found best matching file path: {} (score: {})", bestMatch, bestMatchScore);
                    return bestMatch;
                }

                log.warn("File '{}' not found in repository tree", fileName);
            }
        } catch (IOException e) {
            log.error("Error fetching repository tree from Gitee", e);
        }

        return null;
    }

    /**
     * Extract simple file name from a path
     * 
     * @param path Path or filename
     * @return Simple file name without directories
     */
    private String getSimpleFileName(String path) {
        if (path == null) {
            return "";
        }
        int lastSeparator = path.lastIndexOf('/');
        return lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
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
                properties.getGitee().getToken(),
                properties.getGitee().getRepoOwner(),
                properties.getGitee().getRepoName(),
                "Gitee")) {
            return null;
        }

        // Get the full file path
        String filePath = getFilePathFromName(fileName);
        if (filePath == null) {
            log.error("Could not find file path for: {}", fileName);
            return null;
        }

        try {
            String url = String.format(
                    "https://gitee.com/api/v5/repos/%s/%s/blame/%s?access_token=%s&ref=%s",
                    properties.getGitee().getRepoOwner(),
                    properties.getGitee().getRepoName(),
                    filePath,
                    properties.getGitee().getToken(),
                    properties.getGitee().getBranch()

            );

            Request request = new Request.Builder()
                .url(url)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to get blame information from Gitee: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode blameData = objectMapper.readTree(responseBody);

                // Track the current line number through all blame ranges
                int currentLineIndex = 1;
                
                // Iterate through blame ranges
                for (JsonNode range : blameData) {
                    JsonNode lines = range.get("lines");
                    int linesCount = lines.size();

                    // Check if our target line is within this range
                    if (lineNumber >= currentLineIndex && lineNumber < currentLineIndex + linesCount) {
                        // Found the range containing our line
                        JsonNode commit = range.get("commit");
                        JsonNode commitAuthor = commit.get("committer");
                        String dateStr = commitAuthor.get("date").asText()
                                .replaceAll("\\+\\d{2}:\\d{2}$", "")
                                .replaceAll("T"," ");
                        return CodeAuthorInfo.builder()
                                .name(commitAuthor.get("name").asText())
                                .email(commitAuthor.get("email").asText())
                                .lastCommitTime(LocalDateTime.parse(dateStr, this.DATE_FORMAT))
                                .fileName(filePath)
                                .lineNumber(lineNumber)
                                .commitMessage(commit.get("message").asText())
                                .build();
                    }
                    
                    // Move to the next range
                    currentLineIndex += linesCount;
                }

                log.warn("Could not find author information for line {} in file {}", lineNumber, filePath);
            }
        } catch (IOException e) {
            log.error("Error fetching author information from Gitee", e);
        }
        
        return null;
    }
} 