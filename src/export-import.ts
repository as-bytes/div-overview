import { db } from "./db";
import { formatDateISO, readUploadedFile } from "./utils";
import { Dividend, Isin, QSt } from "./models";
import { useMessagesStore } from "./stores";

type ModelV1Export = { divs: Array<Dividend & { date: string }>, qsts: QSt[], isins: Isin[] };
type ModelV1Import = { divs: Array<Dividend>, qsts: QSt[], isins: Isin[] };

export const exportDataJson = async () => {
  try {
    const divs = (await db.getDivsAll()).map(_ => ({ ..._, date: formatDateISO(_.date) })) as ModelV1Export['divs'];
    const qsts = await db.getQstsAll();
    const isins = await db.getIsinsAll();

    const data: ModelV1Export = { divs, qsts, isins };
    const exportJson = JSON.stringify(data, null, 2);

    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(exportJson);

    const exportFileDefaultName = 'quellensteuer-daten.json';

    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  } catch (error) {
    console.error('Fehler beim Exportieren der Daten:', error);
  }
};

export const importDataJson = async (event: Event): Promise<ModelV1Import | null> => {
  if (!event.target) return null;
  const input: HTMLInputElement = (event.target as HTMLInputElement);
  const file = input.files ? input.files[0] : null;
  if (!file) return null;

  try {
    const fileContents = await readUploadedFile(file);
    const data = JSON.parse(`${fileContents}`);

    if (data.divs && data.qsts) {
      return await importDataJsonV1(data);
    } else {
      throw new Error('Ungültiges Datenformat');
    }
  } catch (error) {
    useMessagesStore().showError('Fehler beim Importieren der Daten: ', error as Error);
  } finally {
    input.value = '';
  }

  return null;
};

export const importDataJsonV1 = async (data: ModelV1Export): Promise<ModelV1Import | null> => {
  try {
    useMessagesStore().showInfo('Daten erfolgreich importiert!');
    const divs = await db.bulkAddDivs(data.divs.map(_ => ({ ..._, date: new Date(_.date) })));
    const qsts = await db.bulkAddQsts(data.qsts);
    const isins = await db.bulkAddIsins(data.isins);
    return { divs, qsts, isins }
  } catch (error) {
    useMessagesStore().showError('Fehler beim Importieren der Daten (v2): ', error as Error);
  }

  return null;
};