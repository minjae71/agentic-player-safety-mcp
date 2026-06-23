package com.agenticplayer.safety.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.agenticplayer.safety.tool.ProductSafetyTools;

@Configuration
public class McpToolConfig {

    @Bean
    List<McpServerFeatures.SyncToolSpecification> productSafetyToolSpecifications(ProductSafetyTools tools) {
        ToolCallbackProvider provider = MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();

        return Arrays.stream(provider.getToolCallbacks())
                .map(McpToolUtils::toSyncToolSpecification)
                .map(this::withRequiredAnnotations)
                .toList();
    }

    private McpServerFeatures.SyncToolSpecification withRequiredAnnotations(
            McpServerFeatures.SyncToolSpecification specification) {
        McpSchema.Tool source = specification.tool();
        McpSchema.ToolAnnotations annotations = new McpSchema.ToolAnnotations(
                toolTitle(source.name()),
                true,
                false,
                true,
                true,
                false);

        McpSchema.Tool annotatedTool = McpSchema.Tool.builder()
                .name(source.name())
                .title(toolTitle(source.name()))
                .description(source.description())
                .inputSchema(source.inputSchema())
                .outputSchema(source.outputSchema())
                .annotations(annotations)
                .meta(source.meta() == null ? Map.of() : source.meta())
                .build();

        return new McpServerFeatures.SyncToolSpecification(
                annotatedTool,
                specification.call(),
                specification.callHandler());
    }

    private String toolTitle(String toolName) {
        return switch (toolName) {
            case "search_product_recalls" -> "Search product recalls / 제품 리콜 검색";
            case "get_recall_details" -> "Get recall details / 리콜 상세 조회";
            case "create_product_safety_checklist" -> "Create safety checklist / 안전 체크리스트";
            default -> toolName;
        };
    }
}
