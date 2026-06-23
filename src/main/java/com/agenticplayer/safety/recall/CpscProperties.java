package com.agenticplayer.safety.recall;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safety.cpsc")
public record CpscProperties(
        String baseUrl,
        int readTimeoutMillis,
        int maxResults) {

    public CpscProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://www.saferproducts.gov";
        }
        if (readTimeoutMillis <= 0) {
            readTimeoutMillis = 8_000;
        }
        if (maxResults <= 0) {
            maxResults = 10;
        }
    }
}
