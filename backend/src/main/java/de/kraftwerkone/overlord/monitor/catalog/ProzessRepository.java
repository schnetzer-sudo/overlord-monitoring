package de.kraftwerkone.overlord.monitor.catalog;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROCESS;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECT;
import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.PROJECTMANDANT;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die Prozesse eines Mandanten — <b>Stammdaten</b>, nicht Bewegungsdaten.
 *
 * <p>Das ist der Unterschied, an dem die Regeln dieses Statements haengen: {@code Process} hat
 * 1.490 Zeilen insgesamt (Messung M1), fuer einen einzelnen Mandanten deutlich weniger. Regel L1
 * (Pflicht-Zeitfenster) und Regel L3 (Cursor statt {@code OFFSET}) gelten fuer {@code Message} mit
 * seinen 3,3 Millionen Zeilen; sie auf eine Stammdatentabelle dieser Groesse anzuwenden, hiesse
 * einen Prozess zu verstecken, nur weil er im gewaehlten Fenster keine Nachricht getragen hat — und
 * genau der ist der interessante Fall, wenn jemand wissen will, warum nichts ankommt.
 */
@Repository
public class ProzessRepository {

  private final DSLContext glassfishDsl;

  ProzessRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * Alle Prozesse des aktiven Mandanten, sortiert nach Projekt und Prozessname.
   *
   * <p><b>Der Mandantenfilter ist Bestandteil des Statements</b> (Regel M3). Anders als in der
   * Nachrichtenliste steht er hier als <b>Join</b> und nicht als {@code EXISTS} — und das ist kein
   * Widerspruch, sondern folgt aus der Richtung des Zugriffs:
   *
   * <ul>
   *   <li>In der Liste treibt das Zeitfenster ueber {@code Message}; ein Join auf das im Schema n:m
   *       stehende {@code ProjectMandant} koennte dort Zeilen vervielfachen, sobald ein Projekt
   *       jemals mehreren Mandanten gehoerte.
   *   <li>Hier ist {@code ProjectMandant} der <b>selektivste</b> Teil der Bedingung und soll den
   *       Zugriff treiben. Vervielfachen kann er nichts: Der Primaerschluessel ist {@code
   *       (ProjectID, MandantID)}, und mit {@code MandantID = ?} bleibt je Projekt hoechstens eine
   *       Zeile uebrig. Das gilt aus dem Schema heraus und nicht erst aus den Daten.
   * </ul>
   *
   * <p><b>{@code Project} als {@code LEFT JOIN}:</b> {@code Process.ProjectID} ist {@code
   * NULL}-faehig. Ein innerer Join liesse einen Prozess ohne Projekt lautlos verschwinden — er
   * traegt dann keinen Mandanten und ist ohnehin fuer niemanden sichtbar (Annahme A8), aber die
   * Sichtbarkeit soll an der Mandantenkette haengen und nicht am Anzeigenamen.
   *
   * <p><b>Sortiert wird in SQL, nicht im Speicher:</b> {@code ProjectName} gruppiert die Auswahl,
   * {@code ProcessName} ordnet innerhalb der Gruppe. Prozesse ohne Projektnamen stehen damit vorn
   * ({@code NULL} sortiert in MariaDB aufsteigend zuerst) — sichtbar und nicht am Ende versteckt.
   */
  public List<ProzessResponse> findeAlle(MandantContext mandant) {
    return glassfishDsl
        .select(PROCESS.PROCESSID, PROCESS.PROCESSNAME, PROJECT.PROJECTNAME)
        .from(PROCESS)
        .join(PROJECTMANDANT)
        .on(PROJECTMANDANT.PROJECTID.eq(PROCESS.PROJECTID))
        .leftJoin(PROJECT)
        .on(PROJECT.PROJECTID.eq(PROCESS.PROJECTID))
        .where(PROJECTMANDANT.MANDANTID.eq(mandant.mandantId()))
        .orderBy(PROJECT.PROJECTNAME.asc(), PROCESS.PROCESSNAME.asc())
        .fetch(satz -> new ProzessResponse(satz.value1(), satz.value2(), satz.value3()));
  }
}
