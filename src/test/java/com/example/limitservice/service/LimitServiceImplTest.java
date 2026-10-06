package com.example.limitservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.example.limitservice.config.LimitProperties;
import com.example.limitservice.dto.LimitRequest;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.SpendingLimit;
import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.SpendingLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitServiceImplTest {

    private static final String ACCOUNT = "0000000123";
    private static final Instant NOW = Instant.parse("2022-01-10T03:00:00Z");

    @Mock
    private SpendingLimitRepository limitRepository;
    @Mock
    private AccountRepository accountRepository;

    private LimitServiceImpl limitService;

    @BeforeEach
    void setUp() {
        LimitProperties properties = new LimitProperties(ZoneOffset.UTC, new BigDecimal("1000.00"), Duration.ofMinutes(1));
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        limitService = new LimitServiceImpl(limitRepository, accountRepository, properties, fixedClock);
    }

    @Test
    void newLimitGetsCurrentDateAndUsdCurrency() {
        when(limitRepository.save(any(SpendingLimit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpendingLimit result = limitService.createLimit(
                new LimitRequest(ACCOUNT, ExpenseCategory.SERVICE, new BigDecimal("2000.00")));

        assertThat(result.getLimitDatetime().toInstant()).isEqualTo(NOW);
        assertThat(result.getLimitCurrencyShortname()).isEqualTo("USD");
        assertThat(result.getLimitSum()).isEqualByComparingTo("2000.00");
        verify(accountRepository).createIfNotExists(ACCOUNT);
    }

    @Test
    void activeLimitIsReturnedWhenExists() {
        OffsetDateTime datetime = OffsetDateTime.parse("2022-01-13T12:00:00Z");
        SpendingLimit existing = new SpendingLimit();
        when(limitRepository
                .findFirstByAccountNumberAndExpenseCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                        ACCOUNT, ExpenseCategory.PRODUCT, datetime))
                .thenReturn(Optional.of(existing));

        SpendingLimit result = limitService.getActiveLimit(ACCOUNT, ExpenseCategory.PRODUCT, datetime);

        assertThat(result).isSameAs(existing);
        verify(limitRepository, never()).save(any());
    }

    @Test
    void defaultLimitFromStartOfMonthIsCreatedWhenNoLimit() {
        OffsetDateTime datetime = OffsetDateTime.parse("2022-01-13T12:00:00Z");
        when(limitRepository
                .findFirstByAccountNumberAndExpenseCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                        ACCOUNT, ExpenseCategory.PRODUCT, datetime))
                .thenReturn(Optional.empty());
        when(limitRepository.save(any(SpendingLimit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpendingLimit result = limitService.getActiveLimit(ACCOUNT, ExpenseCategory.PRODUCT, datetime);

        assertThat(result.getLimitSum()).isEqualByComparingTo("1000.00");
        assertThat(result.getLimitDatetime()).isEqualTo(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
    }
}
