export type CurrencyCode = 'EUR' | 'USD' | 'GBP' | string;

export type TransType =
  | 'BUY' | 'SELL' | 'DIV' | 'KNO' | 'TAX' | 'CAPITAL' | 'SPLIT' | 'SPLITBUY' | 'SPLITSELL'
  | 'PREEMPTIVE' | 'INTEREST' | 'FEE' | 'ISINSWITCH' | 'ISINSWITCHIN' | 'ISINSWITCHOUT'
  | 'UNSUPPORTED' | 'UNKNOWN';

export type ParserType = 'FINNETZERO_CSV' | 'TRADEREPUBLIC' | 'SCALABLE';

export interface Transaction {
  id: string;
  parserType: ParserType;
  type: TransType;
  date: string;
  isin: string;
  wkn?: string;
  name?: string;
  currency: CurrencyCode;
  amount: number;
  openAmount: number;
  rate: number;
  total: number;
  tax: number;
  fee: number;
  interest: number;
  winlossFifo: number;
  winlossAvg: number;
  crypto: boolean;
  spinOff: boolean;
  temporaryStatement: boolean;
  overridden: boolean;
  sourceFileName?: string;
  sourceRelativePath?: string;
  sourceLastModified?: number;
  sourceSize?: number;
  smartBrokerStatus?: 'EXECUTED' | 'CLEARED';
  deGiroSaldo?: number;
  hasBuyTransAgeUnder365Days?: boolean;
  dataVersion: number;
}

export interface Share {
  isin: string;
  name: string;
  transactions: Transaction[];
  amount: number;
  dividend: number;
  invested: number;
  winloss: number;
  tax: number;
  fee: number;
  interest: number;
  openAmount: number;
  openTotal: number;
  rate: number;
  isClosed: boolean;
}

export interface ImportError {
  line: number;
  parser: ParserType;
  message: string;
  raw: string;
}

export interface ImportArtifact {
  fileName: string;
  content: string;
  lastModified?: number;
  size?: number;
}

export const DATA_VERSION = 1;

export const SPECIAL_TYPES: TransType[] = ['KNO', 'TAX', 'DIV', 'CAPITAL', 'PREEMPTIVE', 'INTEREST', 'FEE'];
