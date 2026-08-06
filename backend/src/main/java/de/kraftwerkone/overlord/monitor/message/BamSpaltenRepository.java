package de.kraftwerkone.overlord.monitor.message;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMMANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MESSAGEBAMTYPE;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.BAM_SPALTE;

import de.kraftwerkone.overlord.monitor.common.BamSpalte;
import de.kraftwerkone.overlord.monitor.common.BamSpaltenRegel;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Welche zwei BAM-Spalten der aktive Mandant sieht — die Auflösungsregel aus {@code
 * docs/datenzugriff.md} §5, angewandt auf echte Daten.
 *
 * <p>Zwei Abfragen, beide winzig: die Kuratierung aus {@code overlord_monitor.bam_spalte} (0 bis 2
 * Zeilen) und die Konfiguration des Altsystems aus {@code GlassfishDB.MessageBAMMandant} (69 Zeilen
 * insgesamt, hoechstens 40 je Mandant; Messung M7: 0,937 ms). Beide laufen ueber {@code
 * glassfishDsl} — der Lese-Kontext darf beide Schemata lesen, und nur ueber ihn ist ein
 * schemauebergreifender Zugriff auf einer Verbindung moeglich.
 *
 * <p><b>Entschieden wird nicht hier, sondern in {@link BamSpaltenRegel}.</b> Diese Klasse liest und
 * haengt die Beschreibungen an; die Reihenfolge und die Auswahl kommen aus {@code common}, weil ab
 * Schritt 7 auch das Paket {@code bam} sie braucht und Fachpakete einander nicht kennen.
 */
@Repository
public class BamSpaltenRepository {

  private final DSLContext glassfishDsl;

  BamSpaltenRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Die Spalten des aktiven Mandanten — leer, eine oder zwei.
   *
   * <p>Leer heisst leer: {@code EDITIONLINGERI}, {@code SYSTEM} und {@code WOC} haben keine
   * BAM-Konfiguration. Fuer sie entfaellt spaeter auch die Nachlade-Abfrage vollstaendig; ein
   * Aufruf mit leerer Typliste findet nicht statt.
   */
  public List<BamSpalte> spalten(MandantContext mandant) {
    List<BamSpaltenRegel.Kuratiert> kuratiert =
        glassfishDsl
            .select(BAM_SPALTE.POSITION, BAM_SPALTE.MESSAGE_BAM_TYPE)
            .from(BAM_SPALTE)
            .where(BAM_SPALTE.MANDANT_ID.eq(mandant.mandantId()))
            .fetch(satz -> new BamSpaltenRegel.Kuratiert(satz.value1().intValue(), satz.value2()));

    Map<Short, String> beschreibungen = new HashMap<>();
    List<BamSpaltenRegel.Konfiguriert> konfiguriert = new ArrayList<>();
    glassfishDsl
        .select(
            MESSAGEBAMMANDANT.MESSAGEBAMTYPE,
            MESSAGEBAMMANDANT.MESSAGEBAMTYPESORTINDEX,
            MESSAGEBAMTYPE.MESSAGEBAMTYPEDESCRIPTION)
        .from(MESSAGEBAMMANDANT)
        .leftJoin(MESSAGEBAMTYPE)
        .on(MESSAGEBAMTYPE.MESSAGEBAMTYPE_.eq(MESSAGEBAMMANDANT.MESSAGEBAMTYPE))
        .where(MESSAGEBAMMANDANT.MANDANTID.eq(mandant.mandantId()))
        .fetch()
        .forEach(
            satz -> {
              konfiguriert.add(new BamSpaltenRegel.Konfiguriert(satz.value1(), satz.value2()));
              beschreibungen.put(satz.value1(), satz.value3());
            });

    List<Short> typen = BamSpaltenRegel.waehle(kuratiert, konfiguriert);
    ergaenzeFehlendeBeschreibungen(typen, beschreibungen);

    List<BamSpalte> spalten = new ArrayList<>();
    for (int i = 0; i < typen.size(); i++) {
      spalten.add(BamSpalte.mitBeschreibung(i + 1, typen.get(i), beschreibungen.get(typen.get(i))));
    }
    return List.copyOf(spalten);
  }

  /**
   * Ein kuratierter Typ muss in {@code MessageBAMMandant} nicht vorkommen — die Kuratierung ist
   * genau dafuer da, von dieser Konfiguration abzuweichen. Dann fehlt seine Beschreibung noch. Der
   * Nachschlag kostet einen Primaerschluessel-Zugriff und faellt im Normalfall gar nicht an.
   */
  private void ergaenzeFehlendeBeschreibungen(
      List<Short> typen, Map<Short, String> beschreibungen) {
    List<Short> fehlend = typen.stream().filter(typ -> !beschreibungen.containsKey(typ)).toList();
    if (fehlend.isEmpty()) {
      return;
    }
    glassfishDsl
        .select(MESSAGEBAMTYPE.MESSAGEBAMTYPE_, MESSAGEBAMTYPE.MESSAGEBAMTYPEDESCRIPTION)
        .from(MESSAGEBAMTYPE)
        .where(MESSAGEBAMTYPE.MESSAGEBAMTYPE_.in(fehlend))
        .fetch()
        .forEach(satz -> beschreibungen.put(satz.value1(), satz.value2()));
  }
}
