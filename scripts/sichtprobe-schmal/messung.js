/**
 * Der Messkern der Sichtprobe am schmalen Fenster — läuft IN der Seite.
 *
 * Eine Funktion, zwei Aufrufer: `sichtprobe.mjs` schickt sie über das
 * DevTools-Protokoll in ein kopfloses Chrome (`(fn)(document, window)`), und
 * `rahmen.js` ruft sie im angemeldeten Chrome der Erweiterung für das Dokument
 * eines gleichherkunftigen `<iframe>` auf. Deshalb kommen `document` und
 * `window` als Parameter und nichts wird aus dem globalen Geltungsbereich
 * gelesen — im iframe-Fall wären das die falschen.
 *
 * Gemessen wird im DOM, nicht am Bild: die sieben Messgrößen des Auftrags
 * (siehe `sichtprobe.mjs`, Kopfkommentar). Keine Zeit als Kriterium (T1).
 */
function sichtprobeMessen(document, window) {
  const { innerWidth, innerHeight, devicePixelRatio, location, Element } = window;
  const getComputedStyle = window.getComputedStyle.bind(window);
  const matchMedia = window.matchMedia.bind(window);
  const rund = (n) => Math.round(n * 100) / 100;
  const rect = (el) => {
    const b = el.getBoundingClientRect();
    return { x: rund(b.left), y: rund(b.top), w: rund(b.width), h: rund(b.height), rechts: rund(b.right) };
  };
  const text = (el) => (el.textContent || "").replace(/\s+/g, " ").trim();
  const kurz = (s, n = 40) => (s.length > n ? s.slice(0, n - 1) + "…" : s);
  const sichtbar = (el) => {
    if (!(el instanceof Element)) return false;
    const cs = getComputedStyle(el);
    if (cs.display === "none" || cs.visibility === "hidden") return false;
    const r = el.getClientRects();
    return r.length > 0 && (r[0].width > 0 || r[0].height > 0);
  };
  const pfad = (el) => {
    const teile = [];
    let e = el;
    while (e && e !== document.body && teile.length < 4) {
      let t = e.tagName.toLowerCase();
      const slot = e.getAttribute("data-slot");
      const rolle = e.getAttribute("role");
      if (slot) t += "[" + slot + "]";
      else if (e.id) t += "#" + e.id;
      else if (rolle) t += "[role=" + rolle + "]";
      teile.unshift(t);
      e = e.parentElement;
    }
    return teile.join(">");
  };
  const name = (el) => {
    const a = el.getAttribute("aria-label") || el.getAttribute("title");
    if (a) return a;
    const t = text(el);
    if (t) return t;
    const labelledBy = el.getAttribute("aria-labelledby");
    if (labelledBy) {
      const l = document.getElementById(labelledBy);
      if (l) return text(l);
    }
    if (el.id) {
      const l = document.querySelector('label[for="' + el.id + '"]');
      if (l) return text(l);
    }
    const svg = el.querySelector("svg");
    if (svg) return "(Symbol " + [...svg.classList].filter((c) => c.startsWith("lucide-")).join(" ") + ")";
    return el.tagName.toLowerCase();
  };
  // Je Elternelement einmal gerechnet: Im Katalog stehen 733 Zeilen mit rund
  // 15.700 Elementen, und die Hülle jeder Zelle ist dieselbe. Ohne den
  // Zwischenspeicher dauerte eine Breite dort über eine Minute (15.09.2026).
  const huellen = new Map();
  const inScrollhuelle = (el) => {
    const e = el.parentElement;
    if (!e || e === document.body) return null;
    if (huellen.has(e)) return huellen.get(e);
    const ox = getComputedStyle(e).overflowX;
    const h = ox === "auto" || ox === "scroll" || ox === "hidden" ? pfad(e) : inScrollhuelle(e);
    huellen.set(e, h);
    return h;
  };

  const doc = document.documentElement;
  const alle = Array.from(document.body.querySelectorAll("*"));
  const sichtbare = alle.filter(sichtbar);
  const E = {
    url: location.pathname + location.search,
    innerWidth,
    innerHeight,
    dpr: devicePixelRatio,
    pointerCoarse: matchMedia("(pointer: coarse)").matches,
    hoverHover: matchMedia("(hover: hover)").matches,
    dichte: doc.dataset.dichte ?? null,
    thema: doc.dataset.thema ?? null,
    dokument: {
      scrollWidth: doc.scrollWidth,
      clientWidth: doc.clientWidth,
      bodyScrollWidth: document.body.scrollWidth,
      scrollHeight: doc.scrollHeight,
      clientHeight: doc.clientHeight,
      scrollX: rund(window.scrollX),
      scrollY: rund(window.scrollY),
    },
    elemente: alle.length,
    sichtbareElemente: sichtbare.length,
  };

  // 1. Waagerechter Überlauf — und wer über den rechten Rand hinaussteht.
  const ueber = [];
  for (const el of sichtbare) {
    const b = el.getBoundingClientRect();
    if (b.right > innerWidth + 0.5 && b.width > 0) {
      ueber.push({ el, rechts: rund(b.right), breite: rund(b.width), inHuelle: inScrollhuelle(el) });
    }
  }
  ueber.sort((a, b) => b.rechts - a.rechts);
  E.ueberlauf = {
    waagerecht: doc.scrollWidth - innerWidth,
    ueberstehendeGesamt: ueber.length,
    ohneHuelle: ueber.filter((u) => u.inHuelle === null).length,
    // Pfad und Text nur für die Beispiele — nicht für Tausende Zellen.
    beispiele: ueber.slice(0, 6).map((u) => ({ pfad: pfad(u.el), rechts: u.rechts, breite: u.breite, text: kurz(text(u.el), 30), inHuelle: u.inHuelle })),
  };

  // 2.–4. Tabellen: Kopfzellen als Kasten UND als Textkasten, 0-px-Spalten, gekürzte Zellen.
  const zelleInfo = (z) => {
    const b = rect(z);
    const range = document.createRange();
    range.selectNodeContents(z);
    const tb = range.getBoundingClientRect();
    const traeger = [z, ...z.querySelectorAll("*")].find(
      (e) => e.scrollWidth > e.clientWidth + 0.5 && ["hidden", "clip", "auto", "scroll"].includes(getComputedStyle(e).overflowX),
    );
    return {
      text: kurz(text(z), 40),
      zelle: b,
      textkasten: { x: rund(tb.left), w: rund(tb.width), rechts: rund(tb.right) },
      scrollWidth: z.scrollWidth,
      clientWidth: z.clientWidth,
      abgeschnitten: z.scrollWidth > z.clientWidth + 0.5 || Boolean(traeger),
      title:
        z.getAttribute("title") ||
        (traeger && (traeger.getAttribute("title") || traeger.closest("[title]")?.getAttribute("title"))) ||
        z.querySelector("[title]")?.getAttribute("title") ||
        null,
    };
  };
  E.tabellen = Array.from(document.querySelectorAll("table"))
    .filter(sichtbar)
    .map((t) => {
      const huelle = t.closest('[data-slot="table-container"]') || t.parentElement;
      const kopf = Array.from(t.querySelectorAll("thead th, thead td")).filter(sichtbar);
      const kopfzellen = kopf.map(zelleInfo);
      const schnitte = [];
      for (let a = 0; a < kopfzellen.length; a++) {
        for (let b = a + 1; b < kopfzellen.length; b++) {
          const A = kopfzellen[a];
          const B = kopfzellen[b];
          const zs = Math.min(A.zelle.rechts, B.zelle.rechts) - Math.max(A.zelle.x, B.zelle.x);
          const ts = Math.min(A.textkasten.rechts, B.textkasten.rechts) - Math.max(A.textkasten.x, B.textkasten.x);
          if ((zs > 0.5 && A.zelle.w > 0 && B.zelle.w > 0) || (ts > 0.5 && A.textkasten.w > 0 && B.textkasten.w > 0)) {
            schnitte.push({ a: A.text, b: B.text, zellenSchnitt: rund(Math.max(0, zs)), textSchnitt: rund(Math.max(0, ts)) });
          }
        }
      }
      const nullbreit = kopfzellen
        .filter((z) => z.zelle.w < 1 && z.text !== "")
        .map((z) => ({ text: z.text, breite: z.zelle.w, textbreite: z.textkasten.w, textVon: z.textkasten.x }));
      const zeilen = t.querySelectorAll("tbody tr");
      const erste = zeilen[0];
      const koerper = erste ? Array.from(erste.children).filter(sichtbar).map(zelleInfo) : [];
      let abg = 0;
      let abgTitle = 0;
      const beispiele = [];
      for (const z of t.querySelectorAll("th, td")) {
        if (!sichtbar(z)) continue;
        const i = zelleInfo(z);
        if (!i.abgeschnitten) continue;
        abg++;
        if (i.title) abgTitle++;
        if (beispiele.length < 5) beispiele.push({ text: i.text, title: Boolean(i.title), breite: i.zelle.w, scrollWidth: i.scrollWidth });
      }
      return {
        pfad: pfad(t),
        huelle: {
          breite: rund(huelle.getBoundingClientRect().width),
          scrollWidth: huelle.scrollWidth,
          clientWidth: huelle.clientWidth,
          overflowX: getComputedStyle(huelle).overflowX,
        },
        breite: rect(t).w,
        zeilen: zeilen.length,
        kopf: kopfzellen.map((z) => ({ text: z.text, x: z.zelle.x, w: z.zelle.w, textW: z.textkasten.w, textX: z.textkasten.x, abgeschnitten: z.abgeschnitten })),
        schnitte,
        nullbreit,
        ersteZeile: koerper.map((z) => ({ text: z.text, x: z.zelle.x, w: z.zelle.w, abgeschnitten: z.abgeschnitten, title: Boolean(z.title) })),
        abgeschnitten: { zellen: abg, mitTitle: abgTitle, ohneTitle: abg - abgTitle, beispiele },
      };
    });

  // 4b. Gekürzte Elemente außerhalb von Tabellen.
  const truncs = Array.from(document.querySelectorAll(".truncate, [class*='line-clamp']"))
    .filter(sichtbar)
    .filter((e) => e.scrollWidth > e.clientWidth + 0.5 && !e.closest("table"));
  E.gekuerztAusserhalb = {
    anzahl: truncs.length,
    beispiele: truncs.slice(0, 6).map((e) => ({ pfad: pfad(e), text: kurz(text(e), 30), title: Boolean(e.getAttribute("title") || e.closest("[title]")) })),
  };

  // 5. Scrollbereiche — angelegt (overflow-y auto/scroll) und tatsächlich scrollend.
  const senkrecht = [];
  const waagerecht = [];
  for (const el of sichtbare) {
    const cs = getComputedStyle(el);
    if (cs.overflowY === "auto" || cs.overflowY === "scroll") {
      senkrecht.push({ pfad: pfad(el), scrollHeight: el.scrollHeight, clientHeight: el.clientHeight, scrollt: el.scrollHeight > el.clientHeight + 1, istMain: el.tagName === "MAIN" });
    }
    if ((cs.overflowX === "auto" || cs.overflowX === "scroll") && el.scrollWidth > el.clientWidth + 1) {
      waagerecht.push({ pfad: pfad(el), scrollWidth: el.scrollWidth, clientWidth: el.clientWidth });
    }
  }
  const main = document.querySelector("main");
  E.scroller = {
    senkrechtAngelegt: senkrecht.length,
    senkrechtAktiv: senkrecht.filter((s) => s.scrollt).length,
    senkrecht,
    waagerecht,
    dokumentScrollt: doc.scrollHeight > doc.clientHeight + 1,
    main: main ? { scrollHeight: main.scrollHeight, clientHeight: main.clientHeight, clientWidth: main.clientWidth, overflowY: getComputedStyle(main).overflowY } : null,
  };

  // 6. Berührungsflächen — Layoutmaß, ohne Transformationen.
  const ZIELE =
    'a[href], button, input:not([type="hidden"]), select, textarea, summary, [role="button"], [role="link"], [role="menuitem"], [role="menuitemradio"], [role="menuitemcheckbox"], [role="switch"], [role="checkbox"], [role="radio"], [role="tab"], [role="option"], [role="treeitem"], [role="combobox"], [tabindex="0"]';
  const ziele = Array.from(document.querySelectorAll(ZIELE))
    .filter(sichtbar)
    .filter((el) => !el.closest('[aria-hidden="true"]') && !el.disabled);
  const gruppen = new Map();
  let klein = 0;
  let nurBreite = 0;
  let nurHoehe = 0;
  let beides = 0;
  for (const el of ziele) {
    const w = el.offsetWidth ?? rect(el).w;
    const h = el.offsetHeight ?? rect(el).h;
    if (w >= 44 && h >= 44) continue;
    klein++;
    if (w < 44 && h < 44) beides++;
    else if (w < 44) nurBreite++;
    else nurHoehe++;
    const schluessel = pfad(el) + "|" + w + "x" + h;
    const g = gruppen.get(schluessel);
    if (g) {
      g.anzahl++;
      continue;
    }
    gruppen.set(schluessel, {
      pfad: pfad(el),
      name: kurz(name(el), 36),
      w,
      h,
      anzahl: 1,
      inline: getComputedStyle(el).display.startsWith("inline"),
      inTabelle: Boolean(el.closest("table")),
    });
  }
  E.ziele = { gesamt: ziele.length, unter44: klein, nurBreite, nurHoehe, beides, gruppen: Array.from(gruppen.values()).sort((a, b) => a.h - b.h || a.w - b.w) };

  // 7. Kopfzeile — Zeilen über die top-Koordinate der Kinder, Mandantencode, Suchfeld.
  const header = document.querySelector("header");
  if (header) {
    const innen = header.firstElementChild;
    const kinder = Array.from(innen.children)
      .filter(sichtbar)
      .map((k) => ({ bereich: k.getAttribute("data-bereich"), text: kurz(text(k), 24), rect: rect(k), pfad: pfad(k) }));
    // Zwei Kinder stehen in einer Zeile, wenn sich ihre Höhen überlappen — nicht,
    // wenn ihre Oberkanten gleich sind: In der einzeiligen Kopfzeile ab `md`
    // sitzt der Produktname (Text) vier Pixel tiefer als die Schaltflächen
    // daneben, und eine Toleranz auf die Oberkante zählte dort zwei Zeilen
    // (gesehen am 15.09.2026 im Rahmen, 768 px; die Zahl unter 768 war davon
    // nicht betroffen).
    const zeilen = [];
    for (const k of kinder.sort((a, b) => a.rect.y - b.rect.y)) {
      const z = zeilen.find((z) => k.rect.y < z.unten && k.rect.y + k.rect.h > z.y);
      if (z) {
        z.elemente.push(k);
        z.unten = Math.max(z.unten, k.rect.y + k.rect.h);
      } else zeilen.push({ y: k.rect.y, unten: k.rect.y + k.rect.h, elemente: [k] });
    }
    const mandant = Array.from(header.querySelectorAll(".font-mono")).find((m) => !m.closest("[data-bereich='suche']"));
    const suche = header.querySelector("[data-bereich='suche']");
    const sr = suche ? rect(suche) : null;
    const menue = Array.from(header.querySelectorAll("button")).find((b) => b.querySelector("svg.lucide-menu"));
    const aside = document.querySelector("aside");
    E.kopfzeile = {
      hoehe: rect(header).h,
      anzahlZeilen: zeilen.length,
      zeilen: zeilen.map((z) => ({ y: rund(z.y), inhalte: z.elemente.map((e) => (e.bereich ? "[" + e.bereich + "]" : e.text || e.pfad)) })),
      mandantCode: mandant ? text(mandant) : null,
      mandantSichtbar: mandant ? sichtbar(mandant) && rect(mandant).w > 0 && rect(mandant).rechts <= innerWidth + 0.5 : false,
      mandantAbgeschnitten: mandant ? mandant.scrollWidth > mandant.clientWidth + 0.5 : null,
      suchfeld: sr
        ? { breite: sr.w, y: sr.y, eigeneZeile: (zeilen.find((z) => Math.abs(z.y - sr.y) < 4)?.elemente.length ?? 0) === 1, fuellt: Math.abs(sr.w - rect(innen).w) < 40 }
        : null,
      menueSchalter: menue ? { sichtbar: sichtbar(menue), w: menue.offsetWidth, h: menue.offsetHeight } : null,
      navSpalteSichtbar: aside ? sichtbar(aside) : false,
      schubladeOffen: Boolean(document.querySelector('[data-slot="sheet-content"][data-state="open"]')),
    };
  } else {
    E.kopfzeile = null;
  }

  return E;
}
