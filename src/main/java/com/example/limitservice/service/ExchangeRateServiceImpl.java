package com.example.limitservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.example.limitservice.client.TwelveDataClient;
import com.example.limitservice.entity.ExchangeRate;
import com.example.limitservice.repository.ExchangeRateRepository;
import com.example.limitservice.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final TwelveDataClient twelveDataClient;

    @Override
    public Optional<BigDecimal> getRateToUsd(String currency, LocalDate date) {
        if (MoneyUtils.USD.equals(currency)) {
            return Optional.of(BigDecimal.ONE);
        }

        Optional<ExchangeRate> savedRate = exchangeRateRepository.findByCurrencyShortnameAndRateDate(currency, date);
        if (savedRate.isPresent()) {
            return Optional.of(savedRate.get().getCloseRate());
        }

        Optional<TwelveDataClient.ClosePrice> closePrice = twelveDataClient.getLastClose(currency, date);
        if (closePrice.isEmpty()) {
            return Optional.empty();
        }
        saveRate(currency, date, closePrice.get());
        return Optional.of(closePrice.get().close());
    }

    private void saveRate(String currency, LocalDate date, TwelveDataClient.ClosePrice closePrice) {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrencyShortname(currency);
        rate.setRateDate(date);
        rate.setCloseRate(closePrice.close());
        rate.setCloseDate(closePrice.date());
        try {
            exchangeRateRepository.save(rate);
            log.info("Saved rate USD/{} for {} (close of {}): {}", currency, date, closePrice.date(), closePrice.close());
        } catch (DataIntegrityViolationException e) {
            log.debug("Rate USD/{} for {} is already saved", currency, date);
        }
    }
}
