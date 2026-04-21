import type { ImportArtifact, ImportError } from '../trd-models';
import { csvRows, isoDate, makeTx, toNum } from './parser.utils';
import type { ParseResult, ParserModule } from './parser.types';

const mapType = (type: string) => {
  if (type === 'BUY') return 'BUY' as const;
  if (type === 'SELL') return 'SELL' as const;
  if (type === 'EARNINGS') return 'DIV' as const;
  if (type === 'TAX_REFUND') return 'TAX' as const;
  return 'UNSUPPORTED' as const;
};

export const traderepublicCsvParser: ParserModule = {
  parserType: 'TRADEREPUBLIC',
  supports(input) {
    return input.content.includes('transaction_id') && input.content.includes('account_type');
  },
  parse(input: ImportArtifact): ParseResult {
    const rows = csvRows(input.content, ',');
    const header = rows[0] ?? [];
    const errors: ImportError[] = [];
    const get = (row: string[], key: string) => row[header.indexOf(key)] ?? '';
    const transactions = rows.slice(1).flatMap((row, idx) => {
      try {
        const type = mapType(get(row, 'type'));
        if (type === 'UNSUPPORTED') return [];
        const tx = makeTx('TRADEREPUBLIC', type);
        tx.date = isoDate(get(row, 'date'));
        tx.isin = get(row, 'symbol') || (type === 'TAX' ? 'TR-TAX' : '');
        tx.currency = get(row, 'currency') || 'EUR';
        tx.name = get(row, 'name');
        tx.amount = toNum(get(row, 'shares'));
        tx.tax = toNum(get(row, 'tax'));
        tx.total = toNum(get(row, 'amount'));
        tx.rate = tx.amount > 0 ? tx.total / tx.amount : toNum(get(row, 'price'));
        tx.fee = toNum(get(row, 'fee'));
        tx.id = get(row, 'transaction_id') || `${type.toLowerCase()}#${tx.isin}#${tx.date}`;
        tx.openAmount = tx.amount;
        tx.sourceFileName = input.fileName;
        tx.sourceLastModified = input.lastModified;
        tx.sourceSize = input.size;
        return [tx];
      } catch (error) {
        errors.push({ line: idx + 2, parser: 'TRADEREPUBLIC', message: (error as Error).message, raw: row.join(',') });
        return [];
      }
    });
    return { transactions, errors };
  },
};
