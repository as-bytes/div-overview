import type { Transaction } from '../trd-models';

const allowNoIsin = new Set(['TAX', 'INTEREST', 'FEE']);

export const validateTransaction = (tx: Transaction): string[] => {
  const errors: string[] = [];
  if (!tx.date || Number.isNaN(Date.parse(tx.date))) errors.push('Invalid date');
  if (tx.type === 'UNKNOWN') errors.push('Unknown transaction type');
  const special = ['KNO', 'TAX', 'DIV', 'CAPITAL'].includes(tx.type) || tx.spinOff || tx.overridden;
  if (!special) {
    if (!tx.name) errors.push('Missing name');
    if (!(tx.amount > 0)) errors.push('Amount must be > 0');
  }
  if (!allowNoIsin.has(tx.type) && !tx.crypto && tx.isin.length !== 12) errors.push('ISIN must be 12 chars');
  if (!Number.isFinite(tx.rate)) errors.push('Rate must be finite');
  if ((tx.type === 'BUY' || tx.type === 'SELL') && !tx.spinOff && tx.rate <= 0) errors.push('Rate must be > 0 for BUY/SELL');
  if (!Number.isFinite(tx.total)) errors.push('Total required');
  if (!['KNO', 'DIV', 'PREEMPTIVE', 'CAPITAL'].includes(tx.type) && !tx.spinOff && tx.total === 0) errors.push('Total cannot be 0');

  const sign = tx.type === 'BUY' || tx.type === 'TAX' ? -1 : 1;
  const calcTotal = (tx.amount * tx.rate * sign) + tx.tax + tx.interest + tx.fee;
  const diff = tx.amount === 0 ? 0 : Math.abs(calcTotal - tx.total) / Math.max(1, tx.amount);
  if (Number.isFinite(calcTotal) && diff >= 0.01) errors.push(`Reconciliation mismatch (diff=${diff.toFixed(5)})`);
  return errors;
};
