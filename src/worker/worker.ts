import { Env, Hono } from "hono";
import { CountryTax } from '../models';

const app = new Hono<{ Bindings: Env }>();

/**
 * countryCode: https://www.isin.net/country-codes/
 * taxRate: Beschreibt den allgemeinen Quellensteuersatz im Land.
   creditable: Deutet an, dass dieser Anteil in Deutschland angerechnet werden kann.
   refundable: Beschreibt den erstattbaren Anteil. Alternativen.
 */
const countryTaxInfo: CountryTax = {
  DE: { name: 'Deutschland', taxRate: 30.5, creditable: 0, refundable: 0, flag: 'https://flagcdn.com/w40/de.png' },
  DK: { name: 'Dänemark', taxRate: 27, creditable: 15, refundable: 27 - 15, flag: 'https://flagcdn.com/w40/dk.png' },
  FI: { name: 'Finnland', taxRate: 35, creditable: 15, refundable: 20, flag: 'https://flagcdn.com/w40/fi.png' },
  FR: { name: 'Frankreich', taxRate: 30, creditable: 12.8, refundable: 17.2, flag: 'https://flagcdn.com/w40/fr.png' },
  IE: { name: 'Irland', taxRate: 25, creditable: 0, refundable: 0, flag: 'https://flagcdn.com/w40/ie.png' },
  IT: { name: 'Italien', taxRate: 26, creditable: 15, refundable: 11, flag: 'https://flagcdn.com/w40/it.png' },
  JE: { name: 'Jersey', taxRate: 0, creditable: 15, refundable: 0, flag: 'https://flagcdn.com/w40/je.png' },
  NL: { name: 'Niederlande', taxRate: 15, creditable: 15, refundable: 0, flag: 'https://flagcdn.com/w40/nl.png' },
  NO: { name: 'Norwegen', taxRate: 25, creditable: 15, refundable: 10, flag: 'https://flagcdn.com/w40/no.png' },
  AT: { name: 'Österreich', taxRate: 27.5, creditable: 15, refundable: 12.5, flag: 'https://flagcdn.com/w40/at.png' },
  CH: { name: 'Schweiz', taxRate: 35, creditable: 15, refundable: 20, flag: 'https://flagcdn.com/w40/ch.png' },
  SE: { name: 'Schweden', taxRate: 30, creditable: 15, refundable: 15, flag: 'https://flagcdn.com/w40/se.png' },
  ES: { name: 'Spanien', taxRate: 19, creditable: 15, refundable: 4, flag: 'https://flagcdn.com/w40/es.png' },
  TR: { name: 'Türkei', taxRate: 10, creditable: 10, refundable: 0, flag: 'https://flagcdn.com/w40/tr.png' },
  US: { name: 'USA', taxRate: 30, creditable: 15, refundable: 15, flag: 'https://flagcdn.com/w40/us.png' }
};

console.debug('starting hono');

app.get("/api/", (c) => c.json({ name: "Cloudflare" }));
app.get("/api/taxinfo/", (c) => c.json(countryTaxInfo));

app.get("/api/isin-by-openfigi/", async (c) => {
  const isin = c.req.query("isin");
  const apiToken = c.req.query("token");

  if (!isin) {
    return c.json({ error: "ISIN parameter fehlt" }, 400);
  }

  if (!apiToken) {
    return c.json({ error: "Token parameter fehlt" }, 400);
  }

  try {
    const response = await fetch("https://api.openfigi.com/v3/mapping", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-OPENFIGI-APIKEY": apiToken
      },
      body: JSON.stringify([{
        idType: "ID_ISIN",
        idValue: isin
      }]),
    });

    const result = await response.json() as Record<string, unknown> | unknown[];
    return c.json(result);
  } catch (err: unknown) {
    console.error(err);
    return c.json({ ...(err as object) }, 400);
  }
});


app.get("/api/isin-by-google/", async (c) => {
  const isin = c.req.query("isin");
  if (!isin) {
    return c.json({ error: "ISIN parameter fehlt" }, 400);
  }

  try {
    // const response = await fetch(`https://news.google.com/rss/search?q=${isin}&hl=de&gl=DE&ceid=DE:de`, {
    const response = await fetch(`https://search.brave.com/search?q=${isin}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/text",
      },
      redirect: "follow",
    });

    const result = await response.text();

    const allUrls = result.match(/https?:\/\/[^"]+/g)?.filter(_ => !_.includes("brave.com") && !_.includes("w3.org")) || [];

    const possibleNames = Array.from(new Set(allUrls)).flatMap((url) => {
      const splitted = url.replaceAll('-', ' ').replaceAll(' aktie', ' Aktie').split('/');
      console.debug(url);
      if (url.startsWith('chart.aspx') || url.endsWith('.png') || url.endsWith('.jpg')) {
        return [];
      }

      if (url.includes('https://www.deka-etf.de')) {
        return [splitted[splitted.length - 1]];
      }
      if (url.includes('www.finanzen.net')) {
        return [splitted[splitted.length - 1]];
      }
      if (url.includes('boerse.de')) {
        return [splitted[splitted.length - 2]];
      }
      if (url.includes('onvista.de') && !url.includes('onvista.de/api')) {
        return [splitted[splitted.length - 1].replace(` ${isin.toUpperCase()}`, '')];
      }
      return []
    })

    const data = Array.from(new Set(possibleNames.map(_ => _.trim())));
    console.debug('data', data)

    return c.json({ data });

  } catch (err: unknown) {
    console.error(err);
    return c.json({ ...(err as object) }, 400);
  }
});


export default app;
