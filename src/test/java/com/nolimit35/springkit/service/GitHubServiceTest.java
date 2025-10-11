package com.nolimit35.springkit.service;

import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.model.CodeAuthorInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration test for GitHubService
 * Note: This test makes actual API calls to GitHub and requires a valid GitHub token
 */
@ExtendWith(MockitoExtension.class)
@Tag("integration")
public class GitHubServiceTest {

    @Mock
    private ExceptionNotifyProperties properties;

    @Mock
    private ExceptionNotifyProperties.GitHub githubProperties;

    @InjectMocks
    private GitHubService gitHubService;

    private static final String TEST_FILE = "src/main/java/com/nolimit35/springfast/service/GitHubService.java";
    private static final int TEST_LINE = 15;

    @BeforeEach
    public void setUp() {
        // Set up properties with real GitHub credentials
        when(properties.getGithub()).thenReturn(githubProperties);
        when(githubProperties.getToken()).thenReturn("your_token");
        when(githubProperties.getRepoOwner()).thenReturn("GuangYiDing");
        when(githubProperties.getRepoName()).thenReturn("exception-notify");
        when(githubProperties.getBranch()).thenReturn("main");
    }

    @Test
    public void testGetAuthorInfo_Success() {
        // Call the actual service method
        CodeAuthorInfo authorInfo = gitHubService.getAuthorInfo(TEST_FILE, TEST_LINE);

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
        CodeAuthorInfo authorInfo = gitHubService.getAuthorInfo("non-existent-file.txt", 1);
        
        // Should return null for non-existent file
        assertNull(authorInfo);
    }

    @Test
    public void testGetAuthorInfo_MissingConfiguration() {
        // Test with missing token
        when(githubProperties.getToken()).thenReturn(null);

        CodeAuthorInfo authorInfo = gitHubService.getAuthorInfo(TEST_FILE, TEST_LINE);
        assertNull(authorInfo);

        // Reset token and test with missing repo owner
        when(githubProperties.getToken()).thenReturn("your_token");
        when(githubProperties.getRepoOwner()).thenReturn(null);

        authorInfo = gitHubService.getAuthorInfo(TEST_FILE, TEST_LINE);
        assertNull(authorInfo);

        // Reset repo owner and test with missing repo name
        when(githubProperties.getRepoOwner()).thenReturn("GuangYiDing");
        when(githubProperties.getRepoName()).thenReturn(null);

        authorInfo = gitHubService.getAuthorInfo(TEST_FILE, TEST_LINE);
        assertNull(authorInfo);
    }
} 