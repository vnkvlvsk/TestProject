# limit-service

Прототип микросервиса для банка: принимает расходные операции клиентов в разных валютах, переводит их в USD
по биржевому курсу и помечает операции, превысившие месячный лимит (`limit_exceeded`).

## Возможности

- приём транзакций от банковских систем (`POST /api/transactions`);
- месячный лимит в USD отдельно для категорий `product` и `service`, по умолчанию 1000 USD;
- установка нового лимита клиентом (дата ставится сервисом автоматически, изменять лимиты нельзя);
- список транзакций, превысивших лимит, вместе с превышенным лимитом;
- курсы USD/KZT, USD/RUB и др. с twelvedata.com, сохраняются в своей БД и берутся оттуда повторно;
- корректная работа при параллельных транзакциях одного клиента;
- если внешний API курсов не отвечает, транзакция не теряется (см. «Сбои внешнего API»).

## Технологии

Java 21, Spring Boot 3.5 (Web MVC на виртуальных потоках, Data JPA, Validation, Actuator), PostgreSQL,
Liquibase, RestClient, springdoc-openapi (Swagger UI), Lombok.
Тесты: JUnit 5, Mockito, AssertJ, Testcontainers, WireMock.

## Архитектура

Классическое разделение Controller → Service → Repository. Два контроллера: `TransactionController`
(API для банковских систем) и `ClientController` (API для клиента). Каждый сервис — интерфейс + `*Impl`.
`TransactionServiceImpl` получает курс через `ExchangeRateServiceImpl` (сначала БД, потом `TwelveDataClient`),
а сама проверка лимита идёт в `LimitCheckServiceImpl` в одной транзакции БД с блокировкой счёта.
Вспомогательные вычисления вынесены в `util/DateUtils` (границы месяца) и `util/MoneyUtils` (перевод в USD).
Текущее время берётся только из бина `java.time.Clock`, поэтому в тестах его можно «переводить».

```
src/main/java/com/example/limitservice
├── controller   TransactionController, ClientController
├── service      TransactionService, LimitService, LimitCheckService, ExchangeRateService (+ Impl)
├── repository   Spring Data JPA репозитории
├── client       TwelveDataClient (RestClient, таймауты, повторы)
├── entity       Transaction, SpendingLimit, ExchangeRate, Account, enums
├── dto          records запросов/ответов
├── config       @ConfigurationProperties, Clock, RestClient
├── exception    GlobalExceptionHandler (ProblemDetail)
└── util         DateUtils, MoneyUtils
```

### Модель данных

| Таблица | Назначение |
|---|---|
| `accounts` | счёт клиента; строка блокируется (`SELECT ... FOR UPDATE`) при проверке лимита |
| `spending_limits` | все когда-либо установленные лимиты (только вставка, без обновления) |
| `transactions` | транзакции + `sum_usd`, `limit_exceeded`, `status` (`PENDING` / `PROCESSED`) |
| `exchange_rates` | курс закрытия USD/валюта на день; `close_date` ≠ `rate_date` в выходные |

Схема создаётся миграциями Liquibase (`src/main/resources/db/changelog`), Hibernate только проверяет её
(`ddl-auto=validate`).

## Логика лимитов

- **Часовой пояс**: границы месяца считаются в **UTC** (настройка `app.limits.zone`). Например,
  транзакция `2022-02-01T03:00:00+06:00` — это 31 января по UTC, значит она относится к январю.
- **Какой лимит действует**: последний лимит, установленный не позже момента транзакции. Поэтому новый
  лимит не влияет на транзакции, совершённые раньше него.
- **Расчёт флага**: `потрачено в этом месяце (USD) + сумма транзакции (USD) > лимит` → `limit_exceeded = true`.
  Остаток ровно 0 — это ещё не превышение. При смене лимита внутри месяца уже потраченное в этом месяце
  учитывается (как в таблице из задания).
- **Лимит по умолчанию**: если клиент лимит не устанавливал, при первой транзакции создаётся лимит 1000 USD
  с датой начала месяца этой транзакции.
- **Параллельные запросы**: проверка выполняется в транзакции БД, первым делом блокируется строка счёта в
  `accounts` (`@Lock(PESSIMISTIC_WRITE)`). Второй запрос по тому же счёту ждёт, пока первый закоммитит, и
  видит уже сохранённую сумму. Разные счета друг друга не блокируют.
- **Отчёт** `GET /api/client/transactions/exceeded` — один JPQL-запрос `TransactionRepository.findExceeded`:
  JOIN с подзапросом, который через `MAX(limit_datetime) ... GROUP BY` находит лимит, действовавший на момент
  каждой транзакции.

## Курсы валют

Используется twelvedata.com, метод `time_series`, пара `USD/{валюта}`, интервал `1day`, значение `close`.
Курс берётся на день транзакции (в UTC). Если в этот день торгов не было (выходной/праздник), берётся
последнее закрытие перед ним (аналог `previous_close`): сервис запрашивает неделю данных и выбирает
последний день не позже нужного. Полученный курс сохраняется в `exchange_rates`, и следующие транзакции
этого дня внешний API уже не вызывают. Для USD курс = 1.

### Сбои внешнего API

- таймауты: подключение 2 с, чтение 5 с (`app.twelvedata.connect-timeout`, `read-timeout`);
- до 3 попыток с паузой 500 мс (`app.twelvedata.max-attempts`, `retry-delay`);
- внешний запрос выполняется **до** начала транзакции БД, чтобы медленный API не держал блокировку;
- если курс так и не получен, транзакция всё равно **сохраняется** со статусом `PENDING`
  (`sum_usd` и `limit_exceeded` пустые), а API отвечает `201`. Раз в минуту (`app.limits.pending-retry-delay`)
  планировщик `TransactionServiceImpl.processPendingTransactions` пробует обработать такие транзакции снова.

Ограничение прототипа: пока транзакция в `PENDING`, её сумма не учитывается в остатке лимита для
транзакций, пришедших после неё.

## Запуск

Нужны Java 21 и Docker.

1. Получить бесплатный API-ключ на https://twelvedata.com (ключ `demo` не отдаёт USD/KZT).
2. Поднять PostgreSQL:
   ```bash
   docker compose up -d
   ```
   База доступна на порту **5433** (не 5432, чтобы не конфликтовать с локально установленным PostgreSQL).
   Порт можно сменить: `POSTGRES_PORT=5434 docker compose up -d` и `DB_URL=jdbc:postgresql://localhost:5434/limit_service`.
3. Запустить сервис:
   ```bash
   TWELVEDATA_API_KEY=<ключ> ./mvnw spring-boot:run
   ```

Переменные окружения: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `TWELVEDATA_API_KEY`, `SERVER_PORT`.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health-check: http://localhost:8080/actuator/health

## API

| Метод | URL | Описание |
|---|---|---|
| POST | `/api/transactions` | принять транзакцию (интеграция с банком) |
| POST | `/api/client/limits` | установить новый лимит |
| GET | `/api/client/limits?account=` | все лимиты клиента |
| GET | `/api/client/transactions/exceeded?account=` | транзакции, превысившие лимит |

```bash
curl -X POST localhost:8080/api/client/limits -H 'Content-Type: application/json' \
  -d '{"account":"0000000123","expense_category":"product","limit_sum":2000.00}'

curl -X POST localhost:8080/api/transactions -H 'Content-Type: application/json' \
  -d '{"account_from":"0000000123","account_to":"9999999999","currency_shortname":"KZT",
       "sum":10000.45,"expense_category":"product","datetime":"2022-01-30T00:00:00+06:00"}'

curl 'localhost:8080/api/client/transactions/exceeded?account=0000000123'
```

Ответ отчёта:

```json
[{
  "account_from": "0000000123", "account_to": "9999999999", "currency_shortname": "KZT",
  "sum": 300000.00, "expense_category": "product", "datetime": "2022-01-03T06:00:00Z",
  "limit_sum": 1000.00, "limit_datetime": "2022-01-01T03:00:00Z", "limit_currency_shortname": "USD"
}]
```

Даты в ответах возвращаются в UTC (тот же момент времени, что пришёл в запросе).
Ошибки возвращаются в формате ProblemDetail (RFC 9457), для ошибок валидации есть поле `errors`:

```json
{"type":"about:blank","title":"Validation error","status":400,"detail":"Request validation failed",
 "instance":"/api/transactions","errors":{"accountFrom":"must be 10 digits"}}
```

## Тесты

```bash
./mvnw verify
```

Нужен только запущенный Docker: PostgreSQL поднимается в Testcontainers, API курсов замокан WireMock.

- `CaseOneApiTest` — сквозной тест «1 случая» из задания через HTTP (RANDOM_PORT): устанавливает лимиты,
  отправляет 6 транзакций и проверяет, что в отчёте ровно транзакции от 3 и 13 января с нужными лимитами;
- `LimitFlagIntegrationTest` — «2 случай», смена лимита внутри месяца, переход на новый месяц, остаток ровно 0,
  граница месяца для другого часового пояса, лимит по умолчанию, раздельные категории, 20 параллельных
  транзакций, сохранение курса в БД, `PENDING` при падении API;
- `ValidationApiTest` — ошибки валидации в формате ProblemDetail;
- unit-тесты: `LimitCheckServiceImplTest`, `LimitServiceImplTest`, `ExchangeRateServiceImplTest`,
  `DateUtilsTest`, `MoneyUtilsTest`.

## AI в проекте

### Инструмент

Claude Code (desktop-приложение). TODO: опишите своими словами, для чего использовали (например: каркас
проекта, тесты, разбор ошибок, проверка требований задания).

### MCP-серверы

Подключены в `.mcp.json` (конфигурация Claude Code). Секретов в файле нет, адрес БД берётся из
переменной окружения `MCP_POSTGRES_URL`.

- **`postgres`**: [Postgres MCP Pro](https://github.com/crystaldba/postgres-mcp), запускается в Docker в
  режиме `--access-mode=restricted` (только чтение), под отдельным пользователем `mcp_readonly` с правом
  только на `SELECT`. Пользователь создаётся скриптом `docker/init-readonly-user.sql` при первом старте
  docker-compose. Агент видит схему и данные локальной БД: какие флаги проставились, какой курс сохранился,
  какие миграции выполнены.
- **`context7`**: актуальная документация по Spring Boot, Hibernate, Liquibase, Testcontainers.

Перед запуском агента (база из `docker compose up -d`, порт 5433):

```bash
export MCP_POSTGRES_URL=postgresql://mcp_readonly:mcp_readonly@host.docker.internal:5433/limit_service
```

`host.docker.internal`, а не `localhost`: MCP-сервер работает внутри Docker-контейнера. При первом запуске
Claude Code спросит, доверять ли серверам из `.mcp.json`. Проверить подключение можно командой
`claude mcp list`: оба сервера должны быть в статусе `Connected`.

TODO: пример задачи, где MCP помог (например: «посмотри в БД, какие транзакции в статусе PENDING и
почему» или «покажи, какие changeset-ы записаны в `databasechangelog`»).

### Скилл

`.agents/skills/limit-logic-test/SKILL.md`: как написать тест на логику лимитов (`limit_exceeded`) по
конвенциям проекта. В нём описано, какой тестовый класс выбрать, какие утилиты использовать
(`IntegrationTestBase`, `MutableClock`, `stubRate`/`stubRateError`, помощники `setLimitAt`/`send`),
как проверить, что тест действительно ловит ошибку, и какими командами запускать.

Claude Code ищет скиллы в `.claude/skills`, поэтому там симлинк `.claude/skills → ../.agents/skills`.
Применение:

- попросить агента, например «добавь тест, что у разных счетов отдельные лимиты»: агент сам выберет
  скилл по `description`;
- или вызвать явно: `/limit-logic-test`.
