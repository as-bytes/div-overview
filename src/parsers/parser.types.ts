import type { ImportArtifact, ImportError, ParserType, Transaction } from '../trd-models';

export interface ParseResult {
  transactions: Transaction[];
  errors: ImportError[];
}

export interface ParserModule {
  parserType: ParserType;
  supports(input: ImportArtifact): boolean;
  parse(input: ImportArtifact): ParseResult;
  overwrites?: Record<string, (tx: Transaction) => Transaction>;
}
