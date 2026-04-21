# Order Statement Parser → Vue 3 Rewrite: Functional & Logic Specification

This document captures the current Groovy app behavior so it can be migrated to a Vue 3 web app.

## 1) Current pipeline (Groovy)

1. Parse broker exports (CSV/TSV/PDF) into normalized `Transaction` objects per parser.
2. Validate parsed transactions (`date`, `type`, `isin`, `amount`, `rate`, `total`, etc.).
3. Aggregate transactions into `Share` objects with FIFO/AVG win/loss calculations.
4. Write summary XML (`summary.xml`).
5. Transform XML via `summary.xsl` into interactive HTML report.

## 2) Canonical transaction model needed for Vue 3

Your Vue app should keep one canonical transaction shape independent of source parser:

- `parserType`
- `type`: BUY, SELL, DIV, KNO, TAX, FEE, INTEREST, CAPITAL, PREEMPTIVE, SPLIT…
- `date`
- `id`
- `name`
- `isin` (or synthetic id when no ISIN exists)
- `currency`
- `amount`
- `rate`
- `total`
- `tax`
- `fee`
- `interest`
- `winlossFifo` / `winlossAvg`
- metadata flags: `crypto`, `spinOff`, `temporaryStatement`, `overridden`

## 3) Parser-specific logic (input format + matching rules)

---

### 3.1 Bison (`ParserBison`)

**Input format**
- CSV with `;` separator.
- Expected columns (7): `Typ;Datum/Zeit;Symbol;Rate;Anzahl;Betrag;Plattform`.

**Core logic**
- Type mapping by `Typ`: `Kauf`→BUY, `Verkauf`→SELL.
- No dividend/tax/fee parsing in current implementation.
- `name` and `isin` both use `Symbol` column.
- Currency fixed to EUR.
- `id` is the full source line.

---

### 3.2 Bitpanda (`ParserBitpanda`)

**Input format**
- CSV with `,` separator; expected 16 columns.
- Only rows where category contains `Stock` are parsed.

**Core logic**
- Type mapping by `idxType`: `buy`/`transfer`→BUY, `sell`→SELL.
- Currency fixed to EUR.
- `id` from order-id column.
- `name` + `isin` use same symbol column.
- Fee/tax currently set to zero.

---

### 3.3 DeGiro (`ParserDeGiro`)

**Input format**
- Account CSV with `,` separator.
- Parser first unquotes number fields containing commas.

**Core logic**
- Not line-by-line generic parsing; custom pipeline:
  - groups rows by ISIN,
  - extracts buys, sells, fees, dividends, dividend taxes, fx lines.
- BUY/SELL parsed from description regex:
  - `(?<typ>Verkauf|Kauf) (?<amt>[\d.]+) zu je (?<prc>[0-9,.]+) ...`
- Dividend rows and matching dividend-tax rows are merged into one DIV transaction.
- Extra transaction types:
  - `Zinsen`/`Flatex Interest` → INTEREST,
  - `ADR/GDR Weitergabegebühr` → FEE.
- Non-EUR values are converted using an exchange-rate API + local cache (`exchangerates.properties`).

---

### 3.4 Finanzen.Net Zero CSV (`ParserFinanzenZeroCsv`, deprecated)

**Input format**
- `;` separated CSV.
- Key columns: date, total, status, and a combined name/type field.

**Core logic**
- Ignores non-booked rows (`status != gebucht`).
- Ignores account movements (`Gutschrift:`, reference-account withdrawals).
- Type detection from text snippets in one field:
  - `Coupons/Dividende:`→DIV
  - `- Kauf`→BUY
  - `- Verkauf` or `Abrechnung Verkauf`→SELL
  - `Steuerausgleich`→TAX
  - `KKT-Abschluss`→FEE
- ISIN and Order-Nr parsed via regex from text payload.
- Crypto-like IDs (`XX`/`XC`) get synthetic ids (`crypto#...`).

---

### 3.5 Finanzen.Net Zero PDF (`ParserFinanzenZeroPdf`)

**Input format**
- PDF text extraction + regex-based parsing.

**Core logic**
- Unsupported docs filtered (e.g. crypto buy/sell statements).
- Type detection by header patterns:
  - Buy/Sell, Dividende/Fondsausschüttung, Steuerausgleich/Steuerkorrektur, Spin-Off.
- IDs built as `Vorgangs-Nr#Referenz-Nr`.
- Key regex fields:
  - ISIN from `ISIN: ...`
  - Amount from `^STK ...`
  - Total from `Valuta ... EUR ...`
  - Date from `Details zur Ausführung`, `Zahltag`, or first date line.
- Tax parsing supports both refund and debit patterns.
- Has hardcoded overwrite map for known corporate actions/ISIN migrations.

---

### 3.6 ING-DiBa (`ParserIngDiba`)

**Input format**
- PDF text statements.

**Core logic**
- Type detection using document keywords:
  - `Wertpapierabrechnung KAUF/VERKAUF`
  - dividends (`Ertragsgutschrift`, `Dividendengutschrift`, ...)
  - knockout (`Rückzahlung`, `Einlösung`, `Wertpapier Ausgang`)
  - tax (`Vorabpauschale`)
  - pre-emptive rights and ISIN switch markers (`Umtausch Ein/Ausgang`)
- Date from `Valuta`, `Zahltag`, `Datum:`.
- ISIN/WKN from `ISIN (WKN)` line.
- Amount from variants of `Nominale ... Stück`.
- Fees and taxes extracted from multiple specific labels.
- Large overwrite map for known ticker/ISIN history edge cases.

---

### 3.7 Quirion (`ParserQuirion`)

**Input format**
- TSV (`\t`) export.

**Core logic**
- Classifies lines into buys/sells/divs/taxes/fees via type column text.
- Handles `Storno` lines by removing matching prior buy/sell.
- No real ISIN in export: creates synthetic ISIN via CRC32 hash of instrument name.
- Tax and fee rows mapped to standalone TAX / INTEREST-like entries.
- Buy vs Sell can fall back to total sign (negative=buy, positive=sell).

---

### 3.8 Scalable Capital CSV (`ParserScalableCapitalCsv`)

**Input format**
- `;` separated CSV with date/time/status/id/name/type/isin/amount/rate/total/fee/tax/currency.

**Core logic**
- Processes only `Status=Executed`.
- Unsupported types: `withdrawal`, `deposit`, `corporate action`.
- Type mapping:
  - Distribution→DIV
  - Buy/Savings plan→BUY
  - Sell→SELL
  - Taxes→TAX
  - Interest→INTEREST
  - Fee→FEE
- Requires EUR only (non-EUR throws).
- Important accounting rule: CSV total is exclusive of fees/taxes, so parser adds fee+tax back into transaction total (except tax/fee special cases).

---

### 3.9 Scalable Capital PDF (`ParserScalableCapitalPdf`)

**Input format**
- PDF text, Baader style.

**Core logic**
- Type detection similar to Finanzen PDF parser.
- ID built from `Vorgangs-Nr#Referenz-Nr`.
- Amount/ISIN/Date/Total/Rate parsed with dedicated regex patterns.
- Tax parser supports either `Steuerausgleichsrechnung` refund pattern or standard tax lines.
- Mindermengenzuschlag recognized as fee.

---

### 3.10 Scalable Capital RKK (`ParserScalableCapitalRkk`)

**Input format**
- `;` CSV variant with different field positions.

**Core logic**
- Type detection from column `idxType` (`WP-Abrechnung Kauf/Verkauf`, `Coupons/Dividende`, `Steuerausgleich`).
- Unsupported accounting rows filtered (`saldovortrag`, `saldo`, `gutschrift`, `lastschrift aktiv`, `sepa-ueberweisung`).
- Currency fixed EUR.
- Uses computed rate from `total/amount`.
- Uses generated IDs (`System.nanoTime()`) because order-id handling is not finalized.

---

### 3.11 Scalable Capital WUR (`ParserScalableCapitalWur`)

**Input format**
- `;` CSV variant with WUR column layout.

**Core logic**
- Type values are uppercase short labels (`KAUF`, `VERKAUF`, `DVIDENDE`).
- Reads explicit order id, ISIN, amount, rate, total from fixed indexes.
- Fee/tax currently default to zero.

---

### 3.12 SmartBroker (`ParserSmartBroker`)

**Input format**
- `;` CSV export.

**Core logic**
- Type mapping from type column:
  - `KAUF`, `VERKAUF`, `AUSBUCHUNG`(KNO),
  - `Dividenden, Erträge` / `Ausschüttung`(DIV).
- Additional `status` column is parsed and stored.
- Date uses different columns for DIV vs others.
- Currency parsed from currency column; dividend non-EUR totals converted via exchange-rate lookup.
- Fee inferred as difference between abs(total-from-rate) and abs(file-total).

---

### 3.13 Trade Republic (`ParserTradeRepublic`)

**Input format**
- PDF text.

**Core logic**
- Very regex-heavy parser with many statement variants:
  - buy/sell from Market/Limit/Stop-Limit/Stop-Market and ex-ante docs,
  - dividends by `Dividende|Ausschüttung ... Ex-Tag`,
  - taxes by `STEUERABRECHNUNG` / `Vorabpauschale`,
  - interest by `ABRECHNUNG ZINSEN`,
  - knockouts by `TILGUNG`/`Ausbuchung`,
  - spin-offs by `SPIN-OFF`.
- Crypto statements detected and mapped to synthetic ISIN `TR-<token>`.
- IDs:
  - buy/sell: `ORDER#AUSFÜHRUNG`,
  - others: footer parser (`ABRE/DUAN/AVISO`).
- Total parsed from `BUCHUNG` block (fallbacks for ex-ante formats).
- Tax/fee parsed from multiple line patterns.
- Contains overwrite map for known rounding/data anomalies and ISIN corrections.

---

### 3.14 Vivid Money (`ParserVivid`)

**Input format**
- Actually reads PDF text and splits fixed-width-like lines into 16 whitespace-separated fields.

**Core logic**
- Extracts transaction block between `Währungsumr...` and `Ausgegeben am ...`.
- Parses only `BUY` / `SELL` lines.
- Uses ticker as both `name` and `isin`.
- Transaction id: `date@time#ticker`.
- Fee from dedicated field.

---

## 4) Current generated website (XML + XSL → HTML)

## 4.1 Main report sections

The generated HTML contains:

1. **Top overview table** with totals for invested/open total, win/loss, tax, fee, interest.
2. **Yearly summary matrix**:
   - columns = selected years,
   - rows = parser types,
   - plus annual total row,
   - dividend row (with share of annual result),
   - trade count row,
   - fees+taxes row.
3. **Monthly table (`byMonth`)**:
   - per year (row), all 12 months (cells),
   - each cell shows monthly win/loss and monthly dividend sum.
4. **Trade table (`byDate`)**:
   - grouped by calendar week (recent weeks),
   - includes weekly subtotal row,
   - columns: Date, Name, Isin, Amount, Rate, Total, Win/Loss FIFO, Win/Loss AVG, %, Tax, Fee, Interest.
5. **Open positions table (`byIsinOpen`)** with expandable transaction details.
6. **Dividends table (`byDividends`)** sorted by annual dividend contribution per instrument.
7. **Closed positions table (`byIsinClosed`)** with expandable transaction details.

## 4.2 Interactivity currently implemented in JS inside XSL

- Show/hide sections (`By Date`, `By Month`, `By ISIN Open`, `By Dividends`, `By ISIN Closed`).
- Filter `byDate` by parser via checkboxes.
- Filter `byDate` by transaction type (BUY/SELL/DIV/FEE/INTEREST/TAX).
- Expand/collapse detail rows by ISIN.
- Click-to-mark rows for ad-hoc sum calculations.
- Weekly and selected-row running sum display helpers.

## 4.3 Visual semantics currently encoded in CSS

- Color coding by parser (link color) and by transaction type:
  - SELL (green), BUY/TAX/FEE/INTEREST (red), DIV (blue), temporary/split markers (yellow).
- Dark theme table styling.
- Hover highlighting for quick scanning.

## 5) Fit to your requested Vue 3 target UI

Your requested features map well to existing behavior:

- **Yearly summary table** (columns=year, rows=parser + dividend total + taxes/fees + trade count) already exists in XSL logic.
- **Monthly overview table** (profit/loss and dividends by month) already exists (`MonTemplate`).
- **Trade table with filters + colors + calendar-week grouping + weekly total** already exists (`CwTemplate` + JS toggles + CSS classes).
- Trade columns you listed (`date,name,isin,amount,rate,total,winloss fifo,winloss avg,tax,fee,interest`) already match the `byDate` table output.

## 6) Migration recommendations (for Vue 3 implementation)

1. Keep parser modules isolated and pure (`parse(input) -> Transaction[]`).
2. Preserve one normalized domain schema before any UI rendering.
3. Implement parser test fixtures per broker statement type (CSV/PDF variants).
4. Move regex + column maps into declarative config files where possible.
5. Build UI in three independent panes:
   - yearly summary,
   - monthly summary,
   - transaction explorer (filters + CW grouping + weekly subtotal).
6. Keep overwrite rules externalized (JSON/YAML) instead of hardcoded closures.
7. Keep PDF text extraction isolated behind adapter interface for browser/server choice.

