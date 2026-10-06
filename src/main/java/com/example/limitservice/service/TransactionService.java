package com.example.limitservice.service;

import java.util.List;

import com.example.limitservice.dto.ExceededTransactionResponse;
import com.example.limitservice.dto.TransactionRequest;
import com.example.limitservice.entity.Transaction;

public interface TransactionService {

    Transaction acceptTransaction(TransactionRequest request);

    List<ExceededTransactionResponse> getExceededTransactions(String accountNumber);

    void processPendingTransactions();
}
