package com.example.limitservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ExpenseCategory {
    @JsonProperty("product")
    PRODUCT,
    @JsonProperty("service")
    SERVICE
}
