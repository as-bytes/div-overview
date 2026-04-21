import type { Share, Transaction } from '../trd-models';

const byDate = (a: Transaction, b: Transaction) => Date.parse(a.date) - Date.parse(b.date);

export const aggregateShares = (transactions: Transaction[]): { shares: Share[]; errors: string[] } => {
  const groups = new Map<string, Transaction[]>();
  for (const tx of transactions) {
    if (!groups.has(tx.isin)) groups.set(tx.isin, []);
    groups.get(tx.isin)!.push(tx);
  }

  const errors: string[] = [];
  const shares: Share[] = [];

  for (const [isin, txs] of groups.entries()) {
    txs.sort(byDate);
    const share: Share = {
      isin, name: txs[0]?.name ?? isin, transactions: txs, amount: 0, dividend: 0, invested: 0,
      winloss: 0, tax: 0, fee: 0, interest: 0, openAmount: 0, openTotal: 0, rate: 0, isClosed: false,
    };

    const buys = txs.filter((t) => ['BUY', 'CAPITAL', 'PREEMPTIVE'].includes(t.type));
    for (const tx of txs) {
      share.tax += tx.tax;
      share.fee += tx.fee;
      share.interest += tx.interest;
      if (tx.type === 'DIV') {
        tx.winlossFifo = tx.total;
        tx.winlossAvg = tx.total;
        share.winloss += tx.total;
        share.dividend += tx.total;
      }
      if (['BUY', 'PREEMPTIVE', 'INTEREST'].includes(tx.type)) {
        share.amount += tx.amount;
        share.invested += tx.total;
      }
      if (tx.type === 'SELL' || tx.type === 'KNO') {
        let remaining = tx.amount;
        const eligible = buys.filter((b) => b.openAmount > 0 && Date.parse(b.date) <= Date.parse(tx.date));
        const totalOpen = eligible.reduce((sum, b) => sum + b.openAmount, 0);
        if (totalOpen < remaining) {
          errors.push(`Impossible FIFO for ${isin} at ${tx.date}`);
          continue;
        }
        const avgRate = totalOpen > 0 ? eligible.reduce((s, b) => s + (b.openAmount * b.rate), 0) / totalOpen : 0;
        let fifoCost = 0;
        for (const buy of eligible) {
          if (remaining <= 0) break;
          const consumed = Math.min(remaining, buy.openAmount);
          fifoCost += consumed * buy.rate;
          buy.openAmount -= consumed;
          remaining -= consumed;
          if ((Date.parse(tx.date) - Date.parse(buy.date)) < 365 * 24 * 3600 * 1000) tx.hasBuyTransAgeUnder365Days = true;
        }
        const sellRevenue = tx.amount * tx.rate;
        tx.winlossFifo = sellRevenue - fifoCost + tx.tax + tx.fee + tx.interest;
        tx.winlossAvg = sellRevenue - (tx.amount * avgRate) + tx.tax + tx.fee + tx.interest;
        share.winloss += tx.winlossFifo;
        share.amount -= tx.amount;
      }
    }
    share.openAmount = Math.max(share.amount, 0);
    share.openTotal = buys.reduce((sum, b) => sum + (b.openAmount * b.rate), 0);
    share.rate = share.openAmount > 0 ? share.openTotal / share.openAmount : 0;
    share.isClosed = share.openAmount === 0;
    shares.push(share);
  }

  return { shares, errors };
};
