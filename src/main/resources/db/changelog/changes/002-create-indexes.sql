--liquibase formatted sql

--changeset kavaleuskiivan:002-index-limits-lookup
CREATE INDEX idx_spending_limits_account_category_datetime
    ON spending_limits (account_number, expense_category, limit_datetime);

--changeset kavaleuskiivan:002-index-transactions-month-sum
CREATE INDEX idx_transactions_account_category_datetime
    ON transactions (account_from, expense_category, datetime);

--changeset kavaleuskiivan:002-index-transactions-status
CREATE INDEX idx_transactions_status ON transactions (status);
