<script setup lang="ts">
import { computed, ref } from 'vue';
import { parseImports } from '../trd/import-pipeline';
import { aggregateShares } from '../trd/aggregation';
import { annualMatrix, monthlyMatrix, overview, weekOfYear } from '../trd/reports';
import type { ImportArtifact, ImportError, Share, Transaction } from '../trd-models';
import { trdDb } from '../trd-db';

const transactions = ref<Transaction[]>([]);
const shares = ref<Share[]>([]);
const errors = ref<ImportError[]>([]);
const selectedYear = ref<number>(new Date().getUTCFullYear());

const annual = computed(() => annualMatrix(transactions.value));
const monthly = computed(() => monthlyMatrix(transactions.value, selectedYear.value));
const top = computed(() => overview(shares.value));
const openShares = computed(() => shares.value.filter((s) => !s.isClosed));
const closedShares = computed(() => shares.value.filter((s) => s.isClosed));
const dividends = computed(() => transactions.value.filter((t) => t.type === 'DIV').sort((a, b) => b.total - a.total));
const byWeek = computed(() => {
  const grouped: Record<string, Transaction[]> = {};
  for (const tx of transactions.value) {
    const key = weekOfYear(tx.date);
    grouped[key] ??= [];
    grouped[key].push(tx);
  }
  return grouped;
});

const formatNum = (value: unknown, fraction = 2): string => {
  const num = Number(value);
  return Number.isFinite(num) ? num.toFixed(fraction) : '0.00';
};

const onFiles = async (ev: Event) => {
  const target = ev.target as HTMLInputElement;
  const files = Array.from(target.files ?? []);
  if (files.length === 0) return;

  const inputs: ImportArtifact[] = await Promise.all(files.map(async (file) => ({
    fileName: file.name,
    content: await file.text(),
    lastModified: file.lastModified,
    size: file.size,
  })));

  const parsed = parseImports(inputs);
  const aggr = aggregateShares(parsed.transactions);
  transactions.value = parsed.transactions;
  shares.value = aggr.shares;
  errors.value = [
    ...parsed.errors,
    ...aggr.errors.map((message) => ({ line: 0, parser: 'SCALABLE' as const, message, raw: '' })),
  ];

  const now = new Date().toISOString();
  await trdDb.transactions.clear();
  await trdDb.transactions.bulkAdd(parsed.transactions.map((tx) => ({ ...tx, pk: `${tx.parserType}:${tx.id}:${tx.isin}:${tx.date}` })));
  await trdDb.imports.put({
    id: crypto.randomUUID(),
    importedAt: now,
    fileCount: files.length,
    warnings: [],
    parserErrors: JSON.parse(JSON.stringify(errors.value)),
  });
};
</script>

<template>
  <v-app>
    <v-main>
      <div class="app-container report-shell">
        <v-card class="pa-6 mb-4 report-header glass-backdrop" elevation="6">
          <h1 class="text-h3 font-weight-bold mb-2">trd-ovr-vue</h1>
          <p class="text-subtitle-1 text-medium-emphasis mb-0">Phase 1 MVP: FinZero / Trade Republic / Scalable CSV parser + parity reports.</p>
        </v-card>

        <v-card class="mb-4" elevation="4">
          <v-card-title>Import</v-card-title>
          <v-card-text>
            <v-file-input label="Import CSV files" multiple accept=".csv" @change="onFiles" variant="outlined" />
          </v-card-text>
        </v-card>

        <v-alert v-if="errors.length" type="warning" title="Import diagnostics" class="mb-4">
          <div v-for="(err, idx) in errors" :key="idx">Line {{ err.line }} ({{ err.parser }}): {{ err.message }}</div>
        </v-alert>

        <v-row>
          <v-col cols="12" md="2" v-for="(val, key) in top" :key="key">
            <v-card class="kpi-card" elevation="4">
              <v-card-title class="text-capitalize">{{ key }}</v-card-title>
              <v-card-text class="text-h6">{{ formatNum(val) }}</v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <h2 class="section-title">Annual matrix</h2>
        <v-table density="compact" class="report-table">
          <tbody>
            <tr v-for="row in annual" :key="row.parser">
              <td>{{ row.parser }}</td>
              <td v-for="entry in row.years" :key="`${row.parser}-${entry.year}`">{{ entry.year }}: {{ formatNum(entry.value) }}</td>
            </tr>
          </tbody>
        </v-table>

        <h2 class="section-title">Monthly matrix ({{ selectedYear }})</h2>
        <v-text-field v-model.number="selectedYear" type="number" label="Year" max-width="180" variant="outlined" />
        <v-table density="compact" class="report-table">
          <tbody>
            <tr>
              <td v-for="(m, idx) in monthly" :key="idx">{{ idx + 1 }}: {{ formatNum(m) }}</td>
            </tr>
          </tbody>
        </v-table>

        <h2 class="section-title">By-date (calendar week)</h2>
        <div v-for="(txs, week) in byWeek" :key="week" class="mb-4">
          <strong>{{ week }}</strong> — subtotal: {{ formatNum(txs.reduce((s, t) => s + t.winlossFifo, 0)) }}
          <v-table density="compact" class="report-table mt-2">
            <tbody>
              <tr v-for="tx in txs" :key="`${tx.parserType}-${tx.id}`">
                <td>{{ tx.date.slice(0, 10) }}</td>
                <td>{{ tx.name }}</td>
                <td>{{ tx.isin }}</td>
                <td>{{ tx.amount }}</td>
                <td>{{ formatNum(tx.rate, 4) }}</td>
                <td>{{ formatNum(tx.total) }}</td>
                <td>{{ formatNum(tx.winlossFifo) }}</td>
                <td>{{ formatNum(tx.winlossAvg) }}</td>
                <td>{{ formatNum(tx.tax) }}</td>
                <td>{{ formatNum(tx.fee) }}</td>
                <td>{{ formatNum(tx.interest) }}</td>
              </tr>
            </tbody>
          </v-table>
        </div>

        <h2 class="section-title">By-ISIN open</h2>
        <v-table density="compact" class="report-table">
          <tbody>
            <tr v-for="s in openShares" :key="s.isin">
              <td>{{ s.isin }}</td>
              <td>{{ s.name }}</td>
              <td>{{ formatNum(s.openAmount, 4) }}</td>
              <td>{{ formatNum(s.openTotal) }}</td>
            </tr>
          </tbody>
        </v-table>

        <h2 class="section-title">Dividends</h2>
        <v-table density="compact" class="report-table">
          <tbody>
            <tr v-for="d in dividends" :key="d.id">
              <td>{{ d.date.slice(0, 10) }}</td>
              <td>{{ d.name }}</td>
              <td>{{ d.isin }}</td>
              <td>{{ formatNum(d.total) }}</td>
            </tr>
          </tbody>
        </v-table>

        <h2 class="section-title">By-ISIN closed</h2>
        <v-table density="compact" class="report-table">
          <tbody>
            <tr v-for="s in closedShares" :key="s.isin">
              <td>{{ s.isin }}</td>
              <td>{{ s.name }}</td>
              <td>{{ formatNum(s.winloss) }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>
    </v-main>
  </v-app>
</template>

<style scoped>
.report-shell {
  padding-top: 16px;
  padding-bottom: 48px;
}

.report-header {
  border: 1px solid rgba(0, 0, 0, 0.10);
}

.section-title {
  margin: 20px 0 10px;
  font-size: 1.65rem;
  font-weight: 700;
}

.kpi-card {
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: linear-gradient(145deg, rgba(255,255,255,1), rgba(247,249,252,1));
}

.report-table {
  border-radius: 12px;
  overflow: hidden;
}

.report-table :deep(tbody tr:nth-child(odd)) {
  background-color: rgba(24, 103, 192, 0.04);
}
</style>
