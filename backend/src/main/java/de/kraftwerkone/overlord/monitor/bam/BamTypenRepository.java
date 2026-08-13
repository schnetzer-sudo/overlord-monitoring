package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMTYPE;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Das eine Statement hinter {@code GET /api/bam/typen}: <b>welche Belegarten der Mandant der
 * Sitzung zur Auswahl bekommt</b>.
 *
 * <p><b>Der Mandantenfilter ist das {@code WHERE} selbst</b> (Regel M3) und keine nachgelagerte
 * Prüfung. Anders als bei jedem anderen Statement dieses Pakets steht hier <b>keine {@code
 * EXISTS}-Kette über {@code Process → ProjectMandant}</b>, und das ist kein Abweichen von der
 * Vorlage: {@code MessageBAMMandant} trägt die {@code MandantID} als Teil ihres Primärschlüssels.
 * Die Kette gibt es genau dort, wo eine Tabelle den Mandanten <i>nicht</i> selbst führt — {@code
 * Message} und {@code MessageBAM} tun das nicht.
 *
 * <p><b>Kein Zeitfenster</b>, und das ist keine Nachlässigkeit gegenüber Regel L1: Die gilt für
 * <i>Listen</i> über {@code Message}. Angefasst werden hier ausschließlich Stammdaten — {@code
 * MessageBAMMandant} und {@code MessageBAMType} zählen zusammen weniger Zeilen als eine einzige
 * Seite der Nachrichtenliste, und keine von beiden trägt einen Zeitstempel.
 *
 * <p><b>{@code Message} und {@code MessageBAM} bleiben unberührt.</b> Die Auswahl sagt, was
 * <i>konfiguriert</i> ist, und behauptet nichts darüber, was im Bestand steht. Eine Auswahl, die
 * nur belegte Typen anböte, wäre eine Existenzfrage über den Gesamtbestand eines Mandanten — genau
 * die Gestalt, die {@code docs/nachrichtenliste.md} §1 als L15-Falle führt: für {@code IBIS}
 * gemessene 13,2 Sekunden gegen 11,9 Millisekunden für {@code WOC}, beide ohne ein einziges
 * Ergebnis.
 */
@Repository
public class BamTypenRepository {

  private final DSLContext glassfishDsl;

  BamTypenRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Die konfigurierten Typen dieses Mandanten, in der Reihenfolge ihres {@code
   * MessageBAMTypeSortIndex}.
   *
   * <p><b>Der Join ist ein {@code LEFT JOIN}</b>, aus demselben Grund wie in {@link
   * BamRepository#zaehleJeTyp}: Fehlt die Zeile in {@code MessageBAMType}, darf der Typ nicht
   * verschwinden — er bekommt statt der Beschriftung seine Typnummer ({@link Typbezeichnung}). Ein
   * innerer Join machte die Beschriftung zur Bedingung dafür, dass ein konfigurierter Typ überhaupt
   * auswählbar ist.
   *
   * <p><b>Sortiert wird über zwei Spalten und nicht über eine.</b> {@code MessageBAMTypeSortIndex}
   * ist im Schema nicht eindeutig; ohne die Typnummer als zweiten Schlüssel entschiede bei gleichem
   * Index die Reihenfolge der Speicherung, und zwei Aufrufe zeigten dieselbe Liste verschieden.
   *
   * @param mandant erster Pflichtparameter (Regel M2) — er <i>ist</i> hier die Bedingung und nicht
   *     nur ein Filter darüber
   */
  public List<BamTypenZeile> findeKonfigurierteTypen(MandantContext mandant) {
    return glassfishDsl
        .select(
            MESSAGEBAMMANDANT.MESSAGEBAMTYPE,
            MESSAGEBAMTYPE.MESSAGEBAMTYPEDESCRIPTION,
            MESSAGEBAMMANDANT.MESSAGEBAMTYPESORTINDEX)
        .from(MESSAGEBAMMANDANT)
        .leftJoin(MESSAGEBAMTYPE)
        .on(MESSAGEBAMTYPE.MESSAGEBAMTYPE_.eq(MESSAGEBAMMANDANT.MESSAGEBAMTYPE))
        .where(MESSAGEBAMMANDANT.MANDANTID.eq(mandant.mandantId()))
        .orderBy(
            MESSAGEBAMMANDANT.MESSAGEBAMTYPESORTINDEX.asc(), MESSAGEBAMMANDANT.MESSAGEBAMTYPE.asc())
        .fetch(satz -> new BamTypenZeile(satz.value1(), satz.value2(), satz.value3()));
  }
}
