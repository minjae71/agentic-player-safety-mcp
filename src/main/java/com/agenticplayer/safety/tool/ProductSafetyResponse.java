package com.agenticplayer.safety.tool;

import java.time.LocalDate;
import java.util.List;

public final class ProductSafetyResponse {

    private ProductSafetyResponse() {
    }

    public record SearchResult(
            String requestedProductName,
            String requestedBrand,
            String requestedModel,
            List<String> searchedQueries,
            List<String> failedQueries,
            String verificationStatus,
            boolean exactModelMatch,
            String verificationMessage,
            String source,
            String limitation,
            List<RecallSummary> recalls) {
    }

    public record RecallSummary(
            Long recallId,
            LocalDate recallDate,
            String title,
            String product,
            String hazard,
            String remedy,
            String officialUrl,
            Boolean requestedModelMatched,
            int matchScore,
            String matchConfidence) {
    }

    public record RecallDetail(
            Long recallId,
            String recallNumber,
            LocalDate recallDate,
            String title,
            String description,
            List<String> products,
            List<String> hazards,
            List<String> injuries,
            List<String> remedies,
            List<String> remedyOptions,
            String consumerContact,
            List<String> imageUrls,
            String officialUrl) {
    }

    public record RecallNotFound(long recallId, String message) {
    }

    public record SafetyChecklist(
            String productName,
            String brand,
            String model,
            List<String> labelChecks,
            List<String> immediateStopRules,
            List<String> nextActions) {
    }
}
