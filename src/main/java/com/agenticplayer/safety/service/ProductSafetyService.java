package com.agenticplayer.safety.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.agenticplayer.safety.recall.CpscRecall;
import com.agenticplayer.safety.recall.CpscRecallClient;
import com.agenticplayer.safety.recall.RecallSourceUnavailableException;
import com.agenticplayer.safety.tool.ProductSafetyResponse;

@Service
public class ProductSafetyService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    private final CpscRecallClient recallClient;
    private final Executor recallSearchExecutor;

    public ProductSafetyService(
            CpscRecallClient recallClient,
            @Qualifier("recallSearchExecutor") Executor recallSearchExecutor) {
        this.recallClient = recallClient;
        this.recallSearchExecutor = recallSearchExecutor;
    }

    public ProductSafetyResponse.SearchResult search(String productName, String brand, String model, int limit) {
        firstNotBlank(productName, brand, model);
        List<String> searchedQueries = buildSearchQueries(productName, brand, model);
        SearchAggregation aggregation = searchAndMerge(searchedQueries, limit);

        List<ProductSafetyResponse.RecallSummary> matches = aggregation.recalls().stream()
                .map(recall -> toSummary(recall, productName, brand, model))
                .sorted((left, right) -> Integer.compare(right.matchScore(), left.matchScore()))
                .limit(normalizeLimit(limit))
                .toList();
        Verification verification = determineVerification(model, matches);

        return new ProductSafetyResponse.SearchResult(
                productName,
                brand,
                model,
                searchedQueries,
                aggregation.failedQueries(),
                verification.status(),
                verification.exactModelMatch(),
                verification.message(),
                "미국 소비자제품안전위원회(CPSC) 공식 리콜 API",
                "미국에서 발표된 소비자제품 리콜만 조회합니다. 검색 결과가 없더라도 안전이 보장되는 것은 아닙니다.",
                matches);
    }

    public Optional<ProductSafetyResponse.RecallDetail> detail(long recallId) {
        return recallClient.findById(recallId).map(this::toDetail);
    }

    public ProductSafetyResponse.SafetyChecklist checklist(
            String productName,
            String brand,
            String model,
            String purchaseChannel) {
        List<String> labelChecks = List.of(
                "제품 본체·포장·설명서에서 브랜드와 정확한 모델 번호를 확인하세요.",
                "제조번호, 로트번호, 생산일자, UPC가 있다면 함께 기록하세요.",
                "리콜 사진과 색상·부품 구성·라벨 위치가 같은지 비교하세요.");
        List<String> stopRules = List.of(
                "모델이나 제조번호가 리콜 범위와 일치하면 즉시 사용을 중지하세요.",
                "화재·질식·감전·추락 위험이 언급된 경우 확인 전까지 어린이와 분리 보관하세요.",
                "임의 수리나 폐기 전에 공식 환불·교환 절차와 증빙 요구사항을 확인하세요.");
        List<String> nextActions = List.of(
                "이 체크리스트만으로 리콜 대상 여부를 판정하지 마세요.",
                "리콜 여부는 공식 검색 결과와 상세 공고의 모델 범위를 확인하세요.",
                "구매처가 " + valueOrUnknown(purchaseChannel) + "인 경우 주문내역과 판매자 정보를 보관하세요.");

        return new ProductSafetyResponse.SafetyChecklist(
                productName,
                brand,
                model,
                labelChecks,
                stopRules,
                nextActions);
    }

    private ProductSafetyResponse.RecallSummary toSummary(
            CpscRecall recall,
            String productName,
            String brand,
            String model) {
        int score = calculateMatchScore(recall, productName, brand, model);
        String confidence = score >= 80 ? "높음" : score >= 45 ? "중간" : "낮음";

        return new ProductSafetyResponse.RecallSummary(
                recall.recallId(),
                recall.recallDate() == null ? null : recall.recallDate().toLocalDate(),
                recall.title(),
                firstName(recall.products()),
                firstName(recall.hazards()),
                firstName(recall.remedies()),
                recall.url(),
                !hasText(model) ? null : recallContains(recall, model),
                score,
                confidence);
    }

    private ProductSafetyResponse.RecallDetail toDetail(CpscRecall recall) {
        return new ProductSafetyResponse.RecallDetail(
                recall.recallId(),
                recall.recallNumber(),
                recall.recallDate() == null ? null : recall.recallDate().toLocalDate(),
                recall.title(),
                recall.description(),
                recall.products().stream().map(CpscRecall.NamedValue::name).filter(this::hasText).toList(),
                recall.hazards().stream().map(CpscRecall.NamedValue::name).filter(this::hasText).toList(),
                recall.injuries().stream().map(CpscRecall.NamedValue::name).filter(this::hasText).toList(),
                recall.remedies().stream().map(CpscRecall.NamedValue::name).filter(this::hasText).toList(),
                recall.remedyOptions().stream().map(CpscRecall.RemedyOption::option).filter(this::hasText).toList(),
                recall.consumerContact(),
                recall.images().stream().map(CpscRecall.RecallImage::url).filter(this::hasText).limit(3).toList(),
                recall.url());
    }

    private int calculateMatchScore(CpscRecall recall, String productName, String brand, String model) {
        String haystack = normalizedRecallText(recall);

        int score = 0;
        score += contains(haystack, model) ? 55 : 0;
        score += contains(haystack, brand) ? 25 : 0;
        score += containsProductKeyword(haystack, productName, brand, model) ? 20 : 0;
        return Math.min(score, 100);
    }

    private SearchAggregation searchAndMerge(List<String> queries, int requestedLimit) {
        int perQueryLimit = Math.max(normalizeLimit(requestedLimit), 5);
        List<CompletableFuture<QuerySearchResult>> searches = queries.stream()
                .map(query -> CompletableFuture
                        .supplyAsync(() -> recallClient.search(query, perQueryLimit), recallSearchExecutor)
                        .handle((recalls, error) -> new QuerySearchResult(
                                query,
                                error == null ? recalls : List.of(),
                                error != null)))
                .toList();

        Map<String, CpscRecall> uniqueRecalls = new LinkedHashMap<>();
        List<QuerySearchResult> results = searches.stream().map(CompletableFuture::join).toList();
        results.stream()
                .flatMap(result -> result.recalls().stream())
                .forEach(recall -> uniqueRecalls.putIfAbsent(recallKey(recall), recall));
        List<String> failedQueries = results.stream()
                .filter(QuerySearchResult::failed)
                .map(QuerySearchResult::query)
                .toList();
        if (failedQueries.size() == queries.size()) {
            throw new RecallSourceUnavailableException(
                    "CPSC 리콜 검색 요청이 모두 실패했습니다. 잠시 후 다시 시도해 주세요.",
                    new IllegalStateException("Failed queries: " + failedQueries));
        }
        return new SearchAggregation(new ArrayList<>(uniqueRecalls.values()), failedQueries);
    }

    private Verification determineVerification(
            String model,
            List<ProductSafetyResponse.RecallSummary> recalls) {
        if (hasText(model)) {
            boolean exactModelMatch = recalls.stream()
                    .anyMatch(summary -> Boolean.TRUE.equals(summary.requestedModelMatched()));
            if (exactModelMatch) {
                return new Verification(
                        "CONFIRMED_RECALL_MATCH",
                        true,
                        "요청한 모델 번호가 공식 리콜 공고 내용에서 확인되었습니다.");
            }
            return new Verification(
                    "MODEL_NOT_CONFIRMED",
                    false,
                    "조회한 CPSC 기록에서는 요청 모델 번호의 정확한 일치를 확인하지 못했습니다. "
                            + "이는 리콜 대상이 아니거나 안전하다는 뜻이 아닙니다. "
                            + "모델 표기, 제조번호, 생산일자와 제조사 또는 CPSC의 최신 공고를 추가로 확인하세요.");
        }
        if (recalls.isEmpty()) {
            return new Verification(
                    "NO_CANDIDATES_FOUND",
                    false,
                    "입력 조건으로 리콜 후보를 찾지 못했지만 제품의 안전 또는 비리콜을 보장하지 않습니다.");
        }
        return new Verification(
                "CANDIDATES_FOUND",
                false,
                "제품명 또는 브랜드와 관련된 리콜 후보입니다. 정확한 모델 번호와 상세 공고를 대조하세요.");
    }

    private List<String> buildSearchQueries(String productName, String brand, String model) {
        Set<String> queries = new LinkedHashSet<>();
        String categoryQuery = removeIdentityTerms(productName, brand, model);

        addQuery(queries, translateKnownKoreanCategory(categoryQuery));
        addQuery(queries, categoryQuery);
        addQuery(queries, productName);
        addQuery(queries, brand);

        if (queries.isEmpty()) {
            addQuery(queries, model);
        }
        return List.copyOf(queries);
    }

    private String removeIdentityTerms(String productName, String brand, String model) {
        if (!hasText(productName)) {
            return "";
        }
        String result = productName;
        if (hasText(brand)) {
            result = result.replaceAll("(?i)" + Pattern.quote(brand), " ");
        }
        if (hasText(model)) {
            result = result.replaceAll("(?i)" + Pattern.quote(model), " ");
        }
        return result.replaceAll("[^\\p{L}\\p{N}\\s-]", " ").replaceAll("\\s+", " ").trim();
    }

    private String translateKnownKoreanCategory(String value) {
        if (!hasText(value)) {
            return "";
        }
        String translated = value;
        Map<String, String> aliases = Map.of(
                "트램펄린", "trampoline",
                "전기포트", "electric kettle",
                "헬멧", "helmet",
                "유모차", "stroller",
                "서랍장", "dresser");
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            translated = translated.replace(alias.getKey(), alias.getValue());
        }
        return translated.replaceAll("\\s+", " ").trim();
    }

    private void addQuery(Set<String> queries, String query) {
        if (!hasText(query)) {
            return;
        }
        String trimmed = query.trim();
        boolean duplicate = queries.stream().anyMatch(existing -> existing.equalsIgnoreCase(trimmed));
        if (!duplicate) {
            queries.add(trimmed);
        }
    }

    private String recallKey(CpscRecall recall) {
        if (recall.recallId() != null) {
            return "id:" + recall.recallId();
        }
        return "fallback:" + valueOrEmpty(recall.url()) + ":" + valueOrEmpty(recall.title());
    }

    private boolean recallContains(CpscRecall recall, String value) {
        return contains(normalizedRecallText(recall), value);
    }

    private String normalizedRecallText(CpscRecall recall) {
        return normalize(String.join(" ",
                valueOrEmpty(recall.title()),
                valueOrEmpty(recall.description()),
                recall.products().stream()
                        .map(CpscRecall.NamedValue::name)
                        .filter(this::hasText)
                        .reduce("", (left, right) -> left + " " + right),
                recall.productUpcs().stream()
                        .map(CpscRecall.ProductUpc::upc)
                        .filter(this::hasText)
                        .reduce("", (left, right) -> left + " " + right)));
    }

    private boolean containsProductKeyword(
            String normalizedHaystack,
            String productName,
            String brand,
            String model) {
        if (!hasText(productName)) {
            return false;
        }
        String category = removeIdentityTerms(productName, brand, model);
        String translated = translateKnownKoreanCategory(category);
        return Pattern.compile("[\\p{L}\\p{N}-]+")
                .matcher(translated)
                .results()
                .map(match -> match.group())
                .filter(token -> token.length() >= 3)
                .anyMatch(token -> contains(normalizedHaystack, token));
    }

    private int normalizeLimit(int requestedLimit) {
        return requestedLimit <= 0 ? 5 : Math.min(requestedLimit, 10);
    }

    private boolean contains(String normalizedHaystack, String needle) {
        return hasText(needle) && normalizedHaystack.contains(normalize(needle));
    }

    private String normalize(String value) {
        return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String firstName(List<CpscRecall.NamedValue> values) {
        return values.stream().map(CpscRecall.NamedValue::name).filter(this::hasText).findFirst().orElse(null);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        throw new IllegalArgumentException("제품명, 브랜드, 모델 중 하나는 반드시 입력해야 합니다.");
    }

    private String valueOrUnknown(String value) {
        return hasText(value) ? value : "미상";
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QuerySearchResult(String query, List<CpscRecall> recalls, boolean failed) {
    }

    private record SearchAggregation(List<CpscRecall> recalls, List<String> failedQueries) {
    }

    private record Verification(String status, boolean exactModelMatch, String message) {
    }

}
