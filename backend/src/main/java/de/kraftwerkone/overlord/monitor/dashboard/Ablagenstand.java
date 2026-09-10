package de.kraftwerkone.overlord.monitor.dashboard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Das Ergebnis <b>eines</b> Durchgangs der Ablagenpruefung — unveraenderlich, im Speicher gehalten,
 * in keiner Tabelle.
 *
 * <h2>Warum nichts davon geschrieben wird</h2>
 *
 * <p><b>Kein Eintrag im {@code audit_log}, keine eigene Tabelle</b> (E‑120). Das Protokoll haelt
 * fest, was ein <i>Nutzer</i> getan hat ({@code docs/protokollierung.md}); ein Hintergrundlauf, der
 * im Minutentakt zwei Knoten anfragt, ist kein Abruf eines Nutzers und wuerde es fluten. Und eine
 * Tabelle waere eine zweite Wahrheit ueber einen Zustand, der ohnehin nur so lange gilt, wie er
 * frisch ist.
 *
 * <p><b>Die Folge ist gewollt:</b> Nach einem Neustart gibt es keinen Stand, und die Kachel sagt
 * {@link Ablagengrund#NOCH_KEIN_DURCHGANG} — statt einen alten Stand aus der Datenbank zu zeigen,
 * fuer den niemand mehr einsteht.
 *
 * @param geprueftAm der Zeitpunkt des Durchgangs, aus der <b>Anwendungsuhr</b> (Regel Z1). Im
 *     Profil {@code dev} traegt er deshalb das Datum des Ankers — <b>sein Alter ist trotzdem
 *     echt</b>, weil auch {@code jetzt} aus derselben Uhr kommt
 * @param ziele je Ziel ein Zustand, in der Reihenfolge der Kennungen. <b>Leer heisst: es ist keines
 *     eingetragen</b> — und das ist etwas anderes als „kein Durchgang"
 */
public record Ablagenstand(LocalDateTime geprueftAm, List<Zielstand> ziele) {

  public Ablagenstand {
    ziele = List.copyOf(ziele);
  }
}
