package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein einzelner BAM-Wert, roh aus Abfrage (b) — <b>schon in der Datenbank gedeckelt</b>.
 *
 * <p>Die Deckelung sitzt im Statement ({@code ROW_NUMBER() OVER (PARTITION BY MessageBAMType …)})
 * und nicht in Java. Der Unterschied ist nicht kosmetisch: Ueber Fenster B stehen auf <b>einer</b>
 * Nachricht bis zu 9.296 Werte (M41), und ueber den ganzen Bestand ist das Maximum <b>unbekannt</b>
 * — M41 hat einen Monat gemessen. Eine Antwort, deren Groesse an einer ungemessenen Zahl haengt,
 * ist keine gedeckelte Antwort.
 *
 * <p><b>Der Wert wird nicht normalisiert.</b> Fuehrende Nullen und Randleerzeichen bleiben stehen,
 * wie sie im Bestand stehen (M43 misst sie, entscheidet aber nichts) — sie sind ein Thema der
 * <i>Suche</i> und nicht der Anzeige.
 *
 * @param typ {@code MessageBAM.MessageBAMType}
 * @param wert {@code MessageBAM.MessageBAMValue}, unveraendert
 */
public record BamWertZeile(short typ, String wert) {}
