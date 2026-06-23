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
        ProductSafetyService service = new ProductSafetyService(client);

        var result = service.search("mini trampoline", "SEGMART", "SOSTT051BR", 5);

        assertThat(result.recalls()).hasSize(1);
        assertThat(result.recalls().get(0).matchScore()).isEqualTo(100);
        assertThat(result.recalls().get(0).matchConfidence()).isEqualTo("높음");
    }

    @Test
    void splitsCombinedBrandAndProductQueryThenMatchesModelInDescription() {
        StubClient client = new StubClient();
        client.respond("trampoline", List.of(matchingRecall));
        client.respond("SEGMART", List.of(matchingRecall));
        ProductSafetyService service = new ProductSafetyService(client);

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
        ProductSafetyService service = new ProductSafetyService(client);

        var result = service.search("SEGMART 미니 트램펄린", "SEGMART", "SOSTT051BR", 5);

        assertThat(result.searchedQueries()).contains("미니 trampoline");
        assertThat(result.recalls()).hasSize(1);
    }

    @Test
    void checklistDoesNotRequireStoredUserData() {
        ProductSafetyService service = new ProductSafetyService(new StubClient());

        var checklist = service.checklist("전기포트", "Example", "K100", "해외직구");

        assertThat(checklist.labelChecks()).hasSize(3);
        assertThat(checklist.nextActions()).anyMatch(item -> item.contains("K100"));
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
