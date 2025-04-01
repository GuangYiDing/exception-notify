package com.nolimit35.springfast.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Listener to set the current environment from spring.profiles.active
 */
@Slf4j
@Component
public class EnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String activeProfile = getActiveProfile(environment);
        
        if (activeProfile != null) {
            // Set the current environment property based on the active profile
            System.setProperty("exception.notify.environment.current", activeProfile);
            log.debug("Set exception.notify.environment.current to active profile: {}", activeProfile);
        }
    }
    
    /**
     * Get the active profile from Spring environment
     * 
     * @param environment the Spring environment
     * @return the active profile or null if not found
     */
    private String getActiveProfile(Environment environment) {
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
        
        return null;
    }
} 