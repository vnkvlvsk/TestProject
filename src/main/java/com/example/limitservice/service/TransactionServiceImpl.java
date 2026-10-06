package com.example.limitservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.limitservice.config.LimitProperties;
import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.dto.TransactionRequest;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;
import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.TransactionRepository;
import com.example.limitservice.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final LimitCheckService limitCheckService;
    private final LimitProperties limitProperties;

    @Override
    public Transaction acceptTransaction(TransactionRequest request) {
        accountRepository.createIfNotExists(request.accountFrom());

        Transaction transaction = new Transaction();
        transaction.setAccountFrom(request.accountFrom());
        transaction.setAccountTo(request.accountTo());
        transaction.setCurrencyShortname(request.currencyShortname());
        transaction.setSum(request.sum());
        transaction.setExpenseCategory(request.expenseCategory());
        transaction.setDatetime(request.datetime());
        transaction.setStatus(TransactionStatus.PENDING);

        Optional<BigDecimal> rate = findRate(transaction);
        if (rate.isEmpty()) {
            log.warn("No exchange rate for {}, transaction of account {} is saved as PENDING",
                    transaction.getCurrencyShortname(), transaction.getAccountFrom());
            return transactionRepository.save(transaction);
        }
        return limitCheckService.checkAndSave(transaction, rate.get());
    }

    @Override
    public List<ExceededTransactionResponse> getExceededTransactions(String accountNumber) {
        return transactionRepository.findExceeded(accountNumber);
    }

    @Override
    @Scheduled(fixedDelayString = "${app.limits.pending-retry-delay}")
    public void processPendingTransactions() {
        List<Transaction> pendingTransactions = transactionRepository.findByStatusOrderByIdAsc(TransactionStatus.PENDING);
        if (pendingTransactions.isEmpty()) {
            return;
        }
        log.info("Processing {} pending transactions", pendingTransactions.size());
        for (Transaction transaction : pendingTransactions) {
            findRate(transaction).ifPresent(rate -> limitCheckService.checkAndSave(transaction, rate));
        }
    }

    private Optional<BigDecimal> findRate(Transaction transaction) {
        LocalDate rateDate = DateUtils.toLocalDate(transaction.getDatetime(), limitProperties.zone());
        return exchangeRateService.getRateToUsd(transaction.getCurrencyShortname(), rateDate);
    }
}
