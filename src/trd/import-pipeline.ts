import type { ImportArtifact, ImportError, Transaction } from '../trd-models';
import { finzeroCsvParser } from '../parsers/finzero.csv.parser';
import { scalableCsvParser } from '../parsers/scalable.csv.parser';
import { traderepublicCsvParser } from '../parsers/traderepublic.csv.parser';
import { validateTransaction } from './validation';

const parsers = [finzeroCsvParser, traderepublicCsvParser, scalableCsvParser];

export const parseImports = (inputs: ImportArtifact[]) => {
  const parsed: Transaction[] = [];
  const errors: ImportError[] = [];
  const byId = new Map<string, Transaction>();

  for (const input of inputs) {
    const parser = parsers.find((p) => p.supports(input));
    if (!parser) {
      errors.push({ line: 1, parser: 'FINNETZERO_CSV', message: `No parser for file ${input.fileName}`, raw: '' });
      continue;
    }

    const { transactions, errors: parserErrors } = parser.parse(input);
    errors.push(...parserErrors);

    for (const tx of transactions) {
      const key = `${tx.parserType}:${tx.id}`;
      if (byId.has(key)) tx.overridden = true;
      byId.set(key, tx);
      const validationErrors = validateTransaction(tx);
      if (validationErrors.length > 0) {
        errors.push({ line: 0, parser: tx.parserType, message: validationErrors.join('; '), raw: tx.id });
      } else {
        parsed.push(tx);
      }
    }
  }

  return { transactions: Array.from(new Map(parsed.map((tx) => [`${tx.parserType}:${tx.id}`, tx])).values()), errors };
};
