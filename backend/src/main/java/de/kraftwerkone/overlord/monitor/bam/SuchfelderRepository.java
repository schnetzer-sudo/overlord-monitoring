package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEPROPERTYSEARCHLISTENTRY;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Das eine Statement hinter der Feld-Gruppe von {@code GET /api/bam/suchfelder}: <b>welche
 * Feldnamen der Mandant der Sitzung zur Auswahl bekommt</b>.
 *
 * <p><b>Der Mandantenfilter ist das {@code WHERE} selbst</b> (Regel M3), und er hat zwei Zweige:
 * {@code MandantID IS NULL OR MandantID = :mandant}. {@code NULL} heißt „gilt für alle" — das ist
 * eine Auskunft des Auftraggebers und keine Messung, und sie trägt die Hälfte des Angebots: Alle
 * acht Typ‑0‑Namen sind global konfiguriert (M154, M161). Ein {@code WHERE MandantID = :mandant}
 * <b>ohne</b> den {@code NULL}-Zweig löschte acht von vierzehn Einträgen aus dem Angebot; ein
 * Zugriff <b>ohne</b> Filter zeigte einem Mandanten die Konfiguration eines anderen.
 *
 * <p><b>Keine {@code EXISTS}-Kette</b>, aus demselben Grund wie in {@link BamTypenRepository}: Die
 * Tabelle trägt die {@code MandantID} selbst. Die Kette gibt es dort, wo eine Tabelle den Mandanten
 * nicht führt.
 *
 * <p><b>Kein Zeitfenster</b>, und Regel L1 ist nicht berührt: Sie gilt für Listen über {@code
 * Message}. Hier steht eine Konfigurationstabelle mit vierzehn Zeilen (M161) und ohne Zeitstempel.
 * <b>{@code Message} und {@code MessageProperty} bleiben unberührt</b> — die Auswahl sagt, was
 * konfiguriert ist, und behauptet nichts darüber, was im Bestand steht. M157 beziffert, was die
 * Gegenform kostete: Schon das Zählen eines einzigen Namens über den Gesamtbestand braucht 125,527
 * s.
 *
 * <p><b>Sortiert nach dem Namen</b>, weil die Tabelle keine Ordnung kennt — sie hat weder eine
 * Beschriftungs- noch eine Sortierspalte (M153, Punkt 147). Eine alphabetische Ordnung ist die
 * einzige, die ohne Kuratierung auskommt (E‑105).
 */
@Repository
public class SuchfelderRepository {

  private final DSLContext glassfishDsl;

  SuchfelderRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Die für diesen Mandanten sichtbaren Feldnamen: die globalen und die ihm zugeordneten.
   *
   * @param mandant erster Pflichtparameter (Regel M2) — er <i>ist</i> hier der zweite Zweig der
   *     Bedingung
   */
  public List<SuchfeldZeile> findeKonfigurierteFelder(MandantContext mandant) {
    return glassfishDsl
        .select(
            MESSAGEPROPERTYSEARCHLISTENTRY.MESSAGEPROPERTYNAME,
            MESSAGEPROPERTYSEARCHLISTENTRY.MESSAGEPROPERTYTYPE)
        .from(MESSAGEPROPERTYSEARCHLISTENTRY)
        .where(
            MESSAGEPROPERTYSEARCHLISTENTRY
                .MANDANTID
                .isNull()
                .or(MESSAGEPROPERTYSEARCHLISTENTRY.MANDANTID.eq(mandant.mandantId())))
        .orderBy(MESSAGEPROPERTYSEARCHLISTENTRY.MESSAGEPROPERTYNAME.asc())
        .fetch(satz -> new SuchfeldZeile(satz.value1(), satz.value2()));
  }
}
