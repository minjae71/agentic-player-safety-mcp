package com.agenticplayer.safety.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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
        ProductSafetyService service = new ProductSafetyService(new StubClient(List.of(matchingRecall)));

        var result = service.search("mini trampoline", "SEGMART", "SOSTT051BR", 5);

        assertThat(result.recalls()).hasSize(1);
        assertThat(result.recalls().get(0).matchScore()).isEqualTo(100);
        assertThat(result.recalls().get(0).matchConfidence()).isEqualTo("높음");
    }

    @Test
    void checklistDoesNotRequireStoredUserData() {
        ProductSafetyService service = new ProductSafetyService(new StubClient(List.of()));

        var checklist = service.checklist("전기포트", "Example", "K100", "해외직구");

        assertThat(checklist.labelChecks()).hasSize(3);
        assertThat(checklist.nextActions()).anyMatch(item -> item.contains("K100"));
    }

    private static final class StubClient extends CpscRecallClient {

        private final List<CpscRecall> recalls;

        private StubClient(List<CpscRecall> recalls) {
            super(null, null);
            this.recalls = recalls;
        }

        @Override
        public List<CpscRecall> search(String productName, int requestedLimit) {
            return recalls;
        }

        @Override
        public Optional<CpscRecall> findById(long recallId) {
            return recalls.stream().filter(recall -> recall.recallId() == recallId).findFirst();
        }
    }
}
