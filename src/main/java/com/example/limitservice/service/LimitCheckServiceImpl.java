package com.example.limitservice.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.limitservice.config.LimitProperties;
import com.example.limitservice.entity.SpendingLimit;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;
import com.example.limitservice.repository.AccountRepository;
import com.example.limitservice.repository.TransactionRepository;
import com.example.limitservice.util.DateUtils;
import com.example.limitservice.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitCheckServiceImpl implements LimitCheckService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LimitService limitService;
    private final LimitProperties limitProperties;

    @Override
    @Transactional
    public Transaction checkAndSave(Transaction transaction, BigDecimal rateToUsd) {
        accountRepository.findByIdForUpdate(transaction.getAccountFrom())
                .orElseThrow(() -> new IllegalStateException("Account " + transaction.getAccountFrom() + " not found"));

        BigDecimal sumUsd = MoneyUtils.toUsd(transaction.getSum(), rateToUsd);
        SpendingLimit limit = limitService.getActiveLimit(
                transaction.getAccountFrom(), transaction.getExpenseCategory(), transaction.getDatetime());

        OffsetDateTime monthStart = DateUtils.startOfMonth(transaction.getDatetime(), limitProperties.zone());
        OffsetDateTime nextMonthStart = DateUtils.startOfNextMonth(transaction.getDatetime(), limitProperties.zone());
        BigDecimal spentInMonth = transactionRepository.sumUsdForPeriod(
                transaction.getAccountFrom(), transaction.getExpenseCategory(), monthStart, nextMonthStart);

        boolean limitExceeded = spentInMonth.add(sumUsd).compareTo(limit.getLimitSum()) > 0;

        transaction.setSumUsd(sumUsd);
        transaction.setLimitExceeded(limitExceeded);
        transaction.setStatus(TransactionStatus.PROCESSED);

        log.debug("Transaction of account {}: {} USD, spent in month {} USD, limit {} USD, exceeded = {}",
                transaction.getAccountFrom(), sumUsd, spentInMonth, limit.getLimitSum(), limitExceeded);
        return transactionRepository.save(transaction);
    }
}
