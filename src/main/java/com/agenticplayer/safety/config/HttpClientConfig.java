package com.agenticplayer.safety.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.agenticplayer.safety.recall.CpscProperties;

@Configuration
@EnableConfigurationProperties(CpscProperties.class)
public class HttpClientConfig {

    @Bean
    RestClient cpscRestClient(RestClient.Builder builder, CpscProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMillis());
        requestFactory.setReadTimeout(properties.readTimeoutMillis());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "AgenticPlayerSafetyMCP/0.1")
                .build();
    }
}
