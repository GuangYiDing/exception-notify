package com.nolimit35.springkit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assumptions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * Integration test for GitLabService
 * Note: This test makes actual API calls to GitLab and requires a valid GitLab token
 */
@ExtendWith(MockitoExtension.class)
@Tag("integration")
public class GitLabServiceTest {

    @Mock
    private ExceptionNotifyProperties properties;

    @Mock
    private ExceptionNotifyProperties.GitLab gitlabProperties;

    @InjectMocks
    private GitLabService gitLabService;

    private static final String TEST_FILE = "src/main/java/com/nolimit35/springkit/service/GitLabService.java";
    private static final int TEST_LINE = 15;

    @BeforeEach
    public void setUp() {
        // Set up properties with test GitLab credentials
        // Replace these values with valid test values for your GitLab instance
        when(properties.getGitlab()).thenReturn(gitlabProperties);
        when(gitlabProperties.getToken()).thenReturn("your_gitlab_token");
        when(gitlabProperties.getProjectId()).thenReturn("your_project_id");
        when(gitlabProperties.getBranch()).thenReturn("main");
        when(gitlabProperties.getBaseUrl()).thenReturn("https://gitlab.com/api/v4");
    }

    @Test
    public void testGetAuthorInfo_Success() {
        // Skip this test if you don't have valid GitLab credentials for testing
        // Uncomment the following line to skip
        // assumeTrue(false, "Skipped because no valid GitLab credentials available");
        
        // Call the actual service method
        CodeAuthorInfo authorInfo = gitLabService.getAuthorInfo(TEST_FILE, TEST_LINE);

        // Verify we got a result
        assertNotNull(authorInfo);
        
        // Verify the basic structure is correct (we can't assert exact values since they depend on the actual repository)
        assertNotNull(authorInfo.getName());
        assertNotNull(authorInfo.getEmail());
        assertNotNull(authorInfo.getLastCommitTime());
        assertEquals(TEST_FILE, authorInfo.getFileName());
        assertEquals(TEST_LINE, authorInfo.getLineNumber());
    }

    @Test
    public void testGetAuthorInfo_InvalidFile() {
        // Test with a file that doesn't exist
        CodeAuthorInfo authorInfo = gitLabService.getAuthorInfo("non-existent-file.txt", 1);
        
        // Should return null for non-existent file
        assertNull(authorInfo);
    }

} 