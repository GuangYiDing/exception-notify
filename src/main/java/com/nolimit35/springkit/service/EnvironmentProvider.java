package com.nolimit35.springkit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Service to provide the current environment from Spring's active profiles
 */
@Slf4j
@Component
public class EnvironmentProvider {
    
    private final Environment environment;
    
    @Autowired
    public EnvironmentProvider(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Get the current environment from Spring's active profiles
     * 
     * @return the current environment or "dev" if not found
     */
    public String getCurrentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        if (activeProfiles.length > 0) {
            // Use the first active profile
            return activeProfiles[0];
        }
        
        // Check default profiles if no active profiles found
        String[] defaultProfiles = environment.getDefaultProfiles();
        if (defaultProfiles.length > 0) {
            return defaultProfiles[0];
        }
        
        // Default to "dev" if no profiles found
        return "dev";
    }
} 