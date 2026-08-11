package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.Kettenrollen;
import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Ein Glied der Kette, roh aus dem Quellschema — vor Einordnung und Zeitumrechnung.
 *
 * <p><b>Genug fuer eine Zeile, nicht mehr.</b> Keine Schrittfolge, keine Eigenschaften, keine
 * BAM-Werte: Wer ein Glied ansehen will, oeffnet es, und dafuer gibt es den Detail-Endpunkt. Die
 * vier Verkettungsspalten kommen mit, weil sie zweierlei tragen — die Rollen des Glieds <b>und</b>
 * den naechsten Schritt des Aufstiegs.
 *
 * @param messageId {@code Message.MessageID}
 * @param status der Rohwert {@code MessageStatus}. Freier Text, kein Aufzaehlungstyp
 * @param zeitpunkt {@code MessageLastUpdate} — Zeitpunkt der letzten Aenderung, kein Anlagedatum
 *     (Regel Q2). Zugleich die erste Haelfte des Sortierschluessels der Abwaertsglieder
 * @param sosName {@code SOS.SOSName}, der Anzeigename des Ablaufs. Nullbar — der Join ist ein
 *     {@code LEFT JOIN}, damit ein fehlender Anzeigename nicht darueber entscheidet, ob ein Glied
 *     ueberhaupt erscheint
 * @param source {@code Message.Source} als {@link Boolean} — der Codegen bildet {@code bit(1)} per
 *     {@code forcedType} ab ({@code datenzugriff.md} §9). Die {@code + 0}-Falle aus {@code
 *     datenmodell.md} §5.8 gilt fuer Erhebungen von Hand, nicht hier
 * @param target {@code Message.Target}, ebenso {@link Boolean}
 * @param sourceMessageId {@code Message.SourceMessageID} — der Elternteil, falls dieses Glied ein
 *     Split-Kind ist. Der Aufstieg folgt ihm zuerst
 * @param targetMessageId {@code Message.TargetMessageID} — das Merge-Ergebnis, falls dieses Glied
 *     ein Eingang ist. Nie zugleich mit {@code sourceMessageId} belegt (M28‑1), weshalb der
 *     Aufstieg je Glied genau <b>ein</b> Glied darueber hat
 */
record Kettengliedzeile(
    String messageId,
    String status,
    LocalDateTime zeitpunkt,
    String sosName,
    Boolean source,
    Boolean target,
    String sourceMessageId,
    String targetMessageId) {

  /** Die Rollen dieses Glieds — berechnet in {@code common}, nicht hier nachgebaut. */
  EnumSet<Kettenrolle> rollen() {
    return Kettenrollen.aus(source, sourceMessageId, targetMessageId, target);
  }

  /**
   * Die Kennung des <b>einen</b> Glieds darueber, oder {@code null} am oberen Ende der Kette.
   *
   * <p><b>„Aufwaerts" ist der Mechanismus und nicht die Bedeutung.</b> Ueber {@code
   * SourceMessageID} fuehrt der Schritt zur Wurzel, also gegen den Datenfluss; ueber {@code
   * TargetMessageID} fuehrt er zum Merge-<i>Ergebnis</i>, also <b>mit</b> dem Fluss. Ein Name, der
   * die Herkunft behauptete, laege damit bei jedem Merge-Eingang daneben.
   *
   * <p>Zuerst {@code SourceMessageID}, dann {@code TargetMessageID}. Die Reihenfolge ist
   * gleichgueltig, solange nie beide belegt sind — und genau das ist gemessen: {@code Kind +
   * Eingang} ist ueber beide Bezugsfenster <b>null</b> (M28‑1). Die Reihenfolge steht trotzdem
   * fest, damit der Aufstieg auch dann eindeutig bleibt, wenn die Produktion sich nicht daran
   * haelt.
   */
  String aufwaertsId() {
    if (belegt(sourceMessageId)) {
      return sourceMessageId;
    }
    return belegt(targetMessageId) ? targetMessageId : null;
  }

  /**
   * Ueber welche Beziehung das Glied darueber erreicht wird — {@code null}, wenn es keines gibt.
   *
   * <p>Sie steht ausdruecklich in der Antwort, obwohl sie aus Rollen und Ebene ableitbar waere: Der
   * Unterschied zwischen „aufgeteilt" und „zusammengefuehrt" ist das, was die Oberflaeche dem
   * Nutzer sagen muss, und er soll nicht dort zusammengerechnet werden. Seit der Umbenennung der
   * beiden Listen auf {@code aufwaerts}/{@code abwaerts} traegt dieses Feld die Bedeutung allein.
   */
  Kettenbeziehung aufwaertsBeziehung() {
    if (belegt(sourceMessageId)) {
      return Kettenbeziehung.AUFTEILUNG;
    }
    return belegt(targetMessageId) ? Kettenbeziehung.ZUSAMMENFUEHRUNG : null;
  }

  private static boolean belegt(String kennung) {
    return kennung != null && !kennung.isBlank();
  }
}
