package de.kraftwerkone.overlord.monitor.bam;

import java.util.List;

/**
 * Eine Typgruppe der Antwort — <b>die wahre Zahl neben den gezeigten Werten</b>.
 *
 * @param typ die Typnummer. Sie steht in der Antwort, obwohl die Oberflaeche sie nicht zeigt: Sie
 *     ist zusammen mit dem Wert der Schluessel der Liste, und ohne sie waere er der Wert allein —
 *     bei 4,17 Prozent der Paare stuende derselbe Wert dann unter zwei Schluesseln (M37)
 * @param bezeichnung {@code MessageBAMType.MessageBAMTypeDescription}, unveraendert. <b>Nie {@code
 *     null}</b>: Fehlt die Zeile in {@code MessageBAMType}, steht hier die Typnummer — sichtbar
 *     unfertig statt lautlos leer.
 *     <p><b>Die Endungen {@code _K_SAP}, {@code _L_SAP} und {@code _FORS} bleiben stehen.</b> Sie
 *     sehen nach internem Beiwerk aus, tragen aber mutmasslich Bedeutung; ob die Beschreibungen
 *     ohne sie noch eindeutig sind, ist <b>nicht gemessen</b>. Eine Kuerzungsregel waere nach Regel
 *     Q4 geraten ({@code docs/bam-werte.md}, offener Punkt 1)
 * @param gesamt die <b>wahre</b> Zahl der Werte dieses Typs auf dieser Nachricht — aus Abfrage (a)
 *     und damit unabhaengig von der Deckelung
 * @param werte hoechstens {@link BamRepository#WERTE_JE_GRUPPE} Werte, aufsteigend nach {@code
 *     MessageBAMValue}. <b>Nie {@code null}</b>
 * @param weitereVorhanden {@code gesamt > werte.size()}. Die Oberflaeche schreibt daraus die
 *     ehrliche Restangabe — <b>kein „mehr laden"</b>: Das braeuchte einen Cursor und kommt erst,
 *     wenn jemand es braucht
 */
public record BamGruppeResponse(
    int typ, String bezeichnung, int gesamt, List<String> werte, boolean weitereVorhanden) {}
