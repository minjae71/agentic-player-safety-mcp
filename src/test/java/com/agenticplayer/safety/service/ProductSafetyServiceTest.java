package com.agenticplayer.safety.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.agenticplayer.safety.recall.CpscRecall;
import com.agenticplayer.safety.recall.CpscRecallClient;

class ProductSafetyServiceTest {

    private final CpscRecall matchingRecall = new CpscRecall(
            10718L,
            "26414",
            LocalDateTime.of(2026, 4, 16, 0, 0),
            "SEGMART toddler trampolines recalled",
            "Models SOSTT051BR, SOSTT051CR and SOSTT051YP are included.",
            "https://www.cpsc.gov/Recalls/example",
            "Contact manufacturer",
            List.of(new CpscRecall.NamedValue("SEGMART Mini Trampoline", "", "1,200")),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(new CpscRecall.NamedValue("Strangulation hazard", null, null)),
            List.of(new CpscRecall.NamedValue("Remove accessories and request refund", null, null)),
            List.of(new CpscRecall.RemedyOption("Refund")),
            List.of());

    @Test
    void modelAndBrandIncreaseMatchScore() {
        StubClient client = new StubClient();
        client.respond("mini trampoline", List.of(matchingRecall));
        ProductSafetyService service = service(client);

        var result = service.search("mini trampoline", "SEGMART", "SOSTT051BR", 5);

        assertThat(result.recalls()).hasSize(1);
        assertThat(result.recalls().get(0).matchScore()).isEqualTo(100);
        assertThat(result.recalls().get(0).matchConfidence()).isEqualTo("높음");
        assertThat(result.verificationStatus()).isEqualTo("CONFIRMED_RECALL_MATCH");
        assertThat(result.exactModelMatch()).isTrue();
    }

    @Test
    void splitsCombinedBrandAndProductQueryThenMatchesModelInDescription() {
        StubClient client = new StubClient();
        client.respond("trampoline", List.of(matchingRecall));
        client.respond("SEGMART", List.of(matchingRecall));
        ProductSafetyService service = service(client);

        var result = service.search("SEGMART trampoline", "SEGMART", "SOSTT051BR", 5);

        assertThat(client.queries()).contains("trampoline", "SEGMART");
        assertThat(result.searchedQueries()).contains("trampoline", "SEGMART");
        assertThat(result.recalls()).hasSize(1);
        assertThat(result.recalls().get(0).recallId()).isEqualTo(10718L);
        assertThat(result.recalls().get(0).matchScore()).isEqualTo(100);
    }

    @Test
    void translatesKnownKoreanProductCategoryForCpscSearch() {
        StubClient client = new StubClient();
        client.respond("미니 trampoline", List.of(matchingRecall));
        ProductSafetyService service = service(client);

        var result = service.search("SEGMART 미니 트램펄린", "SEGMART", "SOSTT051BR", 5);

        assertThat(result.searchedQueries()).contains("미니 trampoline");
        assertThat(result.recalls()).hasSize(1);
    }

    @Test
    void searchesByBrandWhenOnlyBrandAndModelAreProvided() {
        StubClient client = new StubClient();
        client.respond("SEGMART", List.of(matchingRecall));
        ProductSafetyService service = service(client);

        var result = service.search("", "SEGMART", "SOSTT051CR", 5);

        assertThat(client.queries()).containsExactly("SEGMART");
        assertThat(result.recalls()).hasSize(1);
        assertThat(result.recalls().get(0).recallId()).isEqualTo(10718L);
        assertThat(result.recalls().get(0).matchConfidence()).isEqualTo("높음");
    }

    @Test
    void doesNotClaimThatUnknownModelHasNoRecall() {
        StubClient client = new StubClient();
        client.respond("SEGMART", List.of(matchingRecall));
        ProductSafetyService service = service(client);

        var result = service.search("", "SEGMART", "TEST-NOT-EXIST", 5);

        assertThat(result.verificationStatus()).isEqualTo("MODEL_NOT_CONFIRMED");
        assertThat(result.exactModelMatch()).isFalse();
        assertThat(result.verificationMessage())
                .contains("정확한 일치를 확인하지 못했습니다")
                .contains("리콜 대상이 아니거나 안전하다는 뜻이 아닙니다");
        assertThat(result.recalls()).isNotEmpty();
        assertThat(result.recalls())
                .allMatch(summary -> Boolean.FALSE.equals(summary.requestedModelMatched()));
    }

    @Test
    void checklistDoesNotRequireStoredUserData() {
        ProductSafetyService service = service(new StubClient());

        var checklist = service.checklist("전기포트", "Example", "K100", "해외직구");

        assertThat(checklist.labelChecks()).hasSize(3);
        assertThat(checklist.nextActions())
                .contains("이 체크리스트만으로 리콜 대상 여부를 판정하지 마세요.")
                .noneMatch(item -> item.contains("search_product_recalls"));
    }

    private ProductSafetyService service(StubClient client) {
        return new ProductSafetyService(client, Runnable::run);
    }

    private static final class StubClient extends CpscRecallClient {

        private final java.util.concurrent.ConcurrentMap<String, List<CpscRecall>> responses =
                new java.util.concurrent.ConcurrentHashMap<>();
        private final List<String> queries = java.util.Collections.synchronizedList(new ArrayList<>());

        private StubClient() {
            super(null, null);
        }

        private void respond(String query, List<CpscRecall> recalls) {
            responses.put(query.toLowerCase(), recalls);
        }

        private List<String> queries() {
            return List.copyOf(queries);
        }

        @Override
        public List<CpscRecall> search(String productName, int requestedLimit) {
            queries.add(productName);
            return responses.getOrDefault(productName.toLowerCase(), List.of());
        }

        @Override
        public Optional<CpscRecall> findById(long recallId) {
            return responses.values().stream()
                    .flatMap(List::stream)
                    .filter(recall -> recall.recallId() == recallId)
                    .findFirst();
        }
    }
}
