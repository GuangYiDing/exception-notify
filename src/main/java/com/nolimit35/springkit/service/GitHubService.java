package com.nolimit35.springkit.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for interacting with GitHub API
 */
@Slf4j
@Service
public class GitHubService extends AbstractGitSourceControlService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String GITHUB_GRAPHQL_ENDPOINT = "https://api.github.com/graphql";
    
    public GitHubService(ExceptionNotifyProperties properties) {
        super(properties);
    }

    /**
     * Get author information for a specific file and line using GitHub GraphQL API
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
            // Construct GraphQL query for blame information
            String graphQLQuery = String.format(
                "{\"query\":\"query {\\n" +
                "  repository(name: \\\"%s\\\", owner: \\\"%s\\\") {\\n" +
                "    ref(qualifiedName:\\\"%s\\\") {\\n" +
                "      target {\\n" +
                "        ... on Commit {\\n" +
                "          blame(path:\\\"%s\\\") {\\n" +
                "            ranges {\\n" +
                "              commit {\\n" +
                "                author {\\n" +
                "                  name\\n" +
                "                  email\\n" +
                "                  date\\n" +
                "                }\\n" +
                "                committer {\\n" +
                "                  date\\n" +
                "                }\\n" +
                "                message\\n" +
                "              }\\n" +
                "              startingLine\\n" +
                "              endingLine\\n" +
                "            }\\n" +
                "          }\\n" +
                "        }\\n" +
                "      }\\n" +
                "    }\\n" +
                "  }\\n" +
                "}\"}",
                properties.getGithub().getRepoName(),
                properties.getGithub().getRepoOwner(),
                properties.getGithub().getBranch(),
                fileName
            );

            RequestBody body = RequestBody.create(graphQLQuery, JSON);
            Request request = new Request.Builder()
                .url(GITHUB_GRAPHQL_ENDPOINT)
                .header("Authorization", "Bearer " + properties.getGithub().getToken())
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to get blame information: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode data = objectMapper.readTree(responseBody).get("data");
                
                if (data == null || data.has("errors")) {
                    log.error("GraphQL query returned errors: {}", responseBody);
                    return null;
                }
                
                JsonNode blame = data.get("repository")
                    .get("ref")
                    .get("target")
                    .get("blame");
                
                JsonNode ranges = blame.get("ranges");
                
                // Find the blame range that contains the line
                for (JsonNode range : ranges) {
                    int startLine = range.get("startingLine").asInt();
                    int endLine = range.get("endingLine").asInt();
                    
                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        JsonNode commit = range.get("commit");
                        JsonNode author = commit.get("author");
                        JsonNode committer = commit.get("committer");
                        
                        return CodeAuthorInfo.builder()
                            .name(author.get("name").asText())
                            .email(author.get("email").asText())
                            .lastCommitTime(LocalDateTime.parse(committer.get("date").asText(), DateTimeFormatter.ISO_DATE_TIME))
                            .fileName(fileName)
                            .lineNumber(lineNumber)
                            .commitMessage(commit.get("message").asText())
                            .build();
                    }
                }
                
                log.warn("Could not find blame information for {}:{}", fileName, lineNumber);
            }
        } catch (IOException e) {
            log.error("Error fetching author information from GitHub", e);
        }
        
        return null;
    }
} 