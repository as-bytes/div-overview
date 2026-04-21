import jsPDF from "jspdf";
import { formatCurrency } from "./utils";
import { db } from "./db";
import { calculateCreditableTax, calculateRefundableTax, calculateWithholdingTax } from "./tax";
import { IsinOverview } from "./models";

const year = new Date().getFullYear();

export const exportToPDF = async (entry: IsinOverview) => {

  console.debug(entry);

  const countryInfo = await db.getQst(entry.isin);
  const isinData = await db.getIsin(entry.isin);

  const doc = new jsPDF();
  doc.setFont('times', 'normal');

  let y = 0;

  // Title
  doc.setFontSize(18);
  doc.text('Quellensteuer-Erstattung', 105, y += 20, { align: 'center' });

  // Entry details
  doc.setFontSize(12);

  if (isinData?.name) {
    doc.text(`ISIN: ${entry.isin} (${isinData.name.toUpperCase()})`, 20, y += 20);
  } else {
    doc.text(`ISIN: ${entry.isin}`, 20, y += 20);
  }

  doc.text(`Land: ${countryInfo?.name || 'Unbekannt'}`, 20, y += 10);

  [0, 1, 2, 3].forEach((key, idx) => {
    const yearKey = `year-${key}` as keyof Omit<IsinOverview, 'isin'>;
    const totalNum = entry[yearKey];
    const totalStr = formatCurrency(totalNum, true, true);


    doc.text(`Betrag in ${year - idx}: ${totalStr}`, 20, y += 15);
    doc.line(15, y - 10, 195, y - 10);

    doc.setFontSize(11);
    doc.setFont('times', 'bold')
    doc.text('Steuerdetails:', 20, y += 10);
    doc.setFont('times', 'normal')
    y += 5;

    doc.text(`- Quellensteuer (${countryInfo?.taxRate || 0}%): ${formatCurrency(calculateWithholdingTax(totalNum, countryInfo))}`, 20, y += 5);
    doc.text(`- Anrechenbar (${countryInfo?.creditable || 0}%): ${formatCurrency(calculateCreditableTax(totalNum, countryInfo))}`, 20, y += 5);
    doc.text(`- Erstattbar (${countryInfo?.refundable || 0}%): ${formatCurrency(calculateRefundableTax(totalNum, countryInfo))}`, 20, y += 5);
    doc.setFontSize(12);
  })

  doc.line(15, y + 10, 195, y + 10);

  // Footer
  doc.setFontSize(10);
  doc.setTextColor(100);
  doc.text(`Exportiert am: ${new Date().toLocaleDateString('de-DE')}`, 105, 280, { align: 'center' });

  doc.save(`Quellensteuer-${entry.isin}-${new Date().toISOString().slice(0, 10)}.pdf`);
};
