package com.example.limitservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.dto.TransactionRequest;
import com.example.limitservice.dto.TransactionResponse;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

class CaseOneApiTest extends IntegrationTestBase {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";

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
    void returnsTransactionsFromThirdAndThirteenthOfJanuaryWithExceededLimits() {
        clock.setTime(OffsetDateTime.parse("2022-01-01T09:00:00+06:00"));
        createLimit("1000.00");

        assertThat(sendTransaction("250000.00", "2022-01-02T12:00:00+06:00").limitExceeded()).isFalse();
        assertThat(sendTransaction("300000.00", "2022-01-03T12:00:00+06:00").limitExceeded()).isTrue();

        clock.setTime(OffsetDateTime.parse("2022-01-10T09:00:00+06:00"));
        createLimit("2000.00");

        assertThat(sendTransaction("50000.00", "2022-01-11T12:00:00+06:00").limitExceeded()).isFalse();
        assertThat(sendTransaction("350000.00", "2022-01-12T12:00:00+06:00").limitExceeded()).isFalse();
        assertThat(sendTransaction("50000.00", "2022-01-13T12:00:00+06:00").limitExceeded()).isFalse();
        assertThat(sendTransaction("50000.00", "2022-01-13T13:00:00+06:00").limitExceeded()).isTrue();

        List<ExceededTransactionResponse> exceeded = restClient.get()
                .uri("/api/client/transactions/exceeded?account={account}", ACCOUNT)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(exceeded).hasSize(2);

        ExceededTransactionResponse first = exceeded.get(0);
        assertThat(first.accountFrom()).isEqualTo(ACCOUNT);
        assertThat(first.accountTo()).isEqualTo(COUNTERPARTY);
        assertThat(first.currencyShortname()).isEqualTo("KZT");
        assertThat(first.sum()).isEqualByComparingTo("300000.00");
        assertThat(first.expenseCategory()).isEqualTo(ExpenseCategory.PRODUCT);
        assertThat(first.datetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-01-03T12:00:00+06:00"));
        assertThat(first.limitSum()).isEqualByComparingTo("1000.00");
        assertThat(first.limitDatetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-01-01T09:00:00+06:00"));
        assertThat(first.limitCurrencyShortname()).isEqualTo("USD");

        ExceededTransactionResponse second = exceeded.get(1);
        assertThat(second.sum()).isEqualByComparingTo("50000.00");
        assertThat(second.datetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-01-13T13:00:00+06:00"));
        assertThat(second.limitSum()).isEqualByComparingTo("2000.00");
        assertThat(second.limitDatetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-01-10T09:00:00+06:00"));
        assertThat(second.limitCurrencyShortname()).isEqualTo("USD");
    }

    private void createLimit(String limitSum) {
        restClient.post()
                .uri("/api/client/limits")
                .body(new LimitRequest(ACCOUNT, ExpenseCategory.PRODUCT, new BigDecimal(limitSum)))
                .retrieve()
                .toBodilessEntity();
    }

    private TransactionResponse sendTransaction(String sum, String datetime) {
        TransactionRequest request = new TransactionRequest(ACCOUNT, COUNTERPARTY, "KZT",
                new BigDecimal(sum), ExpenseCategory.PRODUCT, OffsetDateTime.parse(datetime));
        return restClient.post()
                .uri("/api/transactions")
                .body(request)
                .retrieve()
                .body(TransactionResponse.class);
    }
}
