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
                    제품명, 브랜드 또는 모델 번호로 미국 CPSC의 공식 소비자제품 리콜을 검색합니다.
                    해외직구 제품이나 미국에서 유통된 제품의 화재, 감전, 질식, 추락 등 위험을 확인할 때 사용하세요.
                    결과의 matchScore는 입력 정보와 리콜 설명의 문자열 일치 정도이며 최종 동일제품 판정이 아닙니다.
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
                    search_product_recalls가 반환한 recallId로 공식 리콜 상세정보를 조회합니다.
                    대상 모델 범위, 위험 내용, 사고 사례, 환불·교환·수리 방법과 공식 링크를 확인할 때 사용하세요.
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
                    리콜 검색 전후에 사용자가 제품 라벨에서 확인할 정보와 즉시 사용 중지 기준을 체크리스트로 만듭니다.
                    제품을 실제 리콜 대상과 안전하게 대조하거나 중고거래·해외직구 전에 확인할 항목이 필요할 때 사용하세요.
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
