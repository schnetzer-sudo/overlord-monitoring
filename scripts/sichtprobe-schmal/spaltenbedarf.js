/**
 * Der Spaltenbedarf — der zweite Messkern der Sichtprobe am schmalen Fenster,
 * für Teil 2 (Spaltenwahl nach dem Platz). Läuft IN der Seite, wie `messung.js`:
 * `document` und `window` kommen als Parameter, damit er im Rahmen des
 * Erweiterungs-Chrome und im kopflosen Chrome dasselbe misst.
 *
 * Er misst, was `messung.js` nicht misst: **wie breit eine Spalte sein muss,
 * damit ihr längster tatsächlicher Inhalt hineinpasst** (Auftrag Teil 2, 3.2;
 * Regel Q4 — gemessen, nicht geschätzt). Die heutige Breite der Spalte spielt
 * dafür keine Rolle: Jede Zelle wird in eine Sonde außerhalb der Tabelle
 * kopiert, mit den geerbten Schrifteigenschaften der Zelle, und dort gemessen.
 * Deshalb zählen auch die Zellen ausgeblendeter Spalten mit.
 *
 * Je Zelle:
 *   einzeilig  max-content — der ganze Inhalt in einer Zeile
 *   unteilbar  min-content ohne den Notumbruch in einem Wort (`overflow-wrap`
 *              auf `normal`): bei gekürzten Zellen (`nowrap`) dieselbe Zahl wie
 *              `einzeilig`, bei umbrechenden das längste Wort. `word-break`
 *              bleibt, wie die Zelle es setzt — eine Kennung mit `break-all`
 *              darf überall brechen, das ist ihre Bauform; `break-words` ist der
 *              Notumbruch, der am 15.09.2026 buchstabenweise umgebrochen hat
 *              (M176 §4.9), und genau den schließt die Messung aus
 *   plakette   die Statusplakette allein, ohne den Zusatz daneben
 *   innen      Innenabstand und Rahmen der Zelle
 *
 * Und je Spalte eine Rechnung über die **endliche** Menge ihrer Beschriftungen
 * (`etiketten`, aus `etiketten.mjs`): die Kopfbeschriftung in beiden Sprachen,
 * die acht Einordnungen der Plakette, die vier Kettenrollen, und für den
 * Zeitpunkt jede Ziffer an jeder Stelle des angezeigten Werts. Damit hängt die
 * Zahl nicht daran, welcher Status zufällig auf der Seite steht.
 *
 * Keine Zeit als Kriterium (T1). Keine Zelltexte im Ergebnis (G1) — nur Breiten,
 * Zeichenzahlen und Zeilennummern.
 */
function spaltenbedarfMessen(document, window, etiketten) {
  const cs = (el) => window.getComputedStyle(el);
  const rund = (n) => (n === null || n === undefined ? null : Math.round(n * 100) / 100);
  const px = (v) => parseFloat(v) || 0;
  const wurzel = px(cs(document.documentElement).fontSize);
  const sprache = document.documentElement.lang === "en" ? "en" : "de";

  const regel = document.createElement("style");
  regel.textContent =
    "[data-sichtprobe-sonde][data-ganz], [data-sichtprobe-sonde][data-ganz] * { overflow-wrap: normal !important; }";
  document.head.appendChild(regel);
  const sonde = document.createElement("div");
  sonde.setAttribute("data-sichtprobe-sonde", "");
  sonde.style.cssText = "position:absolute;left:-100000px;top:0;visibility:hidden;contain:layout style;";
  document.body.appendChild(sonde);

  const ERBEN = [
    "fontFamily", "fontSize", "fontWeight", "fontStyle", "fontStretch", "lineHeight", "letterSpacing",
    "wordSpacing", "fontFeatureSettings", "fontVariationSettings", "fontVariantNumeric", "fontKerning",
    "textTransform", "whiteSpace", "wordBreak", "overflowWrap", "hyphens",
  ];
  const erben = (von) => {
    const s = cs(von);
    for (const p of ERBEN) sonde.style[p] = s[p];
  };

  /** Eine Zelle in der Sonde: einzeilig, unteilbar, die Plakette allein. */
  function zelleMessen(zelle) {
    const s = cs(zelle);
    erben(zelle);
    sonde.innerHTML = zelle.innerHTML;
    sonde.removeAttribute("data-ganz");
    sonde.style.width = "max-content";
    const einzeilig = sonde.getBoundingClientRect().width;
    const badge = sonde.querySelector('[data-slot="badge"]');
    const plakette = badge ? badge.getBoundingClientRect().width : null;
    sonde.setAttribute("data-ganz", "");
    sonde.style.width = "min-content";
    const unteilbar = sonde.getBoundingClientRect().width;
    sonde.removeAttribute("data-ganz");
    const innen = px(s.paddingLeft) + px(s.paddingRight) + px(s.borderLeftWidth) + px(s.borderRightWidth);
    return { einzeilig, unteilbar, plakette, innen };
  }

  /** Ein Text in der Schrift eines Trägers, einzeilig. */
  function textMessen(text, traeger) {
    erben(traeger);
    sonde.removeAttribute("data-ganz");
    sonde.style.whiteSpace = "nowrap";
    sonde.textContent = text;
    sonde.style.width = "max-content";
    return sonde.getBoundingClientRect().width;
  }

  /** Die sichtbaren Textknoten — ohne `sr-only`, ohne SVG. */
  function sichtbareTexte(el) {
    const knoten = [];
    const gang = document.createTreeWalker(el, 4 /* NodeFilter.SHOW_TEXT */);
    for (let n = gang.nextNode(); n; n = gang.nextNode()) {
      if (!n.nodeValue.trim()) continue;
      const eltern = n.parentElement;
      if (!eltern || eltern.closest(".sr-only") || eltern.closest("svg")) continue;
      knoten.push(n);
    }
    return knoten;
  }
  const sichtbarerText = (el) => sichtbareTexte(el).map((n) => n.nodeValue).join("").replace(/\s+/g, " ").trim();
  const traegerVon = (el) => sichtbareTexte(el)[0]?.parentElement ?? el;

  const verteilung = (werte) => {
    if (werte.length === 0) return { n: 0, max: null, zeile: null, p50: null, p90: null, p99: null };
    const sortiert = werte.map((w) => w.wert).sort((a, b) => a - b);
    const q = (p) => sortiert[Math.min(sortiert.length - 1, Math.floor(p * (sortiert.length - 1)))];
    const groesster = werte.reduce((a, b) => (b.wert > a.wert ? b : a));
    return { n: werte.length, max: rund(groesster.wert), zeile: groesster.zeile, zeichen: groesster.zeichen, p50: rund(q(0.5)), p90: rund(q(0.9)), p99: rund(q(0.99)) };
  };

  const tabellen = [];
  for (const tabelle of document.querySelectorAll("table")) {
    const kopfzeile = tabelle.querySelector("thead tr");
    if (!kopfzeile) continue;
    const kopfzellen = Array.from(kopfzeile.children);
    const ganz = (el) => (el.textContent || "").replace(/\s+/g, " ").trim();

    // Welche Tabelle ist das? Über die Kopfbeschriftungen der aktiven Sprache.
    let name = null;
    let beschriftung = null;
    for (const [kandidat, spalten] of Object.entries(etiketten.tabellen)) {
      const passend = kopfzellen.map((th) => spalten.findIndex((s) => s[sprache] === ganz(th)));
      if (passend.every((i) => i >= 0)) {
        name = kandidat;
        beschriftung = passend.map((i) => spalten[i]);
        break;
      }
    }
    if (!name) {
      tabellen.push({ name: null, kopf: kopfzellen.map((th) => ganz(th).length) });
      continue;
    }

    const zeilen = Array.from(tabelle.querySelectorAll("tbody tr")).filter((tr) => tr.children.length === kopfzellen.length);
    const spalten = kopfzellen.map((th, index) => {
      const etikett = beschriftung[index];

      // Der Kopf: was um den Text herum steht (Sortierpfeil), plus die längere der beiden Beschriftungen.
      const kopfMessung = zelleMessen(th);
      const kopfTraeger = traegerVon(th);
      const kopfText = sichtbarerText(th);
      const kopfHuelle = kopfText ? kopfMessung.einzeilig - textMessen(kopfText, kopfTraeger) : kopfMessung.einzeilig;
      const kopfBedarf =
        etikett.art === "nurVorlesen" || !kopfText
          ? { de: kopfMessung.innen + kopfMessung.einzeilig, en: kopfMessung.innen + kopfMessung.einzeilig }
          : { de: kopfMessung.innen + kopfHuelle + textMessen(etikett.de, kopfTraeger), en: kopfMessung.innen + kopfHuelle + textMessen(etikett.en, kopfTraeger) };

      const einzeilig = [];
      const unteilbar = [];
      const plaketten = [];
      let innen = 0;
      zeilen.forEach((tr, zeile) => {
        const td = tr.children[index];
        const m = zelleMessen(td);
        const zeichen = (td.textContent || "").trim().length;
        innen = Math.max(innen, m.innen);
        einzeilig.push({ wert: m.innen + m.einzeilig, zeile, zeichen });
        unteilbar.push({ wert: m.innen + m.unteilbar, zeile, zeichen });
        if (m.plakette !== null) plaketten.push({ wert: m.innen + m.plakette, zeile, zeichen });
      });

      // Die Rechnungen über die endlichen Mengen.
      let rechnung = null;
      const ersteMitText = zeilen.map((tr) => tr.children[index]).find((td) => sichtbarerText(td) !== "");
      if (etikett.art === "status" && ersteMitText) {
        const badge = ersteMitText.querySelector('[data-slot="badge"]');
        if (badge) {
          const m = zelleMessen(ersteMitText);
          const huelle = m.plakette - textMessen(sichtbarerText(badge), traegerVon(badge));
          const je = {};
          for (const s of ["de", "en"]) je[s] = etiketten.status[s].map((t) => ({ laenge: t.length, breite: rund(m.innen + huelle + textMessen(t, traegerVon(badge))) }));
          rechnung = { art: "status", huelle: rund(huelle), je, max: Math.max(...je.de.map((x) => x.breite), ...je.en.map((x) => x.breite)) };
        }
      } else if (etikett.art === "kette" && ersteMitText) {
        const m = zelleMessen(ersteMitText);
        const traeger = traegerVon(ersteMitText);
        const huelle = m.einzeilig - textMessen(sichtbarerText(ersteMitText), traeger);
        const je = {};
        for (const s of ["de", "en"]) {
          const liste = [...etiketten.kette[s], etiketten.kette[s].join(" · ")];
          je[s] = liste.map((t) => ({ laenge: t.length, breite: rund(m.innen + huelle + textMessen(t, traeger)) }));
        }
        rechnung = { art: "kette", huelle: rund(huelle), je, max: Math.max(...je.de.map((x) => x.breite), ...je.en.map((x) => x.breite)) };
      } else if (etikett.art === "zeitpunkt" && ersteMitText) {
        const m = zelleMessen(ersteMitText);
        const traeger = traegerVon(ersteMitText);
        const wert = sichtbarerText(ersteMitText);
        const huelle = m.einzeilig - textMessen(wert, traeger);
        const ziffern = [];
        for (let d = 0; d <= 9; d++) ziffern.push({ ziffer: d, breite: rund(m.innen + huelle + textMessen(wert.replace(/\d/g, String(d)), traeger)) });
        const breiteste = ziffern.reduce((a, b) => (b.breite > a.breite ? b : a));
        rechnung = { art: "zeitpunkt", sprache, laenge: wert.length, huelle: rund(huelle), ziffern, max: breiteste.breite, ziffer: breiteste.ziffer };
      }

      // Die Wortrechnung über die festen Texte einer Zelle — auch die, die in den
      // Daten gerade nirgends vorkommen (`woerter` aus `etiketten.mjs`). In der
      // Schrift der Zelle; ganzer Text, wo sie nicht umbricht, sonst Wort für Wort.
      if (etikett.woerter && zeilen.length > 0) {
        const td = zeilen[0].children[index];
        const s = cs(td);
        const innenTd = px(s.paddingLeft) + px(s.paddingRight) + px(s.borderLeftWidth) + px(s.borderRightWidth);
        const umbricht = !s.whiteSpace.startsWith("nowrap") && s.whiteSpace !== "pre";
        const je = {};
        let groesste = 0;
        for (const spr of ["de", "en"]) {
          je[spr] = etikett.woerter[spr].map((text) => {
            // Umbruchgelegenheiten wie im Browser: das Leerzeichen und die Stelle
            // nach einem Bindestrich („EDI-" | „Betreuung").
            const stuecke = umbricht ? text.split(" ").flatMap((wort) => wort.split(/(?<=-)/)).filter(Boolean) : [text];
            const breite = innenTd + Math.max(...stuecke.map((wort) => textMessen(wort, td)));
            groesste = Math.max(groesste, breite);
            return { laenge: text.length, breite: rund(breite) };
          });
        }
        rechnung = { art: "woerter", umbricht, je, max: rund(groesste) };
      }

      return {
        index,
        schluessel: etikett.schluessel,
        art: etikett.art,
        sichtbar: cs(th).display !== "none",
        breiteHeute: rund(th.getBoundingClientRect().width),
        kopf: { innen: rund(kopfMessung.innen), huelle: rund(kopfHuelle), bedarf: { de: rund(kopfBedarf.de), en: rund(kopfBedarf.en) }, bedarfMax: rund(Math.max(kopfBedarf.de, kopfBedarf.en)) },
        zellen: { innen: rund(innen), einzeilig: verteilung(einzeilig), unteilbar: verteilung(unteilbar), plakette: verteilung(plaketten) },
        rechnung,
      };
    });

    tabellen.push({ name, zeilen: zeilen.length, breite: rund(tabelle.getBoundingClientRect().width), spalten });
  }

  sonde.remove();
  regel.remove();
  return { url: window.location.pathname, sprache, wurzel, dichte: document.documentElement.dataset.dichte ?? null, tabellen };
}
