package com.example.limitservice.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.ExchangeRateRepository;
import com.example.limitservice.repository.SpendingLimitRepository;
import com.example.limitservice.repository.TransactionRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
public abstract class IntegrationTestBase {

    protected static final WireMockServer WIRE_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        WIRE_MOCK.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.twelvedata.base-url", WIRE_MOCK::baseUrl);
        registry.add("app.twelvedata.retry-delay", () -> "10ms");
        registry.add("app.limits.pending-retry-delay", () -> "1h");
    }

    @Autowired
    protected MutableClock clock;
    @Autowired
    protected TransactionRepository transactionRepository;
    @Autowired
    protected SpendingLimitRepository limitRepository;
    @Autowired
    protected ExchangeRateRepository exchangeRateRepository;
    @Autowired
    protected AccountRepository accountRepository;

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
        limitRepository.deleteAll();
        exchangeRateRepository.deleteAll();
        accountRepository.deleteAll();

        WIRE_MOCK.resetAll();
        stubRate("KZT", "500.00");
    }

    protected static void stubRate(String currency, String close) {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/time_series"))
                .withQueryParam("symbol", equalTo("USD/" + currency))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "meta": {"symbol": "USD/%s", "interval": "1day"},
                                  "values": [{"datetime": "2021-12-31", "close": "%s"}],
                                  "status": "ok"
                                }
                                """.formatted(currency, close))));
    }

    protected static void stubRateError(String currency) {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/time_series"))
                .withQueryParam("symbol", equalTo("USD/" + currency))
                .willReturn(aResponse().withStatus(500)));
    }
}
