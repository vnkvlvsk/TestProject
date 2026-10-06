package com.example.limitservice.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.example.limitservice.config.TwelveDataProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TwelveDataClient {

    private static final int DAYS_TO_LOOK_BACK = 7;

    private final RestClient restClient;
    private final TwelveDataProperties properties;

    public TwelveDataClient(RestClient twelveDataRestClient, TwelveDataProperties properties) {
        this.restClient = twelveDataRestClient;
        this.properties = properties;
    }

    public Optional<ClosePrice> getLastClose(String currency, LocalDate date) {
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                TimeSeriesResponse response = requestTimeSeries(currency, date);
                Optional<ClosePrice> closePrice = findLastClose(response, date);
                if (closePrice.isPresent()) {
                    return closePrice;
                }
                log.warn("twelvedata returned no data for USD/{} on {}, attempt {}: {}",
                        currency, date, attempt, response == null ? null : response.message());
            } catch (RestClientException e) {
                log.warn("twelvedata request for USD/{} on {} failed, attempt {}: {}",
                        currency, date, attempt, e.getMessage());
            }
            if (attempt < properties.maxAttempts()) {
                sleepBeforeRetry();
            }
        }
        log.error("Could not get rate USD/{} on {} after {} attempts", currency, date, properties.maxAttempts());
        return Optional.empty();
    }

    private TimeSeriesResponse requestTimeSeries(String currency, LocalDate date) {
        log.debug("Requesting twelvedata rate USD/{} on {}", currency, date);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/time_series")
                        .queryParam("symbol", "USD/" + currency)
                        .queryParam("interval", "1day")
                        .queryParam("start_date", date.minusDays(DAYS_TO_LOOK_BACK))
                        .queryParam("end_date", date.plusDays(1))
                        .queryParam("apikey", properties.apiKey())
                        .build())
                .retrieve()
                .body(TimeSeriesResponse.class);
    }

    private Optional<ClosePrice> findLastClose(TimeSeriesResponse response, LocalDate date) {
        if (response == null || !"ok".equals(response.status()) || response.values() == null) {
            return Optional.empty();
        }
        return response.values().stream()
                .map(value -> new ClosePrice(LocalDate.parse(value.datetime().substring(0, 10)), value.close()))
                .filter(closePrice -> !closePrice.date().isAfter(date))
                .max(Comparator.comparing(ClosePrice::date));
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.retryDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record ClosePrice(LocalDate date, BigDecimal close) {
    }

    record TimeSeriesResponse(String status, String message, List<Value> values) {
    }

    record Value(String datetime, BigDecimal close) {
    }
}
