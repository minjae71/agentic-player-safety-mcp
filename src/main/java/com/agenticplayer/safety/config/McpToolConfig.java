package com.agenticplayer.safety.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.agenticplayer.safety.tool.ProductSafetyTools;

@Configuration
public class McpToolConfig {

    @Bean
    ToolCallbackProvider productSafetyToolCallbacks(ProductSafetyTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
