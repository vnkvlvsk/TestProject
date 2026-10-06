---
name: limit-logic-test
description: Use when writing, changing or fixing a test for the limit_exceeded logic of limit-service - monthly USD limits per account and expense category, limit changed inside a month, month border / time zone, remainder exactly 0, default 1000 USD limit, parallel transactions of one account, exchange rate conversion, PENDING transactions when the twelvedata API fails, or the exceeded-transactions report. Tells which test class to use, which project test utilities to call and how to run the tests.
---

# Writing a test for the limit logic

## Where the logic lives

- `src/main/java/com/example/limitservice/service/LimitCheckServiceImpl.java` - `checkAndSave`: locks the
  account row, converts the sum to USD, sums the month, sets `limit_exceeded`
  (`spentInMonth + sumUsd > limitSum`, remainder exactly 0 is NOT exceeded).
- `src/main/java/com/example/limitservice/service/LimitServiceImpl.java` - `getActiveLimit`: the latest limit
  with `limitDatetime <= transaction datetime`, otherwise a default 1000 USD limit from the start of the month.
- `src/main/java/com/example/limitservice/util/DateUtils.java` - month borders in `app.limits.zone` (UTC).
- `src/main/java/com/example/limitservice/repository/TransactionRepository.java` - `sumUsdForPeriod` and the
  report query `findExceeded`.

## 1. Choose the test class

| What is checked | Test class (`src/test/java/com/example/limitservice/...`) |
|---|---|
| Arithmetic of one transaction: remainder, rounding, which period is summed | `service/LimitCheckServiceImplTest` - Mockito only, no Spring, no Docker |
| Which limit is active, default limit, limit date | `service/LimitServiceImplTest` - Mockito + `Clock.fixed(...)` |
| Month borders, rounding helpers | `util/DateUtilsTest`, `util/MoneyUtilsTest` |
| Scenario with several limits/transactions on the real DB, concurrency, PENDING, report | `integration/LimitFlagIntegrationTest` |
| Scenario only through HTTP | a new class in `integration/` modelled on `CaseOneApiTest` |

Add a new `@Test` method to an existing class; create a new class only for a new HTTP scenario.

## 2. Integration tests: use the project test utilities

Integration test classes extend `support/IntegrationTestBase`. It already provides:

- the whole app on a random port + PostgreSQL in Testcontainers (`support/TestConfig`, `@ServiceConnection`);
- `cleanUp()` before every test: all tables are emptied, WireMock is reset - do not clean manually;
- `clock` (`support/MutableClock`): limits take their date from the `Clock` bean, so call
  `clock.setTime(OffsetDateTime.parse("2022-01-10T09:00:00Z"))` BEFORE creating a limit;
- WireMock instead of twelvedata, default rate `USD/KZT = 500.00` (250000 KZT = 500 USD);
  `stubRate("RUB", "90.00")` for another currency, `stubRateError("RUB")` to simulate an API failure,
  `WIRE_MOCK.getAllServeEvents()` to count API calls;
- repositories: `transactionRepository`, `limitRepository`, `exchangeRateRepository`, `accountRepository`.

Inside `LimitFlagIntegrationTest` reuse its private helpers, do not write new ones:

- `setLimitAt("2022-01-10T00:00:00Z", "2000.00")` - moves the clock and creates a PRODUCT limit for `ACCOUNT`;
- `send("100.00", "2022-01-11T12:00:00Z")` - USD transaction (rate 1, no API call), returns the saved `Transaction`;
- `sendInCurrency("KZT", "250000.00", "2022-01-05T12:00:00Z")` - transaction in another currency.

For services use the real beans: `transactionService.acceptTransaction(...)`,
`transactionService.getExceededTransactions(ACCOUNT)`, `transactionService.processPendingTransactions()`,
`limitService.getLimits(ACCOUNT)`.

## 3. Conventions

- Amounts in USD via `send(...)` unless the test is about exchange rates.
- Month borders are UTC. To test the border use an explicit offset:
  `2022-02-01T03:00:00+06:00` is still January in UTC.
- Remainder exactly 0 -> `isFalse()`; the next `0.01` -> `isTrue()`.
- AssertJ only: money `isEqualByComparingTo("500.00")`, dates `isAtSameInstantAs(OffsetDateTime.parse(...))`
  (the DB returns UTC), flags `isTrue()` / `isFalse()`.
- Method name describes the expected behaviour: `newMonthStartsWithFullLimit`,
  `remainderExactlyZeroIsNotExceeded`. Given/when/then are separated by blank lines.
- No comments in test code (project convention).

Example for `LimitFlagIntegrationTest`:

```java
@Test
void limitsOfDifferentAccountsAreSeparate() {
    setLimitAt("2022-01-01T00:00:00Z", "1000.00");
    assertThat(send("1000.00", "2022-01-05T12:00:00Z").getLimitExceeded()).isFalse();

    Transaction otherAccount = transactionService.acceptTransaction(new TransactionRequest("0000000456",
            COUNTERPARTY, "USD", new BigDecimal("1000.00"), ExpenseCategory.PRODUCT,
            OffsetDateTime.parse("2022-01-05T13:00:00Z")));

    assertThat(otherAccount.getLimitExceeded()).isFalse();
}
```

Unit test pattern (`LimitCheckServiceImplTest`): stub with the existing `givenLimitAndSpent("2000.00", "1900.00")`
and build input with `transaction("100.00")`, then call `limitCheckService.checkAndSave(transaction, BigDecimal.ONE)`.

## 4. Run and verify

Docker must be running for integration tests.

```bash
./mvnw test -Dtest=LimitFlagIntegrationTest                 # one class
./mvnw test -Dtest='LimitFlagIntegrationTest#newMonth*'     # one method
./mvnw verify                                               # all tests, must be green at the end
```

- A new test must fail if the checked rule is broken: temporarily break the rule
  (e.g. change `> 0` to `>= 0` in `LimitCheckServiceImpl`) and make sure the test goes red, then revert.
- A concurrency test must fail when `@Lock(LockModeType.PESSIMISTIC_WRITE)` is removed from
  `AccountRepository.findByIdForUpdate`.
- If the limit logic itself was changed, `integration/CaseOneApiTest` (case 1 of the task) must stay green.
