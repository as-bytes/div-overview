import { QSt } from "./models";

export function calculateWithholdingTax(total: number, countryInfo: QSt | undefined): number {
    if (!countryInfo) return 0;
    return (total ?? 0) * countryInfo.taxRate / 100;
};

export function calculateCreditableTax(total: number, countryInfo: QSt | undefined): number {
    if (!countryInfo) return 0;
    return (total ?? 0) * countryInfo.creditable / 100;
};

export function calculateRefundableTax(total: number, countryInfo: QSt | undefined): number {
    if (!countryInfo) return 0;
    return (total ?? 0) * countryInfo.refundable / 100;
};
