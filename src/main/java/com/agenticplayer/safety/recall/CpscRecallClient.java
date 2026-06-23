package com.agenticplayer.safety.recall;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CpscRecallClient {

    private final RestClient restClient;
    private final CpscProperties properties;

    public CpscRecallClient(RestClient cpscRestClient, CpscProperties properties) {
        this.restClient = cpscRestClient;
        this.properties = properties;
    }

    public List<CpscRecall> search(String productName, int requestedLimit) {
        try {
            CpscRecall[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/RestWebServices/Recall")
                            .queryParam("ProductName", productName)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(CpscRecall[].class);

            int limit = Math.min(normalizeLimit(requestedLimit), properties.maxResults());
            return response == null ? List.of() : Arrays.stream(response).limit(limit).toList();
        }
        catch (RestClientException exception) {
            throw new RecallSourceUnavailableException("CPSC 리콜 정보를 불러오지 못했습니다.", exception);
        }
    }

    public Optional<CpscRecall> findById(long recallId) {
        try {
            CpscRecall[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/RestWebServices/Recall")
                            .queryParam("RecallID", recallId)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(CpscRecall[].class);

            return response == null ? Optional.empty() : Arrays.stream(response).findFirst();
        }
        catch (RestClientException exception) {
            throw new RecallSourceUnavailableException("CPSC 리콜 상세 정보를 불러오지 못했습니다.", exception);
        }
    }

    private int normalizeLimit(int requestedLimit) {
        return requestedLimit <= 0 ? 5 : requestedLimit;
    }
}
