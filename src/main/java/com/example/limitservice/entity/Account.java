package com.example.limitservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @Column(name = "account_number")
    private String accountNumber;
}
