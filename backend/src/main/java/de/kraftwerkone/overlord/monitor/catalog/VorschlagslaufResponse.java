package de.kraftwerkone.overlord.monitor.catalog;

/**
 * Was ein Heuristik-Lauf fuer den aktiven Mandanten bewirkt hat (E13).
 *
 * <p>Die drei ersten Zahlen ergeben zusammen die Zahl der Prozesse des Mandanten. Die drei letzten
 * zaehlen nur die <b>geschriebenen</b> Zeilen ({@code angelegt + aufgefrischt}) nach der Herkunft
 * ihres Partnervorschlags — unberuehrte Zeilen tragen ihre eigene, aeltere Herkunft und werden hier
 * nicht mitgezaehlt.
 *
 * <p>Auf einem leeren Katalog reproduzieren die drei Herkunftszahlen damit genau die Tabelle aus
 * {@code docs/prozess-katalog.md} §3.5.
 *
 * @param angelegt fehlende Zeilen, die der Lauf neu angelegt hat
 * @param aufgefrischt Zeilen mit {@link Pflegestatus#OFFEN}, die er neu berechnet hat — <b>auch
 *     solche, die schon einen Vorschlag trugen.</b> Sonst friert der erste Lauf jeden spaeteren
 *     Regelfehler ein (E13)
 * @param unberuehrt Zeilen mit {@link Pflegestatus#GEPFLEGT}. Sie ruehrt der Lauf <b>nie</b> an
 * @param regelA geschriebene Zeilen mit Partner aus Regel A
 * @param regelB geschriebene Zeilen mit Partner aus Regel B
 * @param keine geschriebene Zeilen ohne Partnervorschlag. <b>„Geprueft, nichts abgeleitet"</b> —
 *     nach Regel Q4 ein Ergebnis und kein fehlender Wert
 * @param bestandGeprueft Zeilen, auf die der <b>Bestandslauf</b> geschrieben hat — <b>alle Zeilen
 *     des Mandanten, auch die gepflegten</b> (E15). Die Zahl steht neben den drei ersten und ist
 *     mit ihnen nicht zu verrechnen: Sie zaehlt einen anderen Schritt ueber dieselbe Menge
 * @param ohneNachrichten davon die Zeilen, an denen im Bestand <b>keine</b> Nachricht haengt
 * @implNote <b>Warum die beiden Zahlen ueberhaupt in der Antwort stehen.</b> Ohne sie ist ein
 *     reihenweise wirkungsloser Lauf von einem erfolgreichen nicht zu unterscheiden — dieselbe
 *     Falle wie bei der Zahl verworfener Sitzungen in Schritt 9a. Ein Bestandslauf, der wegen eines
 *     Fehlers null Zeilen anfasst, sieht in einer Antwort ohne {@code bestandGeprueft} genauso aus
 *     wie einer, der 733 Zeilen aufgefrischt hat.
 */
public record VorschlagslaufResponse(
    int angelegt,
    int aufgefrischt,
    int unberuehrt,
    int regelA,
    int regelB,
    int keine,
    int bestandGeprueft,
    int ohneNachrichten) {}
