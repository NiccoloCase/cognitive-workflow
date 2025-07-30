package org.caselli.cognitiveworkflow.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Configuration class for dotenv file loading.
 * @author niccolocaselli
 */
@Configuration
public class DotenvConfig {

    @Bean
    public PropertySource<?> dotenvPropertySource() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .filename(".env")
                .ignoreIfMissing()
                .load();

        return new MapPropertySource("dotenv", dotenv.entries().stream()
                .collect(java.util.stream.Collectors.toMap(dotenvEntry -> dotenvEntry.getKey(),
                        dotenvEntry -> dotenvEntry.getValue())));
    }
}