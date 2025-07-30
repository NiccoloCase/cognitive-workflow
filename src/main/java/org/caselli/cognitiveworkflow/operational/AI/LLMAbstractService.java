package org.caselli.cognitiveworkflow.operational.AI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for services that use an LLM via a Spring AI ChatClient.
 * Provides lazy initialization and logger support.
 * Subclasses must implement {@link #buildChatClient()} to provide a specific ChatClient
 * configuration.
 * @author niccolocaselli
 */
public abstract class LLMAbstractService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private ChatClient chatClient;

    public ChatClient getChatClient() {
        if (chatClient == null) {
            chatClient = buildChatClient();
        }
        return chatClient;
    }

    protected abstract ChatClient buildChatClient();
}
