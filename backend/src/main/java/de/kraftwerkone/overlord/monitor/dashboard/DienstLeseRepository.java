package de.kraftwerkone.overlord.monitor.dashboard;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.SERVICE;

import de.kraftwerkone.overlord.monitor.jooq.glassfish.tables.Service;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * <b>Die eine Klasse, die {@code Service} liest</b> — und damit die <b>dritte benannte Ausnahme von
 * Regel M2</b> (E‑123).
 *
 * <h2>Warum sie keinen {@code MandantContext} nimmt</h2>
 *
 * <p><b>Weil {@code Service} keinen Mandanten kennt.</b> Die Tabelle traegt die Dienste der
 * Plattform — 20 Zeilen, davon elf Ablagen (M52) —, und keine ihrer Spalten verweist auf einen
 * Mandanten, ein Projekt oder einen Prozess. Es gibt nichts zu filtern.
 *
 * <p><b>Ein Schein-Kontext waere schlimmer als keiner.</b> Ein {@code MandantContext}, der
 * entgegengenommen und im Statement nicht verwendet wird, saehe von aussen wie Mandantentrennung
 * aus und waere eine Luege im Typsystem — genau das, was Regel M2 verhindern soll. Dieselbe
 * Begruendung traegt {@code rollup/RollupLeseRepository}, die zweite Ausnahme ({@code
 * docs/mandantentrennung.md} §4).
 *
 * <p><b>Die Ausnahme steht namentlich in {@code PaketstrukturTest} und nicht als
 * Paketfreibrief.</b> Eine zweite Klasse in {@code dashboard}, die {@code jooq.glassfish} ohne
 * Mandanten anfasst, faellt <b>nicht</b> von selbst darunter. Zwei weitere Tests halten die
 * Ausnahme eng: Diese Klasse fasst <b>ausschliesslich {@code Service}</b> an, und sie <b>schreibt
 * nicht</b>.
 *
 * <h2>Was der Block dieser Klasse liefert und was er nicht liefert</h2>
 *
 * <p>Der plattformweite Block ist fuer <b>jeden Mandanten identisch</b> — das ist keine Luecke in
 * der Trennung, sondern ihr Gegenstand: Er sagt nichts ueber Belege, sondern ueber die Anlage, auf
 * der sie laufen. Ein Mandant erfaehrt daraus nichts ueber einen anderen.
 *
 * <h2>Zwei Statements, beide ueber {@code glassfishDsl}</h2>
 *
 * <p>Ein Vollzugriff ueber 20 Zeilen. Das ist die in {@code DEVELOPMENT_GUIDELINES.md} unter L9
 * fuer {@code Service} benannte Ausnahme vom Zeitfenster — eine Stammdatentabelle mit 20 Zeilen
 * kennt keinen Zeitbezug, und M52 hat den Vollzugriff mit <b>1,348 ms</b> gemessen. Die Messung
 * dieses Schritts steht in {@code docs/dienste.md} §11 (M175).
 */
@Repository
public class DienstLeseRepository {

  /**
   * Die zweite Sicht auf dieselbe Tabelle: die Zeile, auf die {@code ServiceDefaultFileStore}
   * <i>zeigt</i>.
   *
   * <p>Ohne Alias waere es ein Selbstjoin ohne unterscheidbare Namen, und im gerenderten Statement
   * waere nicht mehr erkennbar, welche Seite die eintragende und welche die eingetragene ist.
   */
  private static final Service ZIEL = SERVICE.as("ablagenziel");

  private final DSLContext glassfishDsl;

  DienstLeseRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /**
   * <b>Die Dienste mit Zeitgrenze</b> — die Lampen des Blocks (E‑117).
   *
   * <pre>
   * SELECT ServiceID, ServiceStatus, ServiceLastUpdate
   *   FROM GlassfishDB.Service
   *  WHERE ServiceTimeout &gt; 0
   *  ORDER BY ServiceID;
   * </pre>
   *
   * <p><b>{@code ServiceTimeout &gt; 0} ist die ganze Bedingung.</b> Wer keine Zeitgrenze hat, kann
   * sie auch nicht reissen — eine Lampe dafuer waere ein Feld ohne Aussage. Zeilen mit {@code NULL}
   * fallen durch den Vergleich von selbst heraus, und das ist die richtige Richtung: {@code NULL}
   * heisst nicht „Grenze null", sondern „keine Angabe".
   *
   * <p><b>{@code ORDER BY ServiceID}</b> und nicht nach Zustand: Die Reihenfolge der Lampen soll
   * zwischen zwei Aufrufen dieselbe sein. Sortierte man nach Zustand, spraenge eine Lampe an eine
   * andere Stelle, sobald sich ihr Zustand aendert — und genau dann sucht jemand sie an ihrem alten
   * Platz.
   *
   * <p>Der Zugriff ist ein Vollzugriff ueber 20 Zeilen; ein Index auf {@code ServiceTimeout}
   * existiert nicht und waere bei dieser Groesse ohne Wirkung.
   */
  public List<Dienstzeile> dienste() {
    return glassfishDsl
        .select(SERVICE.SERVICEID, SERVICE.SERVICESTATUS, SERVICE.SERVICELASTUPDATE)
        .from(SERVICE)
        .where(SERVICE.SERVICETIMEOUT.gt((short) 0))
        .orderBy(SERVICE.SERVICEID)
        .fetch(satz -> new Dienstzeile(satz.value1(), satz.value2(), satz.value3()));
  }

  /**
   * <b>Die Ziele der Ablagenpruefung</b>: die verschiedenen, nicht leeren Werte von {@code
   * ServiceDefaultFileStore} ueber <b>alle</b> Zeilen, je mit der aufgeloesten Zeile.
   *
   * <pre>
   * SELECT DISTINCT s.ServiceDefaultFileStore, ziel.ServiceConnectString
   *   FROM GlassfishDB.Service s
   *   LEFT JOIN GlassfishDB.Service ziel ON ziel.ServiceID = s.ServiceDefaultFileStore
   *  WHERE s.ServiceDefaultFileStore IS NOT NULL
   *    AND s.ServiceDefaultFileStore &lt;&gt; ''
   *  ORDER BY s.ServiceDefaultFileStore;
   * </pre>
   *
   * <p><b>{@code LEFT JOIN} und nicht {@code JOIN}</b>: Eine Kennung, die auf keine Zeile aufloest,
   * soll <i>erscheinen</i> und nicht verschwinden. Sie ist fuer die Pruefung nicht erreichbar — und
   * das ist eine Auskunft, kein Grund zum Weglassen. Ein {@code JOIN} liesse die Kachel gruen
   * bleiben, weil das Ziel gar nicht erst geprueft wuerde.
   *
   * <p><b>{@code NULL} und Leerstring gelten beide als leer.</b> Unter der Sortierung {@code PAD
   * SPACE} faellt eine Zeichenkette aus lauter Leerzeichen ebenfalls unter {@code &lt;&gt; ''} —
   * auch das ist die richtige Richtung, und {@link Ablagenziel#aufloesbar()} faengt den Rest.
   *
   * <p><b>Die Werte der Spalte werden nicht erhoben</b> (E‑119). Diese Methode liest, wogegen
   * geprueft wird; welche Zeile welchen Wert traegt und wie viele es sind, ist keine Frage dieses
   * Schritts.
   */
  public List<Ablagenziel> pruefziele() {
    return glassfishDsl
        .selectDistinct(SERVICE.SERVICEDEFAULTFILESTORE, ZIEL.SERVICECONNECTSTRING)
        .from(SERVICE)
        .leftJoin(ZIEL)
        .on(ZIEL.SERVICEID.eq(SERVICE.SERVICEDEFAULTFILESTORE))
        .where(SERVICE.SERVICEDEFAULTFILESTORE.isNotNull())
        .and(SERVICE.SERVICEDEFAULTFILESTORE.ne(""))
        .orderBy(SERVICE.SERVICEDEFAULTFILESTORE)
        .fetch(satz -> new Ablagenziel(satz.value1(), satz.value2()));
  }
}
