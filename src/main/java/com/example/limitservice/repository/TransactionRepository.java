package com.example.limitservice.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.entity.ExpenseCategory;
import com.example.limitservice.entity.Transaction;
import com.example.limitservice.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT COALESCE(SUM(t.sumUsd), 0)
            FROM Transaction t
            WHERE t.accountFrom = :accountFrom
              AND t.expenseCategory = :category
              AND t.status = com.example.limitservice.entity.TransactionStatus.PROCESSED
              AND t.datetime >= :from
              AND t.datetime < :to
            """)
    BigDecimal sumUsdForPeriod(@Param("accountFrom") String accountFrom,
                               @Param("category") ExpenseCategory category,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to);

    @Query("""
            SELECT new com.example.limitservice.dto.ExceededTransactionResponse(
                t.accountFrom, t.accountTo, t.currencyShortname, t.sum, t.expenseCategory, t.datetime,
                l.limitSum, l.limitDatetime, l.limitCurrencyShortname)
            FROM Transaction t
            JOIN (SELECT t2.id AS transactionId, MAX(l2.limitDatetime) AS limitDatetime
                  FROM Transaction t2
                  JOIN SpendingLimit l2 ON l2.accountNumber = t2.accountFrom
                                       AND l2.expenseCategory = t2.expenseCategory
                                       AND l2.limitDatetime <= t2.datetime
                  WHERE t2.accountFrom = :accountFrom
                    AND t2.limitExceeded = true
                  GROUP BY t2.id) actual ON actual.transactionId = t.id
            JOIN SpendingLimit l ON l.accountNumber = t.accountFrom
                                AND l.expenseCategory = t.expenseCategory
                                AND l.limitDatetime = actual.limitDatetime
            ORDER BY t.datetime, t.id
            """)
    List<ExceededTransactionResponse> findExceeded(@Param("accountFrom") String accountFrom);

    List<Transaction> findByStatusOrderByIdAsc(TransactionStatus status);
}
