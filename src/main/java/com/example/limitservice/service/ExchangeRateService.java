package com.example.limitservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateService {

    Optional<BigDecimal> getRateToUsd(String currency, LocalDate date);
}
