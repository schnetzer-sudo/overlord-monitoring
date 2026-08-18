package de.kraftwerkone.overlord.monitor.payload;

/**
 * Die Anzeige eines Artefakts — <b>JSON, niemals ein Bytestrom mit ratbarem Typ</b>.
 *
 * <p>Das ist der Unterschied zum Altsystem, das fuer Anzeige <i>und</i> Download denselben {@code
 * application/octet-stream} liefert (Q4). Ein Bytestrom, dessen Typ der Browser errät, ist genau
 * der Weg, auf dem fremder Inhalt zu ausgefuehrtem Inhalt wird. Hier ist der Inhalt ein
 * JSON-Zeichenkettenfeld; die Oberflaeche rendert ihn als Textknoten.
 *
 * @param artefaktId die Kennung, wie sie im Pfad steht
 * @param name {@code MessagePropertyName}, unveraendert
 * @param art {@link Artefaktart#NUTZDATEN} oder {@link Artefaktart#PROTOKOLL}
 * @param zustand einer der fuenf Zustaende. Bei allem ausser {@link Artefaktzustand#ANZEIGBAR} ist
 *     {@link #text()} leer — und die Oberflaeche zeigt <b>keinen leeren Kasten</b>, sondern den
 *     benannten Text zu diesem Zustand
 * @param text der Inhalt, nach {@code ISO-8859-1} dekodiert. Leer, wenn {@code zustand} nicht
 *     {@link Artefaktzustand#ANZEIGBAR} ist
 * @param groesseBytes die Groesse der <b>vollstaendigen</b> entpackten Datei in Bytes — nicht die
 *     des angezeigten Ausschnitts. Nur so ist ablesbar, wie viel fehlt
 * @param gekuerzt ob die Anzeige an der Laengengrenze gekappt wurde. Bei einem gemessenen Maximum
 *     von 609.995 Byte (M60) eine Schutzmassnahme, kein Regelfall
 * @param beschnitten ob der Markenbeschnitt gegriffen hat. Wahr nur bei Protokollen und nur fuer
 *     {@code MANDANT}
 * @param kodierung die verwendete Kodierung. Fest {@code ISO-8859-1} — gemessen, nicht geraten: 8
 *     von 8 Protokollen und 9 von 16 Nutzdateien sind <b>kein</b> gueltiges UTF-8 (M61), und das
 *     Altsystem dekodiert an derselben Stelle hart mit {@code ISO-8859-1} (Q4)
 * @param zipEintraege wie viele Eintraege das Archiv trug. In 693 geholten Dateien immer {@code 1};
 *     alles darueber ist ein bisher nie beobachteter Fall und wird <b>vermerkt</b> statt
 *     stillschweigend verworfen wie im Altsystem ({@code :801}–{@code :802})
 */
public record AnzeigeResponse(
    String artefaktId,
    String name,
    Artefaktart art,
    Artefaktzustand zustand,
    String text,
    long groesseBytes,
    boolean gekuerzt,
    boolean beschnitten,
    String kodierung,
    int zipEintraege) {}
