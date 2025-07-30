package org.caselli.cognitiveworkflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration class for MongoDB settings.
 * @author niccolocaselli
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig { }