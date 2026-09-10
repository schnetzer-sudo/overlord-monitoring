package de.kraftwerkone.overlord.monitor.dashboard;

import java.util.List;

/**
 * Der plattformweite Block der Landingpage — <b>fuer jeden Mandanten identisch</b> (E‑116).
 *
 * <h2>Warum das keine Luecke in der Mandantentrennung ist</h2>
 *
 * <p>Er sagt nichts ueber <i>Belege</i>, sondern ueber die <i>Anlage</i>, auf der sie laufen. Ein
 * Mandant erfaehrt daraus nichts ueber einen anderen: keine Kennung, keine Zahl, keinen Prozess.
 * {@code DashboardIsolationDbIT} haelt fest, dass zwei Mandanten hier genau dasselbe sehen — und
 * das ist die <b>Zusicherung</b>, nicht die Ausnahme.
 *
 * <h2>Warum er im selben Aufruf steht</h2>
 *
 * <p><b>Kein Nachladen, kein zweiter Endpunkt</b> — dieselbe Begruendung wie fuer die uebrigen
 * Bloecke ({@code docs/dashboard.md} §1): Eine zweite Anfrage kostet eine Sitzungspruefung und
 * einen Verbindungsgriff, und auf der Testkopie schreibt jede Anfrage zusaetzlich die Sitzung fort.
 * Der Block kostet <b>ein</b> Statement ueber 20 Zeilen; die Pruefung der Ablagen liegt ohnehin
 * ausserhalb der Anfrage.
 *
 * @param dienste je Dienst mit {@code ServiceTimeout > 0} eine Lampe, nach {@code ServiceID}
 *     sortiert. <b>Wer keine Zeitgrenze hat, erscheint hier nicht</b> (E‑117) — und die elf Ablagen
 *     haben keine
 * @param ablagen die eine Kachel fuer die Ablagen. Sie steht <b>immer</b> da, auch wenn die
 *     Pruefung abgeschaltet ist: Dann sagt sie „ungeklaert" mit Grund. Eine fehlende Kachel waere
 *     Abwesenheit, und Abwesenheit ist der schwaechste Kanal, den ein Zustand haben kann
 */
public record PlattformResponse(List<DienstResponse> dienste, AblagenResponse ablagen) {

  public PlattformResponse {
    dienste = List.copyOf(dienste);
  }
}
