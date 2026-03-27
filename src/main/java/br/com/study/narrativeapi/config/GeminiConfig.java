package br.com.study.narrativeapi.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    /**
     * Cria o bean ChatClient a partir do ChatModel auto-configurado pelo Spring AI.
     * O ChatModel é configurado automaticamente via application.yml:
     * spring.ai.google.genai.api-key, model, temperature, max-output-tokens
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}