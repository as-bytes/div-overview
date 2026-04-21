import type { Share, Transaction } from '../trd-models';

const year = (date: string) => new Date(date).getUTCFullYear();
const month = (date: string) => new Date(date).getUTCMonth();

export const overview = (shares: Share[]) => shares.reduce((acc, s) => {
  acc.openTotal += s.openTotal;
  acc.invested += s.invested;
  acc.winloss += s.winloss;
  acc.tax += s.tax;
  acc.fee += s.fee;
  acc.interest += s.interest;
  return acc;
}, { openTotal: 0, invested: 0, winloss: 0, tax: 0, fee: 0, interest: 0 });

export const annualMatrix = (transactions: Transaction[]) => {
  const rows = new Map<string, Record<number, number>>();
  for (const tx of transactions) {
    const key = tx.parserType;
    if (!rows.has(key)) rows.set(key, {});
    rows.get(key)![year(tx.date)] = (rows.get(key)![year(tx.date)] ?? 0) + tx.winlossFifo;
  }

  return Array.from(rows.entries()).map(([parser, values]) => ({
    parser,
    years: Object.entries(values)
      .map(([y, value]) => ({ year: Number(y), value }))
      .sort((a, b) => a.year - b.year),
  }));
};

export const monthlyMatrix = (transactions: Transaction[], y: number) => {
  const data = Array.from({ length: 12 }, () => 0);
  transactions
    .filter((t) => year(t.date) === y)
    .forEach((t) => data[month(t.date)] += t.winlossFifo + (t.type === 'DIV' ? t.total : 0));
  return data;
};

export const weekOfYear = (isoDate: string): string => {
  const d = new Date(isoDate);
  d.setUTCHours(0, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil(((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7));
  return `${d.getUTCFullYear()}-CW${String(weekNo).padStart(2, '0')}`;
};
