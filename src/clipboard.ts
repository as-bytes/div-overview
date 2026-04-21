import { getDivs } from "./db";
import { EntryNew } from "./models";
import { useMessagesStore } from "./stores";
import { findAmountInText, getEmptyEntry, isDupe, toStr } from "./utils";

export async function pasteCsvFromClipboard(max: number, skipDupes = false): Promise<EntryNew[]> {
  const divs: EntryNew[] = [];
  const text = await navigator.clipboard.readText();
  const lines = text.split('\n').filter(_ => _.trim().length > 0).slice(0, max);

  const stored = await getDivs();
  for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
    try {
      const newEntry = parseTestForData(lines[lineIndex], true);
      if (newEntry !== null) {
        if (!skipDupes && isDupe(stored, newEntry)) {
          if (confirm(`Mögliches Duplikat gefunden:\n${toStr(newEntry)}\n[OK] = Hinzufügen, [Abbrechen] = Überspringen`)) {
            divs.push(newEntry);
          }
        } else {
          divs.push(newEntry);
        }
      }
    } catch (err: unknown) {
      useMessagesStore().showError('', err as Error)
    }
  }

  const msg = (lines.length > max) ? `${max} von ${lines.length} Einträgen` : `${lines.length} Einträge`;
  useMessagesStore().showInfo(`Es wurden ${msg} erfasst!`, 1500)



  return divs;
}

export const pasteAnyFromClipboard = async (throwInsteadOfAlert = false): Promise<EntryNew | null> => {
  try {
    const text = await navigator.clipboard.readText();
    return parseTestForData(text, throwInsteadOfAlert);
  } catch (error) {
    console.error('Fehler beim Lesen der Zwischenablage:', error);
    if (!throwInsteadOfAlert) {
      useMessagesStore().add({ color: 'error', text: 'Fehler beim Lesen der Zwischenablage. Stellen Sie sicher, dass die Seite Zugriff auf die Zwischenablage hat.' })
    } else {
      throw error;
    }
  }
  return null;
};

export function parseTestForData(text: string, throwInsteadOfAlert = false): EntryNew | null {
  if (!text) return null;

  // Clear previous values
  const newEntry = getEmptyEntry();

  // Detect ISIN (2 letters followed by 10 digits)
  const isinMatch = text.match(/([A-Z]{2}[0-9A-Z]{10})/);
  if (isinMatch) {
    newEntry.isin = isinMatch[0].toUpperCase();
  }

  // Detect dates (German format dd.mm.yyyy or US format yyyy-mm-dd)
  const dateMatches = [
    ...text.matchAll(/(\d{2})\.(\d{2})\.(\d{4})/g), // German format long
    ...text.matchAll(/(\d{2})\.(\d{2})\.(\d{2})/g), // German format short
    ...text.matchAll(/(\d{4})-(\d{2})-(\d{2})/g)    // US format
  ];

  const dates = dateMatches.map(match => {
    if (match[0].includes('.') && match[0].length === 10) { // German format dd.mm.yyyy
      return new Date(`${match[3]}-${match[2]}-${match[1]}`);
    } else if (match[0].includes('.') && match[0].length === 8) { // German format dd.mm.yy
      const century = new Date().getFullYear().toString().slice(0, 2)
      return new Date(`${century}${match[3]}-${match[2]}-${match[1]}`);
    } else { // US format yyyy-mm-dd
      return new Date(match[0]);
    }
  }).filter(date => !isNaN(date.getTime()));

  if (dates.length > 0) {
    // Find the latest date
    const latestDate = dates.reduce((latest, current) =>
      current > latest ? current : latest
    );
    newEntry.date = latestDate;
  }


  newEntry.amount = findAmountInText(text) ?? 0;

  const errorMessages: string[] = [];

  if (!newEntry.isin) {
    errorMessages.push('ISIN wurde nicht erkannt.');
  }
  if (!newEntry.amount) {
    errorMessages.push('Betrag wurde nicht erkannt.');
  }
  if (!newEntry.date) {
    errorMessages.push('Datum wurde nicht erkannt.');
  }

  if (errorMessages.length > 0) {
    errorMessages.push(`für Zeile: ${text}`);
    if (!throwInsteadOfAlert) {
      useMessagesStore().add({ color: 'error', text: errorMessages.join('\n') })
      return null;
    } else {
      throw new Error(errorMessages.join('\n'));
    }
  }

  return newEntry;
}