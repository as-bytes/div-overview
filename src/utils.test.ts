import { describe, expect, test } from "vitest";
import { findAmountInText, getNameForIsin } from "./utils";

describe('utils', () => {
  test('should parse text for number', () => {
    const text = '2024-05-29;02:00:00;Executed;"WWEK 28419086";"Brenntag";Cash;Distribution;DE000A1DAHH0;;;144,4199;0,00;;EUR';
    expect(findAmountInText(text)).toBe(144.4199);
  });

  test('get name for isin (shares)', async () => {
    const name = await getNameForIsin('NL0010273215');
    expect(name).toBe('ASML Holding NV');
  });

  test('get name for isin (etf)', async () => {
    const name = await getNameForIsin('IE00B0M62S72');
    expect(name).toBe('iShares PLC - iShares Euro Dividend UCITS ETF EUR');
  });
});
