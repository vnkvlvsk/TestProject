package com.example.limitservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.limitservice.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

class ValidationApiTest extends IntegrationTestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private RestClient.Builder restClientBuilder;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = restClientBuilder.baseUrl("http://localhost:" + port).build();
    }

    @Test
    void invalidTransactionReturnsProblemDetailWithFieldErrors() {
        String body = """
                {
                  "account_from": "123",
                  "account_to": "9999999999",
                  "currency_shortname": "KZT",
                  "sum": 10000.456,
                  "expense_category": "product",
                  "datetime": "2022-01-30T00:00:00+06:00"
                }
                """;

        ResponseEntity<Map> response = restClient.post()
                .uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, resp) -> { })
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).containsEntry("status", 400);
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");
        assertThat(errors).containsKeys("accountFrom", "sum");
    }

    @Test
    void invalidAccountInQueryReturnsProblemDetail() {
        ResponseEntity<Map> response = restClient.get()
                .uri("/api/client/limits?account=abc")
                .retrieve()
                .onStatus(status -> true, (request, resp) -> { })
                .toEntity(Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
