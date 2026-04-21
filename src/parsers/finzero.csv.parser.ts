import type { ImportArtifact, ImportError } from '../trd-models';
import { makeTx, toNum, isoDate, csvRows } from './parser.utils';
import type { ParseResult, ParserModule } from './parser.types';

const rowType = (usage: string) => {
  if (usage.includes('Coupons/Dividende')) return 'DIV' as const;
  if (usage.includes('- Kauf')) return 'BUY' as const;
  if (usage.includes('- Verkauf')) return 'SELL' as const;
  if (usage.includes('Steuerausgleich')) return 'TAX' as const;
  return 'UNSUPPORTED' as const;
};

export const finzeroCsvParser: ParserModule = {
  parserType: 'FINNETZERO_CSV',
  supports(input: ImportArtifact): boolean {
    return input.content.includes('Verwendungszweck;IBAN');
  },
  parse(input: ImportArtifact): ParseResult {
    const rows = csvRows(input.content, ';');
    const [, ...data] = rows;
    const errors: ImportError[] = [];
    const transactions = data.flatMap((row, idx) => {
      const raw = row.join(';');
      try {
        const status = row[4]?.toLowerCase() ?? '';
        const usage = row[5] ?? '';
        if (usage.toLowerCase().includes('storno') || usage.toLowerCase().includes('storniert')) return [];
        if (status !== 'gebucht') return [];
        const type = rowType(usage);
        if (type === 'UNSUPPORTED') return [];

        const tx = makeTx('FINNETZERO_CSV', type);
        tx.date = isoDate(row[0]);
        tx.isin = (usage.match(/ISIN\s+([A-Z0-9]{12})/)?.[1] ?? (type === 'TAX' ? 'FINZERO-TAX' : ''));
        tx.currency = 'EUR';
        tx.name = usage.match(/\((.+) ISIN/)?.[1] ?? usage;
        tx.amount = toNum(usage.match(/STK\s+([\d.,]+)/)?.[1]);
        tx.tax = type === 'TAX' ? toNum(row[2]) : 0;
        tx.total = toNum(row[2]);
        tx.rate = tx.amount > 0 ? tx.total / tx.amount : tx.total;
        tx.fee = 0;
        const orderId = usage.match(/Order Nr\s+(\d+)/)?.[1];
        tx.id = orderId ?? `${type.toLowerCase()}#${tx.isin}#${tx.date}`;
        tx.openAmount = tx.amount;
        tx.sourceFileName = input.fileName;
        tx.sourceLastModified = input.lastModified;
        tx.sourceSize = input.size;
        return [tx];
      } catch (error) {
        errors.push({ line: idx + 2, parser: 'FINNETZERO_CSV', message: (error as Error).message, raw });
        return [];
      }
    });
    return { transactions, errors };
  },
};
