# Order Statement Parser → Node.js + Vue 3 Rewrite (Functional Spec v2)

## 1. Purpose and parity target

This spec defines the minimum behavior required to rewrite the current Groovy application into a Vue 3 hosted web app (with browser-side persistence via LocalStorage or IndexedDB/Dexie), while preserving report and calculation parity.

**Parity target:** Given equivalent input statements, the rewritten app must produce the same normalized transactions, share-level aggregates, and report totals as the current implementation.

---

## 2. End-to-end pipeline (authoritative)

1. Import broker statements (CSV/TSV/PDF text).
2. Parse each input into canonical `Transaction` objects using broker-specific parser modules.
3. Validate each transaction with strict parser-independent invariants.
4. Apply overwrite rules and deduplication/merge rules.
5. Group transactions by ISIN and compute share-level positions and P/L (FIFO + AVG).
6. Build report view-models (overview, annual matrix, monthly table, by-date CW table, by-ISIN open/closed, dividends).
7. Persist normalized data + metadata locally.
8. Render interactive UI; support export/import snapshots and optional XML/HTML compatibility export.

---

## 3. Canonical domain model

## 3.1 Transaction (required fields)

```ts
interface Transaction {
  id: string
  parserType: ParserType
  type: TransType
  date: string // ISO timestamp in UTC, e.g. 2024-05-10T00:00:00Z

  isin: string // may be synthetic for crypto/no-ISIN sources
  wkn?: string
  name?: string

  currency: CurrencyCode

  amount: number
  openAmount: number
  rate: number
  total: number
  tax: number
  fee: number
  interest: number

  winlossFifo: number
  winlossAvg: number

  // metadata flags
  crypto: boolean
  spinOff: boolean
  temporaryStatement: boolean
  overridden: boolean

  // source metadata
  sourceFileName?: string
  sourceRelativePath?: string
  sourceLastModified?: number
  sourceSize?: number

  // parser-specific compatibility fields
  smartBrokerStatus?: 'EXECUTED' | 'CLEARED'
  deGiroSaldo?: number
  hasBuyTransAgeUnder365Days?: boolean
}
```

## 3.2 Share aggregate

```ts
interface Share {
  isin: string
  name: string
  transactions: Transaction[]

  amount: number
  dividend: number
  invested: number
  winloss: number
  tax: number
  fee: number
  interest: number

  openAmount: number
  openTotal: number
  rate: number
  isClosed: boolean
}
```

## 3.3 Enumerations

- `TransType`: `BUY | SELL | DIV | KNO | TAX | CAPITAL | SPLIT | SPLITBUY | SPLITSELL | PREEMPTIVE | INTEREST | FEE | ISINSWITCH | ISINSWITCHIN | ISINSWITCHOUT | UNSUPPORTED | UNKNOWN`
- `ParserType`: `INGDIBA | TRADEREPUBLIC | SMARTBROKER | DEGIRO | QUIRION | BISON | VIVID | BITPANDA | FINNETZERO_PDF | FINNETZERO_CSV | SCALABLE | SCALABLE2`

---

## 4. Validation contract (must be deterministic)

Validation runs after parser mapping and before aggregation.

1. `date` must be present and parseable.
2. `type` cannot be `UNKNOWN`.
3. For non-special transactions (`!KNO,!TAX,!DIV,!CAPITAL,!spinOff,!overridden`):
   - `name` required
   - `amount > 0`
4. ISIN rule:
   - required and length `12` unless one of: `TAX`, `INTEREST`, `FEE`, `crypto=true`.
5. `rate` must be finite; for BUY/SELL (non-spinOff), `rate > 0`.
6. `total` required; for non-special types (`!KNO,!DIV,!PREEMPTIVE,!CAPITAL,!spinOff`) it cannot be `0`.
7. Reconciliation rule:
   - `calculatedTotal = amount * rate * sign + tax + interest + fee`
   - where sign is negative for BUY/TAX (except temporary statement handling)
   - tolerance per-unit diff `< 0.01`, with crypto precision exception.

Any validation error rejects that transaction and is logged with source context.

---

## 5. Parsing orchestration rules

These sequencing rules are mandatory:

1. Parse `type` first.
2. Skip if `UNSUPPORTED` or non-executed status line.
3. Parse in order: `date → isin → currency → name → amount → tax`.
4. Then:
   - **Scalable CSV variant:** `rate → fee → total`
   - **Other parsers:** `total → rate → fee`
5. Parse/generate `id` **after** totals/rates are available.
6. Apply overwrite map by `id` (mark `overridden=true`).
7. Run validation.

---

## 6. Deduplication, status upgrades, and temp statements

## 6.1 Intra-import duplicate logic

- Duplicate key candidates include at least: `isin + id + amount + tax` (plus parser-specific checks).
- SmartBroker rule: keep newer transaction when status transitions to `CLEARED`.
- DeGiro rule: duplicate replacement can depend on `deGiroSaldo` equality.
- Additional hash-based duplicate detection should run by parser, with exception/skip handling for known brokers and known PDF “Stornokopie” cases.

## 6.2 Temporary statement handling

- Process non-temporary transactions first.
- For each temporary transaction, if a non-temporary transaction exists with matching parser and `id.startsWith(temp.id)`, temporary one is dropped.
- Unmatched temporary transactions remain included.

---

## 7. Share aggregation and win/loss algorithm

## 7.1 Grouping and accounting

- Group transactions by `isin`.
- Aggregate totals:
  - `share.fee += transaction.fee`
  - `share.tax += transaction.tax`
  - BUY/PREEMPTIVE/INTEREST contribute to invested and amount.
  - SELL/KNO decrease amount (except tax/switch).
- DIV/TAX/INTEREST set `transaction.winlossFifo = transaction.total`; add to share `winloss`; DIV also adds to `dividend`.

## 7.2 FIFO + AVG on SELL/KNO

For each SELL/KNO (by ascending date):
1. Select pre-dated BUY/CAPITAL/PREEMPTIVE transactions from same parser with open amount.
2. Reject with error when no sufficient pre-dated open amount exists.
3. Consume open amounts in FIFO order:
   - compute realized FIFO win/loss per consumed chunk,
   - compute AVG-based win/loss using average pre-dated buy rate,
   - decrement buy open amounts and sell remaining amount.
4. Mark `hasBuyTransAgeUnder365Days` when matched buy is within 365 days before sell.

## 7.3 Crypto/Bison tax adjustments

- Apply crypto-specific tax factor adjustments in realized sell path.
- For Bison, if total under-365-day crypto sell win/loss is positive, apply aggregate tax adjustment to share tax.

---

## 8. Currency and exchange-rate behavior

- Default currency is EUR.
- Currency extraction uses parser-specific patterns.
- Non-EUR transactions requiring conversion (e.g., specific DeGiro/SmartBroker dividend flows) must use a deterministic exchange-rate adapter with local cache.
- Store rate source/date used for conversion in metadata where possible.

---

## 9. Persistence model (Dexie recommended)

## 9.1 Storage choice

- Prefer IndexedDB with Dexie for volume and queryability.
- Optional fallback to LocalStorage for tiny datasets/export snapshots only.

## 9.2 Suggested schema

### Table `raw_documents`
- `id` (PK)
- `sourceRelativePath`
- `sourceSize`
- `sourceLastModified`
- `md5`
- `parserType`
- `importedAt`
- `contentType` (`csv|tsv|pdf-text`)

### Table `transactions`
- `id` (PK composite recommendation: `parserType + ':' + id + ':' + isin + ':' + date`)
- all canonical transaction fields
- `dataVersion` (for migration)

### Table `imports`
- import run metadata, warnings, parser errors

### Table `settings`
- enabled parsers
- selected report years
- UI preferences

## 9.3 Data versioning

- Maintain explicit `dataVersion` similar to prior store versioning.
- On version bump, run deterministic migration or reparse prompt.

---

## 10. UI/report requirements (Vue 3)

Must include parity with current report behavior:

1. Top overview totals: open/invested, win/loss, tax, fee, interest.
2. Annual matrix:
   - columns by selected years,
   - rows by parser,
   - annual totals, dividend contribution, trade count, fees+taxes row.
3. Monthly matrix (`year x Jan..Dec`) with win/loss + dividends in each cell.
4. By-date trade table:
   - grouped by calendar week,
   - weekly subtotal,
   - filters by parser and transaction type,
   - columns: date, name, isin, amount, rate, total, fifo, avg, %, tax, fee, interest.
5. By-ISIN open table with expandable transaction detail rows.
6. Dividends table sorted by annual contribution.
7. By-ISIN closed table with expandable detail rows.
8. Interactive helpers:
   - show/hide sections,
   - row marking for ad-hoc sums,
   - running selected-row sum.

---

## 11. Configuration that must be externalized

No hardcoded values in code for:

- enabled parser list
- parser folder/source mappings (web import sources)
- overwrite maps
- supported years in report matrix
- parser color mapping and type color mapping

Store these as JSON config (or managed settings UI) so behavior can evolve without code edits.

---

## 12. Parser module contract

Each parser module must implement:

```ts
interface ParserModule {
  parserType: ParserType
  supports(input: ImportArtifact): boolean
  parse(input: ImportArtifact): Transaction[]
  overwrites?: Record<string, (tx: Transaction) => Transaction>
}
```

And should keep regex/column maps declarative where feasible.

---

## 13. Error handling and auditability

- Non-fatal parser errors should be collected and shown in import report.
- Fatal consistency errors (e.g., impossible FIFO sell amount) must fail import with explicit diagnostics.
- Every transaction should retain provenance: parser, source file/name, import timestamp.

---

## 14. Compatibility exports

- JSON snapshot export/import for full local backup.
- CSV export for recent/all transactions.
- Optional XML export in legacy shape (for backward compatibility checks).
- Optional HTML export of rendered report.

---

## 15. Test plan (minimum)

1. **Parser fixtures:** one golden fixture set per broker and statement variant.
2. **Validation tests:** invariant and tolerance edge cases.
3. **Aggregation tests:** FIFO/AVG, partial sell chains, pre-dated failures.
4. **Dedup tests:** parser-specific duplicate and status-upgrade behavior.
5. **Temporary statement tests:** replacement and unmatched temp inclusion.
6. **Currency conversion tests:** deterministic conversion with cached rates.
7. **UI parity tests:** table totals and filter/grouping correctness.
8. **Persistence migration tests:** `dataVersion` upgrades.

---

## 16. Acceptance criteria for “rewrite complete”

Rewrite is complete only when all are true:

1. On fixture corpus, canonical transactions match expected snapshots.
2. Share-level totals (`openTotal`, `winloss`, `tax`, `fee`, `interest`, `dividend`) match baseline.
3. By-date weekly totals and annual/monthly matrix totals match baseline.
4. Duplicate/temp-statement and overwrite behavior matches baseline.
5. Import/export roundtrip preserves all canonical fields and metadata.
6. App runs fully offline in browser using local persistence.

