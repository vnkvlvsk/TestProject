--liquibase formatted sql

--changeset kavaleuskiivan:001-create-accounts
CREATE TABLE accounts
(
    account_number VARCHAR(10) PRIMARY KEY
);

--changeset kavaleuskiivan:001-create-spending-limits
CREATE TABLE spending_limits
(
    id                       BIGSERIAL PRIMARY KEY,
    account_number           VARCHAR(10)    NOT NULL REFERENCES accounts (account_number),
    expense_category         VARCHAR(20)    NOT NULL,
    limit_sum                NUMERIC(19, 2) NOT NULL,
    limit_currency_shortname VARCHAR(3)     NOT NULL DEFAULT 'USD',
    limit_datetime           TIMESTAMPTZ    NOT NULL
);

--changeset kavaleuskiivan:001-create-exchange-rates
CREATE TABLE exchange_rates
(
    id                 BIGSERIAL PRIMARY KEY,
    currency_shortname VARCHAR(3)     NOT NULL,
    rate_date          DATE           NOT NULL,
    close_rate         NUMERIC(19, 10) NOT NULL,
    close_date         DATE           NOT NULL,
    CONSTRAINT uq_exchange_rates_currency_date UNIQUE (currency_shortname, rate_date)
);

--changeset kavaleuskiivan:001-create-transactions
CREATE TABLE transactions
(
    id                 BIGSERIAL PRIMARY KEY,
    account_from       VARCHAR(10)    NOT NULL REFERENCES accounts (account_number),
    account_to         VARCHAR(10)    NOT NULL,
    currency_shortname VARCHAR(3)     NOT NULL,
    sum                NUMERIC(19, 2) NOT NULL,
    expense_category   VARCHAR(20)    NOT NULL,
    datetime           TIMESTAMPTZ    NOT NULL,
    sum_usd            NUMERIC(19, 2),
    limit_exceeded     BOOLEAN,
    status             VARCHAR(20)    NOT NULL
);
