import Dexie, { type EntityTable } from 'dexie';
import type { ImportError, Transaction } from './trd-models';

export interface RawDocument {
  id: string;
  sourceRelativePath: string;
  sourceSize?: number;
  sourceLastModified?: number;
  md5: string;
  parserType: string;
  importedAt: string;
  contentType: 'csv' | 'tsv' | 'pdf-text';
}

export interface ImportRun {
  id: string;
  importedAt: string;
  fileCount: number;
  warnings: string[];
  parserErrors: ImportError[];
}

export interface SettingRow { key: string; value: unknown; }
export interface ExchangeRate { key: string; yearMonth: string; from: string; to: string; rate: number; }

export class TrdDb extends Dexie {
  raw_documents!: EntityTable<RawDocument, 'id'>;
  transactions!: EntityTable<Transaction & { pk: string }, 'pk'>;
  imports!: EntityTable<ImportRun, 'id'>;
  settings!: EntityTable<SettingRow, 'key'>;
  exchange_rates!: EntityTable<ExchangeRate, 'key'>;

  constructor() {
    super('trd-ovr-vue');
    this.version(1).stores({
      raw_documents: 'id, sourceRelativePath, parserType, importedAt',
      transactions: 'pk, id, parserType, isin, date, dataVersion',
      imports: 'id, importedAt',
      settings: 'key',
      exchange_rates: 'key, yearMonth, from, to',
    });
  }
}

export const trdDb = new TrdDb();
