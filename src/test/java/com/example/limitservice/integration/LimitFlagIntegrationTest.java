package com.example.limitservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.dto.TransactionRequest;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;
import com.example.limitservice.service.LimitService;
import com.example.limitservice.service.TransactionService;
import com.example.limitservice.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LimitFlagIntegrationTest extends IntegrationTestBase {

    private static final String ACCOUNT = "0000000123";
    private static final String COUNTERPARTY = "9999999999";

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private LimitService limitService;

    @Test
    void caseTwoFromTaskTable() {
        setLimitAt("2022-02-01T09:00:00Z", "1000.00");
        assertThat(send("500.00", "2022-02-02T12:00:00Z").getLimitExceeded()).isFalse();
        assertThat(send("100.00", "2022-02-03T12:00:00Z").getLimitExceeded()).isFalse();

        setLimitAt("2022-02-10T09:00:00Z", "400.00");
        assertThat(send("100.00", "2022-02-11T12:00:00Z").getLimitExceeded()).isTrue();
        assertThat(send("100.00", "2022-02-12T12:00:00Z").getLimitExceeded()).isTrue();

        List<ExceededTransactionResponse> exceeded = transactionService.getExceededTransactions(ACCOUNT);
        assertThat(exceeded).hasSize(2);
        assertThat(exceeded).allSatisfy(transaction -> {
            assertThat(transaction.limitSum()).isEqualByComparingTo("400.00");
            assertThat(transaction.limitDatetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-02-10T09:00:00Z"));
        });
    }

    @Test
    void limitChangedInsideMonthCountsAlreadySpentSum() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");
        assertThat(send("800.00", "2022-01-05T12:00:00Z").getLimitExceeded()).isFalse();

        setLimitAt("2022-01-10T00:00:00Z", "900.00");
        assertThat(send("100.00", "2022-01-11T12:00:00Z").getLimitExceeded()).isFalse();
        assertThat(send("0.01", "2022-01-11T13:00:00Z").getLimitExceeded()).isTrue();
    }

    @Test
    void remainderExactlyZeroIsNotExceeded() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");

        assertThat(send("1000.00", "2022-01-05T12:00:00Z").getLimitExceeded()).isFalse();
        assertThat(send("0.01", "2022-01-05T13:00:00Z").getLimitExceeded()).isTrue();
    }

    @Test
    void newMonthStartsWithFullLimit() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");
        assertThat(send("900.00", "2022-01-31T23:59:59Z").getLimitExceeded()).isFalse();

        assertThat(send("900.00", "2022-02-01T00:00:00Z").getLimitExceeded()).isFalse();
    }

    @Test
    void monthBorderIsCalculatedInUtcForTransactionsWithOffset() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");
        assertThat(send("900.00", "2022-01-15T12:00:00Z").getLimitExceeded()).isFalse();

        assertThat(send("200.00", "2022-02-01T03:00:00+06:00").getLimitExceeded()).isTrue();
    }

    @Test
    void oldLimitIsUsedForTransactionsMadeBeforeNewLimit() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");
        setLimitAt("2022-01-10T00:00:00Z", "5000.00");

        assertThat(send("1500.00", "2022-01-05T12:00:00Z").getLimitExceeded()).isTrue();
    }

    @Test
    void defaultLimitIs1000UsdWhenClientDidNotSetLimit() {
        assertThat(send("1000.00", "2022-03-05T12:00:00Z").getLimitExceeded()).isFalse();
        assertThat(send("1.00", "2022-03-06T12:00:00Z").getLimitExceeded()).isTrue();

        List<SpendingLimit> limits = limitService.getLimits(ACCOUNT);
        assertThat(limits).hasSize(1);
        assertThat(limits.get(0).getLimitSum()).isEqualByComparingTo("1000.00");
        assertThat(limits.get(0).getLimitDatetime()).isAtSameInstantAs(OffsetDateTime.parse("2022-03-01T00:00:00Z"));
    }

    @Test
    void limitsOfProductsAndServicesAreSeparate() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");

        assertThat(send("1000.00", "2022-01-05T12:00:00Z").getLimitExceeded()).isFalse();
        Transaction service = transactionService.acceptTransaction(new TransactionRequest(ACCOUNT, COUNTERPARTY,
                "USD", new BigDecimal("500.00"), ExpenseCategory.SERVICE, OffsetDateTime.parse("2022-01-06T12:00:00Z")));
        assertThat(service.getLimitExceeded()).isFalse();
    }

    @Test
    void parallelTransactionsDoNotUseSameRemainder() throws Exception {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");

        List<Future<Transaction>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 20; i++) {
                futures.add(executor.submit(() -> send("100.00", "2022-01-05T12:00:00Z")));
            }
        }

        long notExceeded = 0;
        for (Future<Transaction> future : futures) {
            if (!future.get().getLimitExceeded()) {
                notExceeded++;
            }
        }
        assertThat(notExceeded).isEqualTo(10);
    }

    @Test
    void transactionIsConvertedWithRateFromDatabaseOrApi() {
        setLimitAt("2022-01-01T00:00:00Z", "1000.00");

        Transaction transaction = sendInCurrency("KZT", "250000.00", "2022-01-05T12:00:00Z");

        assertThat(transaction.getSumUsd()).isEqualByComparingTo("500.00");
        assertThat(exchangeRateRepository.findAll()).hasSize(1);

        sendInCurrency("KZT", "1000.00", "2022-01-05T15:00:00Z");
        assertThat(WIRE_MOCK.getAllServeEvents()).hasSize(1);
    }

    @Test
    void transactionIsSavedAsPendingWhenApiFailsAndProcessedLater() {
        stubRateError("RUB");

        Transaction transaction = sendInCurrency("RUB", "9000.00", "2022-01-05T12:00:00Z");

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getLimitExceeded()).isNull();

        stubRate("RUB", "90.00");
        transactionService.processPendingTransactions();

        Transaction processed = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
        assertThat(processed.getSumUsd()).isEqualByComparingTo("100.00");
        assertThat(processed.getLimitExceeded()).isFalse();
    }

    private void setLimitAt(String datetime, String limitSum) {
        clock.setTime(OffsetDateTime.parse(datetime));
        limitService.createLimit(new LimitRequest(ACCOUNT, ExpenseCategory.PRODUCT, new BigDecimal(limitSum)));
    }

    private Transaction send(String sumUsd, String datetime) {
        return sendInCurrency("USD", sumUsd, datetime);
    }

    private Transaction sendInCurrency(String currency, String sum, String datetime) {
        return transactionService.acceptTransaction(new TransactionRequest(ACCOUNT, COUNTERPARTY, currency,
                new BigDecimal(sum), ExpenseCategory.PRODUCT, OffsetDateTime.parse(datetime)));
    }
}
