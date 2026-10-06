package com.example.limitservice.controller;

import com.example.limitservice.dto.TransactionRequest;
import com.example.limitservice.dto.TransactionResponse;
import com.example.limitservice.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions (bank integration)")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse acceptTransaction(@Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(transactionService.acceptTransaction(request));
    }
}
