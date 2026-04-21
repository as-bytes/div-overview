import { DATA_VERSION, type ParserType, type Transaction, type TransType } from '../trd-models';

export const toNum = (value: string | number | undefined): number => {
  if (value == null || value === '') return 0;
  if (typeof value === 'number') return Math.abs(value);
  const normalized = value.trim().replace(/\./g, '').replace(',', '.');
  const parsed = Number.parseFloat(normalized);
  return Number.isFinite(parsed) ? Math.abs(parsed) : 0;
};

export const isoDate = (value: string): string => {
  if (/^\d{4}-\d{2}-\d{2}/.test(value)) return `${value.slice(0, 10)}T00:00:00Z`;
  const [d, m, y] = value.split('.');
  return `${y}-${m}-${d}T00:00:00Z`;
};

export const makeTx = (parserType: ParserType, type: TransType): Transaction => ({
  id: '', parserType, type, date: '', isin: '', currency: 'EUR', amount: 0, openAmount: 0, rate: 0,
  total: 0, tax: 0, fee: 0, interest: 0, winlossFifo: 0, winlossAvg: 0, crypto: false, spinOff: false,
  temporaryStatement: false, overridden: false, dataVersion: DATA_VERSION,
});

export const csvRows = (content: string, sep: string): string[][] => content
  .split(/\r?\n/)
  .filter((line) => line.trim().length > 0)
  .map((line) => line.split(sep).map((x) => x.replace(/^"|"$/g, '').trim()));
