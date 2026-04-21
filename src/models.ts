export interface EntryNew {
  id: number | null,
  isin: string,
  amount: number | null,
  date: Date | null,
  tags: Array<string>;
}

export interface Dividend {
  id: number;
  isin: string;
  amount: number;
  date: Date;
  tags: Array<string>;
}

export interface QSt {
  iso: string,
  name: string,
  taxRate: number,
  creditable: number,
  refundable: number,
}

export enum IsinType {
  STOCK = 'Common Stock',
  ETF = 'ETF',
}

export interface Isin {
  isin: string,
  name: string,
  type: IsinType,
}

export type CountryTax = Record<string, {
  name: string,
  taxRate: number,
  creditable: number,
  refundable: number,
  flag: string
}>

export type Snack = {
  text: string,
  color: 'error' | 'primary' | 'success' | 'warning',
  timeout?: number,
}

export type ChartPeriodData = { date: string, label: string, tags: [], totals: Record<string, number> };
export type TimePeriod = 'week' | 'month' | 'quarter' | 'annual';

export type PeriodData = {
  keyFormat: string,
  labelFormat: string,
  title: string,
};

export type IsinOverview = {
  isin: string;
  total: number;
  'year-0': number;
  'year-1': number;
  'year-2': number;
  'year-3': number;
}