# trd-ovr-vue

Vue 3 + Vite rewrite of the old Groovy Order Statement Parser focused on deterministic transaction import and reporting parity.

## Phase 1 MVP implemented

- CSV parser modules in dedicated files:
  - `src/parsers/finzero.csv.parser.ts`
  - `src/parsers/traderepublic.csv.parser.ts`
  - `src/parsers/scalable.csv.parser.ts`
- Parse orchestration with deterministic validation, id generation after totals/rates, and overwrite-by-id handling.
- Canonical transaction/share model + FIFO/AVG sell realization.
- Dexie persistence tables:
  - `raw_documents`, `transactions`, `imports`, `settings`, `exchange_rates`
- Report views:
  - overview totals
  - annual matrix
  - monthly matrix
  - by-date calendar-week table
  - by-ISIN open/closed
  - dividends table
- Import diagnostics with explicit line-level parser errors.

## Deferred TODOs

- PDF/Text parsers (e.g. ING-DiBa).
- Legacy XML export format compatibility.
- Remaining broker parsers beyond FinZero CSV, Trade Republic CSV, and Scalable CSV.

## Development

```bash
npm install
npm run dev
```

## Test / check

```bash
npm run vitest:unit
npm run build
```
