/**
 * Der Messrahmen für das angemeldete Chrome der Erweiterung — die zweite
 * Bauform der Sichtprobe, wenn kein kopfloses Chrome mit Sitzung zur
 * Verfügung steht (der Nutzer ist im Chrome der Erweiterung angemeldet und
 * tippt sein Passwort nicht in ein Skript).
 *
 * Wird zusammen mit `messung.js` in die **Oberseite** (`http://localhost:3000/`)
 * eingesetzt. Ein gleichherkunftiger `<iframe>` mit exakter Größe lädt die
 * Route; sein Dokument ist lesbar, die Sitzung ist die echte. Was gegenüber
 * `sichtprobe.mjs` fehlt: `Emulation.setDeviceMetricsOverride` (kein
 * `mobile: true`, kein `deviceScaleFactor: 2`), die Berührungsemulation und
 * das Bildschirmfoto mit Faktor 1. Was es ersetzt: die Rinne des klassischen
 * Scrollbalkens wird über `scrollbar-width: none` entfernt, wie am Handy, wo
 * der Balken über dem Inhalt liegt — die einzige Änderung am gemessenen
 * Dokument, und sie steht in der Doku.
 *
 * Bedienung aus dem `javascript_tool` der Erweiterung:
 *
 *   await __sp.laden("/nachrichten", 360, 740)   → { url, ruhe }
 *   await __sp.messen("nachrichtenliste", BREITEN) → kompakte Tabelle je Breite;
 *                                                    das Vollergebnis liegt in
 *                                                    __sp.S.voll[kennung][breite]
 *   await __sp.zeileOeffnen(i)                    → messageId aus der URL des Rahmens
 *   __sp.voll("nachrichtenliste", 360)             → das vollständige Messobjekt
 */
window.__sp = (() => {
  const S = { f: null, voll: {}, kompakt: {} };
  const schlafen = (ms) => new Promise((ok) => setTimeout(ok, ms));

  function rahmen(w, h) {
    if (!S.f) {
      S.f = document.createElement("iframe");
      S.f.id = "sichtprobe-rahmen";
      document.body.appendChild(S.f);
    }
    S.f.style.cssText = `position:fixed;left:0;top:0;width:${w}px;height:${h}px;border:0;z-index:2147483647;background:#fff;visibility:visible`;
    return S.f;
  }

  function doc() {
    return S.f?.contentDocument ?? null;
  }
  function win() {
    return S.f?.contentWindow ?? null;
  }

  /** Scrollbalken ohne Rinne — wie am Handy. Die eine Änderung am Dokument. */
  function ohneRinne() {
    const d = doc();
    if (!d || d.getElementById("sichtprobe-rinne")) return;
    const st = d.createElement("style");
    st.id = "sichtprobe-rinne";
    st.textContent = "*, html, body { scrollbar-width: none !important; }";
    d.head.appendChild(st);
  }

  /** Ruhe: kein Skelett, nichts aria-busy, readyState complete, DOM und Netzwerkeinträge dreimal in Folge unverändert. */
  async function ruhe(timeoutMs = 40000) {
    const start = Date.now();
    let treffer = 0;
    let vorher = null;
    let letzter = null;
    while (Date.now() - start < timeoutMs) {
      const d = doc();
      const w = win();
      if (!d || !w || !d.body) {
        await schlafen(250);
        continue;
      }
      letzter = {
        skelette: d.querySelectorAll('[data-slot="skeleton"]').length,
        busy: d.querySelectorAll('[aria-busy="true"]').length,
        ready: d.readyState,
        ressourcen: w.performance.getEntriesByType("resource").length,
        knoten: d.body.getElementsByTagName("*").length,
      };
      const still = letzter.skelette === 0 && letzter.busy === 0 && letzter.ready === "complete" && vorher && vorher.ressourcen === letzter.ressourcen && vorher.knoten === letzter.knoten;
      treffer = still ? treffer + 1 : 0;
      vorher = letzter;
      if (treffer >= 3) return { erreicht: true, dauerMs: Date.now() - start, ressourcen: letzter.ressourcen };
      await schlafen(500);
    }
    return { erreicht: false, dauerMs: Date.now() - start, letzter };
  }

  async function laden(pfad, w = 360, h = 740) {
    if (S.f) {
      S.f.remove();
      S.f = null;
    }
    const f = rahmen(w, h);
    const geladen = new Promise((ok) => f.addEventListener("load", ok, { once: true }));
    f.src = pfad;
    await Promise.race([geladen, schlafen(30000)]);
    ohneRinne();
    const r = await ruhe();
    return { url: win()?.location.pathname + win()?.location.search, ruhe: r, hidden: document.hidden };
  }

  function kompakt(m) {
    return {
      b: m.innerWidth,
      url: m.url,
      ueberlauf: m.ueberlauf.waagerecht,
      ueberstehend: m.ueberlauf.ohneHuelle,
      docScrollt: m.scroller.dokumentScrollt,
      scroller: m.scroller.senkrecht.map((s) => `${s.pfad}${s.scrollt ? "*" : ""}`),
      mainW: m.scroller.main?.clientWidth ?? null,
      kopf: m.kopfzeile
        ? { z: m.kopfzeile.anzahlZeilen, h: m.kopfzeile.hoehe, mandant: m.kopfzeile.mandantCode, sichtbar: m.kopfzeile.mandantSichtbar, suche: m.kopfzeile.suchfeld ? (m.kopfzeile.suchfeld.eigeneZeile ? "eigen" : "geteilt") : null, nav: m.kopfzeile.navSpalteSichtbar, zeilen: m.kopfzeile.zeilen.map((z) => z.inhalte.join("|")) }
        : null,
      tabellen: m.tabellen.map((t) => ({
        huelle: t.huelle.breite,
        hscroll: t.huelle.scrollWidth,
        tabelle: t.breite,
        zeilen: t.zeilen,
        kopf: t.kopf.map((k) => `${k.text}:${k.w}${k.abgeschnitten ? "✂" : ""}(${k.textW})`),
        schnitte: t.schnitte.map((s) => `${s.a}/${s.b} z${s.zellenSchnitt} t${s.textSchnitt}`),
        null: t.nullbreit.map((n) => `${n.text} ${n.textbreite}px@${n.textVon}`),
        abg: `${t.abgeschnitten.zellen}/${t.abgeschnitten.ohneTitle}`,
      })),
      gekuerzt: m.gekuerztAusserhalb.anzahl,
      ziele: `${m.ziele.unter44}/${m.ziele.gesamt}`,
      zieleGruppen: m.ziele.gruppen.slice(0, 10).map((g) => `${g.name} ${g.w}x${g.h}${g.anzahl > 1 ? "×" + g.anzahl : ""}`),
      coarse: m.pointerCoarse,
    };
  }

  async function messen(kennung, breiten, wartezeit = 1500) {
    S.voll[kennung] = {};
    S.kompakt[kennung] = [];
    for (const [w, h] of breiten) {
      rahmen(w, h);
      await schlafen(wartezeit);
      await win().document.fonts.ready;
      await ruhe(4000);
      const m = sichtprobeMessen(doc(), win());
      m.masse = { w, h };
      S.voll[kennung][w] = m;
      S.kompakt[kennung].push(kompakt(m));
    }
    return S.kompakt[kennung];
  }

  async function zeileOeffnen(i = 0) {
    const d = doc();
    const zeile = d.querySelectorAll("table tbody tr")[i];
    if (!zeile) return { fehler: "keine Zeile " + i, zeilen: d.querySelectorAll("table tbody tr").length };
    zeile.querySelector("td").click();
    const start = Date.now();
    while (Date.now() - start < 20000) {
      const id = new URLSearchParams(win().location.search).get("nachricht");
      if (id) {
        await ruhe(20000);
        return { messageId: id };
      }
      await schlafen(250);
    }
    return { fehler: "Panel nicht geöffnet" };
  }

  async function api(pfad) {
    const r = await fetch(pfad);
    return r.ok ? r.json() : { status: r.status };
  }

  function voll(kennung, breite) {
    return S.voll[kennung]?.[breite] ?? null;
  }

  /**
   * Das Vollergebnis an den Empfänger (`empfaenger.mjs`, Port 3999) — das
   * `javascript_tool` schneidet seine Ausgabe nach rund tausend Zeichen ab,
   * ein Vollergebnis über fünf Breiten passt da nicht durch.
   */
  async function senden(kennung) {
    const r = await fetch("http://localhost:3999/" + encodeURIComponent(kennung), {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ kennung, zeit: new Date().toISOString(), daten: window.__daten, breiten: window.__BREITEN, voll: S.voll[kennung] }),
    });
    return r.status + ":" + (await r.text());
  }

  /** Eine Zeile je Breite, damit die Ausgabe des Werkzeugs nicht abgeschnitten wird. */
  function kurz(kennung) {
    return S.kompakt[kennung]
      .map((k) =>
        [
          k.b,
          "ü" + k.ueberlauf,
          k.docScrollt ? "D!" : "d",
          "sc" + k.scroller.filter((s) => s.endsWith("*")).length + "/" + k.scroller.length,
          k.kopf ? "k" + k.kopf.z + (k.kopf.sichtbar ? "✓" : "✗") + (k.kopf.suche === "eigen" ? "s" : k.kopf.suche === null ? "" : "-") + (k.kopf.nav ? "N" : "") : "k-",
          "t" + k.tabellen.map((t) => Math.round(t.tabelle) + ":" + t.schnitte.length + "/" + t.null.length + "/" + t.abg).join(","),
          "z" + k.ziele,
        ].join(" "),
      )
      .join("\n");
  }

  /** Laden, über alle Breiten messen, senden, kurz berichten — eine Route je Aufruf. */
  async function route(kennung, pfad) {
    const lade = await laden(pfad, 360, 740);
    await messen(kennung, window.__BREITEN);
    const s = await senden(kennung);
    return `${kennung} ${lade.url} ruhe ${lade.ruhe.erreicht ? lade.ruhe.dauerMs + "ms" : "NEIN"} gesendet ${s}\n` + kurz(kennung);
  }

  function schliessen() {
    S.f?.remove();
    S.f = null;
  }

  return { S, laden, messen, ruhe, zeileOeffnen, api, voll, senden, kurz, route, schliessen, doc, win };
})();
window.__BREITEN = [[360, 740], [390, 844], [430, 932], [744, 1133], [768, 1024]];
window.__daten = window.__daten ?? {};
"Messrahmen bereit";
