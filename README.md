# Transaction Processing Service

## Overview & Architecture
A Spring Boot RESTful microservice built to handle customer financial transactions backed by an embedded H2 database.

### Key Decisions & Assumptions
- All monetary amounts use Java's `BigDecimal` to prevent floating-point calculation inaccuracies.
- Every newly created transaction initializes with a `PENDING` status.
- Once a transaction reaches a terminal status (`COMPLETED` or `FAILED`), further status modifications are rejected to maintain transaction integrity.
- Global exception handling intercepts validation and domain exceptions, returning structured JSON error responses with standard HTTP status codes.

---

## API Endpoints

1. **Create Transaction**
   - `POST /api/transactions`
   - Accepts JSON (`transactionId`, `customerId`, `amount`, `currency`, `transactionType`).
   - Returns `201 Created` with the saved transaction or `400 Bad Request` / `409 Conflict`.

2. **Get Transaction by ID**
   - `GET /api/transactions/{transactionId}`
   - Returns `200 OK` with the record or `404 Not Found`.

3. **Get Transactions by Customer**
   - `GET /api/transactions?customerId={customerId}`
   - Returns `200 OK` with a JSON list of all transactions matching the customer ID.

4. **Update Transaction Status**
   - `PATCH /api/transactions/{transactionId}/status`
   - Accepts `{"status": "COMPLETED" | "FAILED"}`.
   - Returns `200 OK` or `400 Bad Request` if attempting to alter a completed/failed transaction.

---

## Validation Rules
- **Transaction ID**: Mandatory, non-blank string. Unique across the system (duplicate attempts trigger `409 Conflict`).
- **Customer ID**: Mandatory, non-blank string.
- **Amount**: Positive decimal strictly greater than zero (`@DecimalMin("0.01")`).
- **Currency**: Mandatory, non-blank string code (e.g., USD, EUR, INR, GBP).
- **Transaction Type**: Validated enum values (`DEPOSIT`, `WITHDRAWAL`, `TRANSFER`).
- **Transaction Status**: Validated enum values (`PENDING`, `COMPLETED`, `FAILED`).

---

## Testing Strategy
Automated unit and integration testing implemented via JUnit 5 and Spring `MockMvc` in `TransactionControllerTest.java`:
- Successful creation and persistence of transactions.
- Rejection of duplicate Transaction IDs (409 Conflict).
- Input validation rejection on negative/zero amounts and missing fields (400 Bad Request).
- Rejection on non-existent transaction lookups (404 Not Found).
- Filtering transactions by Customer ID.
- Status transition verification and terminal state enforcement.

---

## Known Limitations & Improvements
- **Idempotency**: Introduce unique idempotency headers for payment retry safety in distributed networks.
- **Pagination**: Implement Spring Data `Pageable` parameters on customer query endpoints for large transaction sets.
- **Audit Logging**: Add an event log table recording status transition history and audit timestamps.

---

## AI Usage Disclosure
- **Tool Used**: Gemini AI assistant.
- **Assistance Scope**: Architectural design guidance, Spring Data JPA repository structure, global exception handling, and drafting JUnit 5 MockMvc test suites.
- **Modifications & Validation**: Refined state transition rules to block modifications on terminal states, verified `BigDecimal` mapping, and validated all logic via Maven CLI builds and live API tests.