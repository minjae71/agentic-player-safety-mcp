package com.agenticplayer.safety.recall;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safety.cpsc")
public record CpscProperties(
        String baseUrl,
        int connectTimeoutMillis,
        int readTimeoutMillis,
        int maxResults) {

    public CpscProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://www.saferproducts.gov";
        }
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = 800;
        }
        if (readTimeoutMillis <= 0 || readTimeoutMillis > 2_500) {
            readTimeoutMillis = 2_500;
        }
        if (maxResults <= 0) {
            maxResults = 10;
        }
    }
}
