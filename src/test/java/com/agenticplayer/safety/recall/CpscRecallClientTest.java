package com.agenticplayer.safety.recall;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class CpscRecallClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void parsesOfficialRecallJson() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [{
                          "RecallID": 10718,
                          "RecallNumber": "26414",
                          "RecallDate": "2026-04-16T00:00:00",
                          "Title": "Mini trampoline recall",
                          "Products": [{"Name": "SEGMART trampoline"}],
                          "Hazards": [{"Name": "Strangulation hazard"}],
                          "Remedies": [{"Name": "Remove accessories"}],
                          "RemedyOptions": [{"Option": "Refund"}]
                        }]
                        """));
        var properties = new CpscProperties(server.url("/").toString(), 1_000, 10);
        var client = new CpscRecallClient(
                RestClient.builder().requestFactory(new JdkClientHttpRequestFactory()).baseUrl(properties.baseUrl()).build(),
                properties);

        var recalls = client.search("trampoline", 5);

        assertThat(recalls).hasSize(1);
        assertThat(recalls.get(0).recallId()).isEqualTo(10718L);
        assertThat(recalls.get(0).hazards().get(0).name()).isEqualTo("Strangulation hazard");
        assertThat(server.takeRequest().getPath())
                .contains("/RestWebServices/Recall")
                .contains("ProductName=trampoline")
                .contains("format=json");
    }
}
