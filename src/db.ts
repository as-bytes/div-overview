import Dexie, { Entity, type EntityTable } from "dexie";
import { Dividend, Isin, IsinType, QSt } from "./models";

const dexieSchema = {
  config: 'key',
  dividends: '++id, isin, amount, date, tags',
  qst: 'iso, name, taxRate, creditable, refundable',
  isins: 'isin, name, type'
} as const;

class DB extends Dexie {
  config!: Dexie.Table<Record<string, boolean> | boolean, string>;
  dividends!: EntityTable<DividendEntry, 'id'>;
  qst!: EntityTable<QStEntry, 'iso'>;
  isins!: EntityTable<IsinEntry, 'isin'>;

  constructor() {
    super('QStDB');
    this.version(2).stores(dexieSchema);

    this.dividends.mapToClass(DividendEntry);
    // todo why and is it necessary?
    // this.qst.mapToClass(QStEntry);
    this.isins.mapToClass(IsinEntry);

    this.qst.count().then((count) => {
      if (count === 0) {
        console.debug('init DB - no stored QSt-info using default')
        this.qst.bulkAdd(getInitialQstSettings());
      } else {
        console.debug('init DB');
      }
    })
  }

  async getIsinsAll(): Promise<IsinEntry[]> {
    return await db.isins.toArray();
  }

  async getIsin(isin: string): Promise<IsinEntry | undefined> {
    return await db.isins.get(isin);
  }

  async updIsin(isin: string, data: Isin): Promise<void> {
    await db.isins.put({ name: data.name, type: data.type, isin: data.isin }, isin);
  }


  async getQst(iso: string): Promise<QStEntry | undefined> {
    return await db.qst.get(iso.substring(0, 2).toUpperCase());
  }

  async addQst(qst: QSt): Promise<string> {
    return await db.qst.add({ ...qst });
  }

  async updQst(iso: string, updates: QSt): Promise<number> {
    return await db.qst.update(iso.substring(0, 2).toUpperCase(), { ...updates });
  }

  async delQst(iso: string): Promise<QSt[]> {
    await db.qst.delete(iso.substring(0, 2).toUpperCase());
    return await this.getQstsAll();
  }

  async getQstsAll(): Promise<QSt[]> {
    return await db.qst.toArray();
  }

  async getDivsAll(): Promise<Dividend[]> {
    return await db.dividends.toArray();
  }

  async bulkAddDivs(data: Dividend[]): Promise<Dividend[]> {
    await db.dividends.clear();
    await db.dividends.bulkAdd(data);
    return await this.getDivsAll();
  }

  async bulkAddQsts(data: QSt[]): Promise<QSt[]> {
    await db.qst.clear();
    await db.qst.bulkAdd(data);
    return await this.getQstsAll();
  }

  async bulkAddIsins(data: Isin[]): Promise<Isin[]> {
    await db.isins.clear();
    await db.isins.bulkAdd(data);
    return await this.getIsinsAll();
  }


}

class IsinEntry extends Entity<DB> implements Isin {
  isin!: string;
  name!: string;
  type!: IsinType;
}

class QStEntry extends Entity<DB> implements QSt {
  iso!: string;
  name!: string;
  taxRate!: number;
  creditable!: number;
  refundable!: number;
}

class DividendEntry extends Entity<DB> implements Dividend {
  id!: number;
  isin!: string;
  amount!: number;
  date!: Date;
  tags!: string[];
}

export const db = new DB();

export async function addDivSingle(div: Dividend): Promise<number> {
  return await db.dividends.add({ isin: div.isin, amount: div.amount, date: div.date, tags: [...div.tags] });
}

export async function addDivMany(divs: Dividend[]): Promise<number> {
  return await db.dividends.bulkAdd(divs.map((div) => ({ isin: div.isin, amount: div.amount, date: div.date, tags: [...div.tags] })));
}

export async function updDiv(id: number, updates: Partial<Dividend>): Promise<void> {
  const cloneDiv = { ...updates };
  if (cloneDiv.tags) {
    cloneDiv.tags = cloneDiv.tags.map(tag => String(tag));
  }

  await db.dividends.update(id, cloneDiv);
}

export async function delDiv(id: number): Promise<Dividend[]> {
  await db.dividends.delete(id);
  return getDivs();
}

export async function getDivs(): Promise<Dividend[]> {
  return await db.dividends.toArray();
}


export async function updCfg<T>(key: string, value: Record<string, boolean> | boolean): Promise<T> {
  console.debug('updating config', key, JSON.parse(JSON.stringify(value)));
  await db.config.put(JSON.parse(JSON.stringify(value)), key);
  return value as unknown as T;
}

export async function getCfg<T>(key: string, fallback: T): Promise<T> {
  const value = (await db.config.get(key)) as T ?? fallback;
  console.debug('get config', key, JSON.parse(JSON.stringify(value)));
  return value;
}

export async function delDb(): Promise<void> {
  db.dividends.clear();
}

function getInitialQstSettings(): QSt[] {
  return [
    { iso: 'DE', name: 'Deutschland', taxRate: 30.5, creditable: 0, refundable: 0 },
    { iso: 'DK', name: 'Dänemark', taxRate: 27, creditable: 15, refundable: 27 - 15 },
    { iso: 'FI', name: 'Finnland', taxRate: 35, creditable: 15, refundable: 20 },
    { iso: 'FR', name: 'Frankreich', taxRate: 30, creditable: 12.8, refundable: 17.2 },
    { iso: 'IE', name: 'Irland', taxRate: 25, creditable: 0, refundable: 0 },
    { iso: 'IT', name: 'Italien', taxRate: 26, creditable: 15, refundable: 11 },
    { iso: 'JE', name: 'Jersey', taxRate: 0, creditable: 15, refundable: 0 },
    { iso: 'NL', name: 'Niederlande', taxRate: 15, creditable: 15, refundable: 0 },
    { iso: 'NO', name: 'Norwegen', taxRate: 25, creditable: 15, refundable: 10 },
    { iso: 'AT', name: 'Österreich', taxRate: 27.5, creditable: 15, refundable: 12.5 },
    { iso: 'CH', name: 'Schweiz', taxRate: 35, creditable: 15, refundable: 20 },
    { iso: 'SE', name: 'Schweden', taxRate: 30, creditable: 15, refundable: 15 },
    { iso: 'ES', name: 'Spanien', taxRate: 19, creditable: 15, refundable: 4 },
    { iso: 'TR', name: 'Türkei', taxRate: 10, creditable: 10, refundable: 0 },
    { iso: 'US', name: 'USA', taxRate: 30, creditable: 15, refundable: 15 }
  ]
}

