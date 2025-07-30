package org.caselli.cognitiveworkflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Helper component for checking the application environment
 * @author niccolocaselli
 */
@Component
public class EnvironmentHelper {

    private final boolean isDev;

    public EnvironmentHelper(@Value("${app.environment:prod}") String appEnvironment) {
        this.isDev = "development".equalsIgnoreCase(appEnvironment);
    }

    public boolean isDev() {
        return this.isDev;
    }

    public boolean isProd() {
        return !this.isDev;
    }
}
