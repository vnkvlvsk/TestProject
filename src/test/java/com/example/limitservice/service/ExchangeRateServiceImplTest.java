package com.example.limitservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.example.limitservice.client.TwelveDataClient;
import com.example.limitservice.entity.ExchangeRate;
import com.example.limitservice.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    private static final LocalDate SATURDAY = LocalDate.of(2022, 1, 8);

    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    void usdRateIsOneWithoutAnyRequests() {
        assertThat(exchangeRateService.getRateToUsd("USD", SATURDAY)).contains(BigDecimal.ONE);

        verifyNoInteractions(exchangeRateRepository, twelveDataClient);
    }

    @Test
    void rateFromDatabaseIsUsedWithoutApiCall() {
        ExchangeRate saved = new ExchangeRate();
        saved.setCloseRate(new BigDecimal("430.5"));
        when(exchangeRateRepository.findByCurrencyShortnameAndRateDate("KZT", SATURDAY)).thenReturn(Optional.of(saved));

        assertThat(exchangeRateService.getRateToUsd("KZT", SATURDAY)).contains(new BigDecimal("430.5"));

        verifyNoInteractions(twelveDataClient);
    }

    @Test
    void rateFromApiIsSavedWithDateOfLastClose() {
        LocalDate friday = SATURDAY.minusDays(1);
        when(exchangeRateRepository.findByCurrencyShortnameAndRateDate("KZT", SATURDAY)).thenReturn(Optional.empty());
        when(twelveDataClient.getLastClose("KZT", SATURDAY))
                .thenReturn(Optional.of(new TwelveDataClient.ClosePrice(friday, new BigDecimal("431.2"))));

        assertThat(exchangeRateService.getRateToUsd("KZT", SATURDAY)).contains(new BigDecimal("431.2"));

        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(exchangeRateRepository).save(captor.capture());
        assertThat(captor.getValue().getRateDate()).isEqualTo(SATURDAY);
        assertThat(captor.getValue().getCloseDate()).isEqualTo(friday);
    }

    @Test
    void emptyWhenApiIsNotAvailable() {
        when(exchangeRateRepository.findByCurrencyShortnameAndRateDate("RUB", SATURDAY)).thenReturn(Optional.empty());
        when(twelveDataClient.getLastClose("RUB", SATURDAY)).thenReturn(Optional.empty());

        assertThat(exchangeRateService.getRateToUsd("RUB", SATURDAY)).isEmpty();

        verify(exchangeRateRepository, never()).save(any());
    }
}
