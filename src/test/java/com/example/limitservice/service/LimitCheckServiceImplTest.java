package com.example.limitservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.limitservice.config.LimitProperties;
import com.example.limitservice.entity.Account;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;
import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitCheckServiceImplTest {

    private static final String ACCOUNT = "0000000123";
    private static final OffsetDateTime DATETIME = OffsetDateTime.parse("2022-01-13T12:00:00+06:00");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private LimitService limitService;

    private LimitCheckServiceImpl limitCheckService;

    @BeforeEach
    void setUp() {
        LimitProperties properties = new LimitProperties(ZoneOffset.UTC, new BigDecimal("1000.00"), Duration.ofMinutes(1));
        limitCheckService = new LimitCheckServiceImpl(accountRepository, transactionRepository, limitService, properties);
    }

    @Test
    void notExceededWhenRemainderIsExactlyZero() {
        givenLimitAndSpent("2000.00", "1900.00");

        Transaction result = limitCheckService.checkAndSave(transaction("100.00"), BigDecimal.ONE);

        assertThat(result.getLimitExceeded()).isFalse();
        assertThat(result.getSumUsd()).isEqualByComparingTo("100.00");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    }

    @Test
    void exceededWhenRemainderBecomesNegative() {
        givenLimitAndSpent("2000.00", "2000.00");

        Transaction result = limitCheckService.checkAndSave(transaction("100.00"), BigDecimal.ONE);

        assertThat(result.getLimitExceeded()).isTrue();
    }

    @Test
    void sumIsConvertedToUsdBeforeCheck() {
        givenLimitAndSpent("1000.00", "500.00");

        Transaction result = limitCheckService.checkAndSave(transaction("300000.00"), new BigDecimal("500"));

        assertThat(result.getSumUsd()).isEqualByComparingTo("600.00");
        assertThat(result.getLimitExceeded()).isTrue();
    }

    @Test
    void spentSumIsCalculatedForCalendarMonthInUtc() {
        givenLimitAndSpent("1000.00", "0.00");

        limitCheckService.checkAndSave(transaction("100.00"), BigDecimal.ONE);

        verify(transactionRepository).sumUsdForPeriod(ACCOUNT, ExpenseCategory.PRODUCT,
                OffsetDateTime.parse("2022-01-01T00:00:00Z"), OffsetDateTime.parse("2022-02-01T00:00:00Z"));
    }

    @Test
    void failsWhenAccountDoesNotExist() {
        when(accountRepository.findByIdForUpdate(ACCOUNT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> limitCheckService.checkAndSave(transaction("100.00"), BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class);
    }

    private void givenLimitAndSpent(String limitSum, String spent) {
        SpendingLimit limit = new SpendingLimit();
        limit.setLimitSum(new BigDecimal(limitSum));

        when(accountRepository.findByIdForUpdate(ACCOUNT)).thenReturn(Optional.of(new Account()));
        when(limitService.getActiveLimit(ACCOUNT, ExpenseCategory.PRODUCT, DATETIME)).thenReturn(limit);
        when(transactionRepository.sumUsdForPeriod(any(), any(), any(), any())).thenReturn(new BigDecimal(spent));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Transaction transaction(String sum) {
        Transaction transaction = new Transaction();
        transaction.setAccountFrom(ACCOUNT);
        transaction.setSum(new BigDecimal(sum));
        transaction.setExpenseCategory(ExpenseCategory.PRODUCT);
        transaction.setDatetime(DATETIME);
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }
}
