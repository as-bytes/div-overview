import type { ImportArtifact, ImportError } from '../trd-models';
import { csvRows, isoDate, makeTx, toNum } from './parser.utils';
import type { ParseResult, ParserModule } from './parser.types';

const mapType = (type: string) => {
  if (/^buy$/i.test(type) || /^savings plan$/i.test(type)) return 'BUY' as const;
  if (/^sell$/i.test(type)) return 'SELL' as const;
  if (/^distribution$/i.test(type)) return 'DIV' as const;
  if (/^taxes$/i.test(type)) return 'TAX' as const;
  if (/^fee$/i.test(type)) return 'FEE' as const;
  return 'UNSUPPORTED' as const;
};

export const scalableCsvParser: ParserModule = {
  parserType: 'SCALABLE',
  supports(input) {
    return input.content.includes('status;reference;description;assetType;type;isin');
  },
  parse(input: ImportArtifact): ParseResult {
    const rows = csvRows(input.content, ';');
    const [, ...data] = rows;
    const errors: ImportError[] = [];
    const transactions = data.flatMap((row, idx) => {
      try {
        const status = (row[2] ?? '').toLowerCase();
        if (status === 'pending' || status === 'cancelled') return [];
        if (status !== 'executed') return [];

        const type = mapType(row[6] ?? '');
        if (type === 'UNSUPPORTED') return [];

        const tx = makeTx('SCALABLE', type);
        tx.date = isoDate(row[0]);
        tx.isin = row[7] || (type === 'TAX' || type === 'FEE' ? 'SC-TAXFEE' : '');
        tx.currency = row[13] || 'EUR';
        tx.name = row[4];
        tx.amount = toNum(row[8]);
        tx.tax = type === 'TAX' ? toNum(row[10]) : toNum(row[12]);
        tx.rate = type === 'DIV' || type === 'TAX' || type === 'FEE' ? toNum(row[10]) : toNum(row[9]);
        tx.fee = type === 'FEE' ? toNum(row[10]) : toNum(row[11]);
        tx.total = toNum(row[10]) + (type === 'TAX' ? tx.fee : tx.tax + tx.fee);
        tx.id = row[3] || `${type.toLowerCase()}#${tx.isin}#${tx.date}`;
        tx.openAmount = tx.amount;
        tx.sourceFileName = input.fileName;
        tx.sourceLastModified = input.lastModified;
        tx.sourceSize = input.size;
        return [tx];
      } catch (error) {
        errors.push({ line: idx + 2, parser: 'SCALABLE', message: (error as Error).message, raw: row.join(';') });
        return [];
      }
    });
    return { transactions, errors };
  },
};
