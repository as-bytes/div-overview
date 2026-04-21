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

const onFiles = async (ev: Event) => {
  const target = ev.target as HTMLInputElement;
  const files = Array.from(target.files ?? []);
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
  errors.value = [...parsed.errors, ...aggr.errors.map((message) => ({ line: 0, parser: 'SCALABLE' as const, message, raw: '' }))];

  const now = new Date().toISOString();
  await trdDb.transactions.clear();
  await trdDb.transactions.bulkAdd(parsed.transactions.map((tx) => ({ ...tx, pk: `${tx.parserType}:${tx.id}:${tx.isin}:${tx.date}` })));
  await trdDb.imports.put({ id: crypto.randomUUID(), importedAt: now, fileCount: files.length, warnings: [], parserErrors: errors.value });
};
</script>

<template>
  <v-app>
    <v-main class="pa-4">
      <h1>trd-ovr-vue</h1>
      <p>Phase 1 MVP: FinZero/Trade Republic/Scalable CSV parser + parity reports.</p>
      <v-file-input label="Import CSV files" multiple accept=".csv" @change="onFiles" />

      <v-alert v-if="errors.length" type="warning" title="Import diagnostics" class="mb-4">
        <div v-for="(err, idx) in errors" :key="idx">Line {{ err.line }} ({{ err.parser }}): {{ err.message }}</div>
      </v-alert>

      <v-row>
        <v-col cols="12" md="2" v-for="(val, key) in top" :key="key">
          <v-card><v-card-title>{{ key }}</v-card-title><v-card-text>{{ Number(val).toFixed(2) }}</v-card-text></v-card>
        </v-col>
      </v-row>

      <h2>Annual matrix</h2>
      <v-table density="compact"><tbody><tr v-for="(years, parser) in annual" :key="parser"><td>{{ parser }}</td><td v-for="(value, y) in years" :key="y">{{ y }}: {{ value.toFixed(2) }}</td></tr></tbody></v-table>

      <h2>Monthly matrix ({{ selectedYear }})</h2>
      <v-text-field v-model.number="selectedYear" type="number" label="Year" max-width="180" />
      <v-table density="compact"><tbody><tr><td v-for="(m, idx) in monthly" :key="idx">{{ idx + 1 }}: {{ m.toFixed(2) }}</td></tr></tbody></v-table>

      <h2>By-date (calendar week)</h2>
      <div v-for="(txs, week) in byWeek" :key="week" class="mb-3">
        <strong>{{ week }}</strong> — subtotal: {{ txs.reduce((s, t) => s + t.winlossFifo, 0).toFixed(2) }}
        <v-table density="compact"><tbody><tr v-for="tx in txs" :key="`${tx.parserType}-${tx.id}`"><td>{{ tx.date.slice(0,10) }}</td><td>{{ tx.name }}</td><td>{{ tx.isin }}</td><td>{{ tx.amount }}</td><td>{{ tx.rate.toFixed(4) }}</td><td>{{ tx.total.toFixed(2) }}</td><td>{{ tx.winlossFifo.toFixed(2) }}</td><td>{{ tx.winlossAvg.toFixed(2) }}</td><td>{{ tx.tax.toFixed(2) }}</td><td>{{ tx.fee.toFixed(2) }}</td><td>{{ tx.interest.toFixed(2) }}</td></tr></tbody></v-table>
      </div>

      <h2>By-ISIN open</h2>
      <v-table density="compact"><tbody><tr v-for="s in openShares" :key="s.isin"><td>{{ s.isin }}</td><td>{{ s.name }}</td><td>{{ s.openAmount }}</td><td>{{ s.openTotal.toFixed(2) }}</td></tr></tbody></v-table>

      <h2>Dividends</h2>
      <v-table density="compact"><tbody><tr v-for="d in dividends" :key="d.id"><td>{{ d.date.slice(0,10) }}</td><td>{{ d.name }}</td><td>{{ d.isin }}</td><td>{{ d.total.toFixed(2) }}</td></tr></tbody></v-table>

      <h2>By-ISIN closed</h2>
      <v-table density="compact"><tbody><tr v-for="s in closedShares" :key="s.isin"><td>{{ s.isin }}</td><td>{{ s.name }}</td><td>{{ s.winloss.toFixed(2) }}</td></tr></tbody></v-table>
    </v-main>
  </v-app>
</template>
