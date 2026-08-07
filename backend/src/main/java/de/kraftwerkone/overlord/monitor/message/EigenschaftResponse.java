package de.kraftwerkone.overlord.monitor.message;

/**
 * Eine technische Eigenschaft aus {@code /api/nachrichten/&#123;messageId&#125;/eigenschaften}.
 *
 * @param name {@code MessagePropertyName}, roh
 * @param wert der Wert — moeglicherweise gekappt, siehe {@link #gekappt()}
 * @param position {@code MessageActionID}, der Schritt, an dem die Eigenschaft haengt.
 *     <b>Durchgereicht und nicht gedeutet.</b> M17 (3) hat gemessen, dass sich die Eigenschaften
 *     entlang einer Familiengrenze verteilen — {@code Message.*} ausnahmslos auf dem
 *     Metadaten-Schritt {@code 0}, dienstbezogene am jeweiligen Schritt. Ob die Oberflaeche das
 *     nutzt, entscheidet Teil 2; das Backend liefert die Zuordnung
 * @param gekappt ob der Wert die Byte-Grenze gerissen hat. Ohne dieses Kennzeichen waere ein
 *     abgeschnittener Wert von einem kurzen nicht zu unterscheiden — und ein Nutzer laese eine
 *     halbe Belegnummer als ganze
 * @param originalLaengeBytes die volle Laenge des Werts in Bytes; {@code null}, wenn nichts gekappt
 *     wurde
 */
public record EigenschaftResponse(
    String name, String wert, int position, boolean gekappt, Integer originalLaengeBytes) {}
