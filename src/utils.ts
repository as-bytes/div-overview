import { Chart } from "chart.js";
import { EntryNew, PeriodData, QSt, TimePeriod } from "./models";
import { isValid, parse } from "date-fns";

export const readUploadedFile = (file: Blob) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target?.result);
    reader.onerror = (e) => reject(e);
    reader.readAsText(file);
  });
};

export function formatDate(date: Date | null | undefined | unknown, locale = 'de-DE'): string {
  return date == null || !(date instanceof Date) ? '' : date.toLocaleDateString(locale, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
}

export const formatDateISO = (date: Date | null | undefined | unknown): string => {
  return date == null || !(date instanceof Date) ? '' : date.toISOString().slice(0, 10);
};

export const formatCurrency = (num: number, addSymbol = true, zeroAsDash = false): string => {
  const options = { maximumFractionDigits: 2, minimumFractionDigits: 2, useGrouping: true };
  const str = `${num.toLocaleString('de-DE', options)}${(addSymbol) ? '\u00A0€' : ''}`;
  return (zeroAsDash && num === 0) ? str.replaceAll('0', '–') : str;
};

export function getEmptyEntry(): EntryNew {
  return {
    id: null,
    isin: '',
    amount: null,
    date: null,
    tags: []
  }
}

export function concatDateWithDots(value: string): string {
  const digits = value.replace(/\D/g, '')
  let result = ''
  if (digits.length > 0) result = digits.slice(0, 2)
  if (digits.length >= 2) result += '.' + digits.slice(2, 4)
  if (digits.length >= 4) result += '.' + digits.slice(4, 8)
  return result
}

export function ensureDate(obj: Date | string | null | undefined, format: string): Date | null {
  if (obj instanceof Date) return obj as Date;
  if (obj && obj.length === 10) return parse(`${obj}`, format, new Date())
  return null;
}

export function dateIsValid(obj: Date | string | null | undefined, format: string): Date | null {
  const date: Date | null = ensureDate(obj, format);
  const nowYear = new Date().getFullYear();
  return !!date && isValid(date) && date.getFullYear() > nowYear - 30 && date.getFullYear() <= nowYear ? date : null;
}

export const isDupe = (entries: EntryNew[], entry: EntryNew): boolean => {
  return entries.some(_ => {
    const sameIsin = _.isin === entry.isin;
    const sameAmount = _.amount === entry.amount;
    const sameDate = formatDate(_.date) === formatDate(entry.date);
    return sameIsin && sameAmount && sameDate;
  });
};

export const toStr = (entry: EntryNew): string => {
  return `${entry.isin} ${formatCurrency(entry.amount ?? 0, true)} ${formatDate(entry.date)}`;
};

export const getFlagUrlForIsoCode = (iso: string): string => {
  console.debug('getFlagUrlForIsoCode', iso);
  return `https://flagcdn.com/w40/${iso.toLowerCase()}.png`;
}

const floatStringToNumberOrZero = (str: string) => {
  // Remove any whitespace
  str = str.replace(/\s/g, '');

  // String might be a date
  if (str.indexOf('.') !== str.lastIndexOf('.')) {
    return 0;
  }

  // Determine format based on last separator (comma or period)
  const isGermanFormat = str.lastIndexOf(',') > str.lastIndexOf('.');

  // Clean the string based on format
  if (isGermanFormat) {
    // German format: remove dots for thousands, replace comma with period for decimal
    str = str.replace(/\./g, '').replace(',', '.');
  } else {
    // US format: remove commas for thousands, keep period for decimal
    str = str.replace(/,/g, '');
  }

  // Convert to number
  const num = parseFloat(str);

  // Return decimal if no integer part, otherwise return 0
  if (Number.isInteger(num)) {
    if (num > 0 && str.endsWith('.00')) {
      return num;
    } else {
      return 0;
    }
  } else {
    return num;
  }
};

export const findAmountInText = (text: string): number | undefined => {
  const numberMatches = text.match(/\d+([. ,]\d+)+/g);
  if (numberMatches) {
    // Convert to numbers (replace comma with dot if needed)
    const numbers = numberMatches.flatMap(num => {
      if (num.match(/\.\d\d$/g) || num.match(/,\d\d$/g)) {
        return [floatStringToNumberOrZero(num)].filter(num => !isNaN(num) && num > 0);
      } else {
        return [];
      }
    });

    if (numbers.length > 0) {
      return Math.max(...numbers);
    }


    console.debug('No valid numbers found in text:', text);
    console.debug('numberMatches found:', numberMatches);
    console.debug('numbers found:', numbers);
    console.warn('assuming multi-digit number');
    const numbersFallback = numberMatches.flatMap(num => {
      if (num.match(/\.\d+$/g) || num.match(/,\d+$/g)) {
        return [floatStringToNumberOrZero(num)].filter(num => !isNaN(num) && num > 0);
      } else {
        return [];
      }
    });

    console.debug('numbersFallback found:', numbersFallback);
    if (numbersFallback.length > 0) {
      return Math.max(...numbersFallback);
    }
  }

  return 0;
}

export const timePeriods: Record<TimePeriod, PeriodData> = {
  week: { keyFormat: 'yyyy-II', labelFormat: '\'KW\' II, yyyy', title: 'Wöchentlich' },
  month: { keyFormat: 'yyyy-MM', labelFormat: 'MMM yyyy', title: 'Monatlich' },
  quarter: { keyFormat: 'yyyy-QQQ', labelFormat: 'QQQ, yyyy', title: 'Quartalsweise' },
  annual: { keyFormat: 'yyyy', labelFormat: 'yyyy', title: 'Jählich' },
};

export const getFlag = (country: QSt): string => {
  return `https://flagcdn.com/w40/${country.iso.toLowerCase()}.png`;
};

export const stackedSumPlugin = {
  id: 'stackedSumPlugin',
  afterDatasetsDraw(chart: Chart, _args: unknown, _pluginOptions: unknown) {
    const { ctx, scales: { x, y } } = chart;

    chart.data.labels!.forEach((_label: unknown, index: number) => {
      let sum = 0;
      chart.data.datasets.forEach((dataset) => {
        const val = dataset.data[index] as number;
        if (!isNaN(val)) {
          sum += val;
        }
      });

      const xPosition = x.getPixelForValue(index);
      const yPosition = y.getPixelForValue(sum);

      ctx.save();
      ctx.font = '12px sans-serif';
      ctx.fillStyle = 'white';
      ctx.textAlign = 'center';
      ctx.fillText(`${sum.toLocaleString('de-DE')}`, xPosition, yPosition - 10);
      ctx.restore();
    });
  }
};

export const getNameForIsin = async (isin: string): Promise<string> => {
  const apitoken = 'd2f69s1r01qj3egrdv8gd2f69s1r01qj3egrdv90';
  const response = await fetch(`https://finnhub.io/api/v1/search?q=${isin}&token=${apitoken}`);
  const data = response.ok ? (await response.json()) : `Error fetching ISIN name: ${response.status} - ${response.statusText} for ISIN: ${isin} `;
  if (data && data.result && data.result.length > 0) {
    return data.result[0].description || '';
  } else {
    return '';
  }
};