package de.kraftwerkone.overlord.monitor.message;

/**
 * Eine kuratierte technische Eigenschaft im Kopf der Detailansicht.
 *
 * <p><b>Leere Werte werden nicht geliefert</b> — kein Feld mit leerer Zeichenkette, kein {@code
 * null} als Platzhalter. Die Oberflaeche soll gar nicht erst in die Lage kommen, eine leere Zeile
 * zu zeichnen. Genau daran ist die BAM-Spalte in Schritt 4 gescheitert.
 *
 * @param name der <b>Rohname</b> aus {@code MessagePropertyName}, etwa {@code
 *     Message.SendingPartner}. Die deutsche Beschriftung kommt aus der Sprachdatei der Oberflaeche
 *     und nicht von hier
 * @param wert der Wert, unveraendert. Kuratierte Werte sind kurz (gemessen 1 bis 17 Zeichen) und
 *     werden nicht gekappt
 * @param rang die Anzeigereihenfolge, ab 1 — festgelegt in {@link KuratierteEigenschaften}
 */
public record KuratierteEigenschaftResponse(String name, String wert, int rang) {}
