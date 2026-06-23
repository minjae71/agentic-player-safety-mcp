package com.agenticplayer.safety.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.agenticplayer.safety.service.ProductSafetyService;

@Component
public class ProductSafetyTools {

    private final ProductSafetyService service;

    public ProductSafetyTools(ProductSafetyService service) {
        this.service = service;
    }

    @Tool(
            name = "search_product_recalls",
            description = """
                    SafetyCheck(안심구매) searches official U.S. CPSC consumer-product recalls by product name,
                    brand, or model number. Use it to identify possible fire, electric-shock, choking, fall,
                    and other hazards before buying or using imported and secondhand products.
                    """)
    public ProductSafetyResponse.SearchResult searchProductRecalls(
            @ToolParam(description = "확인할 제품명. 예: toddler trampoline", required = true)
            String productName,
            @ToolParam(description = "브랜드명. 모르면 빈 문자열", required = false)
            String brand,
            @ToolParam(description = "제품 라벨의 정확한 모델 번호. 모르면 빈 문자열", required = false)
            String model,
            @ToolParam(description = "가져올 최대 결과 수. 권장 3~5, 최대 10", required = false)
            int limit) {
        return service.search(productName, brand, model, limit);
    }

    @Tool(
            name = "get_recall_details",
            description = """
                    SafetyCheck(안심구매) retrieves official recall details for a recallId returned by
                    search_product_recalls, including affected products, hazards, incidents, remedies,
                    consumer contact information, images, and the official source URL.
                    """)
    public Object getRecallDetails(
            @ToolParam(description = "CPSC 리콜 식별자", required = true)
            long recallId) {
        return service.detail(recallId)
                .<Object>map(detail -> detail)
                .orElseGet(() -> new ProductSafetyResponse.RecallNotFound(
                        recallId,
                        "해당 recallId의 공식 리콜을 찾지 못했습니다."));
    }

    @Tool(
            name = "create_product_safety_checklist",
            description = """
                    SafetyCheck(안심구매) creates a compact checklist for comparing a physical product with
                    an official recall notice. It covers model and lot labels, immediate stop-use rules,
                    evidence to retain, and recommended next actions for imported or secondhand products.
                    """)
    public ProductSafetyResponse.SafetyChecklist createProductSafetyChecklist(
            @ToolParam(description = "확인할 제품명", required = true)
            String productName,
            @ToolParam(description = "브랜드명. 모르면 빈 문자열", required = false)
            String brand,
            @ToolParam(description = "모델 번호. 모르면 빈 문자열", required = false)
            String model,
            @ToolParam(description = "구매처 또는 구매 경로. 예: Amazon, 중고거래", required = false)
            String purchaseChannel) {
        return service.checklist(productName, brand, model, purchaseChannel);
    }
}
