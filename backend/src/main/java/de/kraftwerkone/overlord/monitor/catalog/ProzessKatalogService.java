package de.kraftwerkone.overlord.monitor.catalog;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Bestandsflag;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Bestandszeile;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Projektzeile;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.UebernehmbareZeile;
import de.kraftwerkone.overlord.monitor.catalog.ProzessKatalogRepository.Vorschlagszeile;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Die Fachlogik des Prozess-Katalogs: was gepflegt wird, was vorgeschlagen wird, und was
 * unangetastet bleibt.
 *
 * <p><b>Die Uhr ist die Systemuhr</b> ({@code systemClock}) und nicht die Anwendungsuhr. Die
 * Anwendungsuhr ist im Profil {@code dev} um den Rueckstand der Testkopie zurueckversetzt — ein
 * Aenderungszeitpunkt, der Wochen in der Vergangenheit liegt, waere kein Aenderungszeitpunkt. Es
 * ist dieselbe Wahl wie beim {@code audit_log} (Regel A5).
 */
@Service
public class ProzessKatalogService {

  private static final Logger log = LoggerFactory.getLogger(ProzessKatalogService.class);

  /** {@code process_catalog.partner} ist {@code varchar(100)}. */
  private static final int PARTNER_MAXLAENGE = 100;

  private final ProzessKatalogRepository repository;
  private final AuditLogWriter auditLogWriter;
  private final Clock systemClock;

  ProzessKatalogService(
      ProzessKatalogRepository repository,
      AuditLogWriter auditLogWriter,
      @Qualifier("systemClock") Clock systemClock) {
    this.repository = repository;
    this.auditLogWriter = auditLogWriter;
    this.systemClock = systemClock;
  }

  /**
   * Die Pflegeliste. <b>Ohne Fortschrittszahl daneben</b> (E8): Die volle Liste kommt zurueck und
   * wird vorne gezaehlt. Ein {@code COUNT(*)} als zweite Abfrage fuehrte denselben Filter ein
   * zweites Mal — und zwei Abfragen koennen zwei Antworten geben.
   */
  public List<KatalogzeileResponse> pflegeliste(MandantContext mandant, boolean nurOffene) {
    return repository.findePflegeliste(mandant, nurOffene);
  }

  /** Die abgeleitete Partner-Auswahlliste des aktiven Mandanten (E2). */
  public List<String> partner(MandantContext mandant) {
    return repository.findePartner(mandant);
  }

  /**
   * Setzt Partner und Richtung eines Prozesses und macht ihn gepflegt.
   *
   * <p><b>Ein leerer Partner ist gueltig</b> und bedeutet „hingesehen, es gibt nichts" (E4). Ohne
   * diese Speicherbarkeit stuenden Auffangprozesse dauerhaft auf offen und der Fortschritt
   * erreichte nie sein Ende.
   *
   * <p>Eine {@code processId} ausserhalb des aktiven Mandanten ergibt {@code 404} und nicht {@code
   * 403} — ein {@code 403} verriete, dass es den Prozess gibt.
   */
  @Transactional
  public KatalogzeileResponse zuordnen(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String processId,
      String partnerRoh,
      String richtungRoh,
      String ip) {
    KatalogzeileResponse vorher =
        repository
            .findeZeile(mandant, processId)
            .orElseThrow(
                () ->
                    new RessourceNichtGefundenException(
                        "Prozess nicht sichtbar oder nicht vorhanden bei der Katalogpflege"));

    String partner = partnerOderNull(partnerRoh);
    Richtung richtung = Richtung.ausText(richtungRoh);

    repository.speichereZuordnung(
        mandant, processId, partner, richtung, jetztUtc(), nutzer.username());
    protokolliere(
        nutzer,
        mandant,
        processId,
        "zugeordnet: Partner "
            + beschriftung(partner)
            + ", Richtung "
            + beschriftung(richtung == null ? null : richtung.name()),
        ip);

    // Die Antwort wird gebaut und NICHT nachgelesen — und das ist keine Sparsamkeit, sondern
    // notwendig: @Transactional bindet ausschliesslich den Schreib-Kontext. Ein Nachlesen liefe
    // ueber den Lese-Pool, also ueber eine ANDERE Verbindung ausserhalb dieser Transaktion, und
    // saehe die eben geschriebene Zeile noch gar nicht (docs/datenzugriff.md §1). Der erste
    // Entwurf hat genau das getan und "OFFEN" zurueckgegeben, obwohl "GEPFLEGT" geschrieben war.
    //
    // Gebaut ist die Antwort trotzdem vollstaendig richtig: Die Stammdatenfelder stammen aus dem
    // Lesevorgang oben, Partner und Richtung sind die eben gesetzten Werte, der Status ist per
    // Definition GEPFLEGT — und die Herkunft bleibt, was sie war. Das gilt in beiden Faellen: Beim
    // Anlegen schreibt das Repository KEINE, und genau das steht in einer Zeile ohne Katalogeintrag
    // auch vorher schon.
    return new KatalogzeileResponse(
        vorher.processId(),
        vorher.projectId(),
        vorher.projectName(),
        vorher.processName(),
        partner,
        richtung,
        Pflegestatus.GEPFLEGT,
        vorher.vorschlagHerkunft(),
        // Beide bleiben, was sie waren: Eine Zuordnung ist eine Kuratierung und keine Erhebung. Sie
        // aendert nicht, ob der Prozess Nachrichten traegt, und sie prueft es auch nicht nach — das
        // tut allein der Bestandslauf (E14, E15). Beim Anlegen einer bisher fehlenden Zeile steht
        // hier null, und das ist richtig: "noch nie geprueft".
        vorher.traegtNachrichten(),
        vorher.bestandGeprueftAm());
  }

  /**
   * Setzt <b>ein</b> Feld fuer alle Prozesse eines Projekts (E11) — in einem von zwei Modi.
   *
   * <p><b>Beide Modi teilen ein Statement und dieselbe Bedingung:</b> Die Vorschau ruft {@code
   * findeProjektbestand}, und die Ausfuehrung ruft ueber {@code speichereFeld} genau dieselbe
   * Methode. Getrennt gebaut driften die beiden auseinander, und der Nutzer bestaetigt dann eine
   * Zahl, die nicht die ist, die passiert.
   */
  @Transactional
  public MassenzuordnungResponse massenzuordnung(
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String projectId,
      String feldRoh,
      String wertRoh,
      String modusRoh,
      String ip) {
    if (projectId == null || projectId.isBlank()) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "projekt-fehlt",
          "Projekt fehlt",
          "Waehle das Projekt, fuer das zugeordnet werden soll.",
          "Massenzuordnung ohne Projekt");
    }
    Zuordnungsfeld feld = Zuordnungsfeld.ausText(feldRoh);
    Massenmodus modus = Massenmodus.ausText(modusRoh);
    String wert = wertFuer(feld, wertRoh);

    if (!repository.projektGehoertZumMandanten(mandant, projectId)) {
      throw new RessourceNichtGefundenException(
          "Projekt nicht sichtbar oder nicht vorhanden bei der Massenzuordnung");
    }

    List<Projektzeile> betroffen =
        modus == Massenmodus.VORSCHAU
            ? repository.findeProjektbestand(mandant, projectId)
            : repository.speichereFeld(
                mandant, projectId, feld, wert, jetztUtc(), nutzer.username());
    int gepflegt =
        (int)
            betroffen.stream()
                .filter(zeile -> zeile.pflegestatus() == Pflegestatus.GEPFLEGT)
                .count();

    if (modus == Massenmodus.AUSFUEHREN) {
      protokolliere(
          nutzer,
          mandant,
          projectId,
          "Massenzuordnung: Feld "
              + feld.name()
              + " auf "
              + beschriftung(wert)
              + " fuer "
              + betroffen.size()
              + " Prozesse, davon "
              + gepflegt
              + " zuvor gepflegt",
          ip);
    }
    return new MassenzuordnungResponse(modus, projectId, feld, wert, betroffen.size(), gepflegt);
  }

  /**
   * Der Lauf fuer den aktiven Mandanten — <b>ein Knopf, drei Schritte</b> (E13, E14, E15).
   *
   * <table border="1">
   *   <caption>Die drei Schritte, und wie vorsichtig jeder ist</caption>
   *   <tr><td><b>1. legt an</b></td><td>fehlende Katalogzeilen</td></tr>
   *   <tr><td><b>2. schlaegt vor</b></td><td>Zeilen mit {@link Pflegestatus#OFFEN}, <b>auch wenn
   *       sie schon einen Vorschlag tragen</b> — sonst friert der erste Lauf jeden spaeteren
   *       Regelfehler ein. Zeilen mit {@link Pflegestatus#GEPFLEGT} ruehrt er <b>nie</b> an
   *       (E13)</td></tr>
   *   <tr><td><b>3. erhebt den Bestand</b></td><td><b>alle</b> Zeilen des Mandanten, auch die
   *       gepflegten (E15)</td></tr>
   * </table>
   *
   * <p><b>Schritt 2 und Schritt 3 sind verschieden vorsichtig, und das ist Absicht.</b> E13
   * schuetzt <b>Kuratierung</b>, nicht <b>Beobachtung</b>: Was ein Mensch entschieden hat, bleibt
   * stehen; was die Datenbank sagt, wird bei jedem Lauf neu gesagt. Die beiden Schritte stehen
   * deshalb in zwei getrennten Repository-Methoden mit sprechenden Namen — {@code
   * speichereVorschlaege} und {@code speichereBestandsflags} —, damit der Unterschied beim Lesen
   * sichtbar ist und nicht in einem Schalter verschwindet.
   *
   * <p><b>Nicht beim Anwendungsstart.</b> Er liefe bei jedem Neustart ueber 1.503 Zeilen, obwohl
   * neue Prozesse selten entstehen — und ein Schreibzugriff im Startpfad ist die Sorte
   * Nebenwirkung, die man ein Jahr spaeter nicht mehr erwartet. Es gibt deshalb keinen {@code
   * ApplicationRunner}, kein {@code @PostConstruct} und keinen Scheduler.
   */
  @Transactional
  public VorschlagslaufResponse vorschlagen(
      AngemeldeterNutzer nutzer, MandantContext mandant, String ip) {
    List<Bestandszeile> bestand = repository.findeBestand(mandant);

    List<Vorschlagszeile> zuSchreiben = new ArrayList<>();
    int angelegt = 0;
    int aufgefrischt = 0;
    int unberuehrt = 0;
    int regelA = 0;
    int regelB = 0;
    int keine = 0;

    for (Bestandszeile zeile : bestand) {
      if (zeile.gespeicherterStatus() == Pflegestatus.GEPFLEGT) {
        unberuehrt++;
        continue;
      }
      if (zeile.gespeicherterStatus() == null) {
        angelegt++;
      } else {
        aufgefrischt++;
      }
      Partnervorschlag vorschlag =
          Partnerheuristik.vorschlag(mandant.mandantId(), zeile.processId(), zeile.projectId());
      switch (vorschlag.herkunft()) {
        case REGEL_A -> regelA++;
        case REGEL_B -> regelB++;
        case KEINE -> keine++;
      }
      zuSchreiben.add(new Vorschlagszeile(zeile.processId(), vorschlag));
    }

    // Schritt 1 und 2 in einem Zug: fehlende Zeilen anlegen, offene auffrischen, gepflegte
    // verschonen.
    repository.speichereVorschlaege(mandant, zuSchreiben, jetztUtc(), nutzer.username());

    // Schritt 3, der Bestandslauf — und er laeuft NACH Schritt 1/2, nicht davor. Danach traegt
    // jeder Prozess des Mandanten eine Katalogzeile, und das UPDATE unten findet sie alle. Davor
    // gingen die eben erst angelegten Zeilen leer aus und stuenden bis zum naechsten Knopfdruck
    // auf "noch nie geprueft".
    List<Bestandsflag> flags = repository.findeBestandsflags(mandant);
    int ohneNachrichten = (int) flags.stream().filter(flag -> !flag.traegtNachrichten()).count();
    int bestandGeprueft = repository.speichereBestandsflags(mandant, flags, jetztUtc());

    protokolliere(
        nutzer,
        mandant,
        mandant.mandantId(),
        "Heuristik-Lauf: "
            + angelegt
            + " angelegt, "
            + aufgefrischt
            + " aufgefrischt, "
            + unberuehrt
            + " unberuehrt (Regel A "
            + regelA
            + ", Regel B "
            + regelB
            + ", keine "
            + keine
            + "); Bestandslauf: "
            + bestandGeprueft
            + " geprueft, davon "
            + ohneNachrichten
            + " ohne Nachrichten",
        ip);
    return new VorschlagslaufResponse(
        angelegt,
        aufgefrischt,
        unberuehrt,
        regelA,
        regelB,
        keine,
        bestandGeprueft,
        ohneNachrichten);
  }

  /**
   * <b>Die Uebernahme der Partnervorschlaege</b> (E22 bis E24) — ein Knopfdruck, alle offenen
   * Zeilen des Mandanten mit einem Partnervorschlag aus Regel A oder Regel B.
   *
   * <p><b>Uebernehmen ist ausschliesslich eine Statusaenderung.</b> Partner und Richtung stehen
   * bereits in der Zeile; die Heuristik hat sie beim Lauf geschrieben, nur mit Status {@link
   * Pflegestatus#OFFEN}. Es wird deshalb <b>kein einziger Feldwert kopiert</b> — gesetzt werden
   * {@code pflegestatus}, {@code geaendert_am} und {@code geaendert_von}, sonst nichts.
   *
   * <p><b>Die Menge wird hier berechnet und nicht im Browser</b> (E23). Ueber die Leitung reist
   * <b>keine Liste von {@code ProcessID}</b>, sondern nur ein Modus. Laege die Bedingung aus E22
   * zusaetzlich im Browser, stuende dieselbe Regel an zwei Stellen und driftete — genau das Muster,
   * das dieses Projekt an mehreren Stellen dokumentiert. Der clientseitige Filter {@code
   * nurMitNachrichten} (E20) hat auf die Uebernahme darum <b>keine Wirkung</b>.
   *
   * <p><b>Beide Modi lesen, und zwar dasselbe</b> (E24). Die Vorschau zaehlt und antwortet; die
   * Ausfuehrung schreibt <b>genau die Liste, die sie gezaehlt hat</b>. Die Zahl, die der Nutzer
   * bestaetigt, ist damit die Zahl, die passiert — und zwar nicht, weil zwei Statements gleich
   * aussehen, sondern weil es nur eine Liste gibt.
   *
   * <p><b>Eine leere Menge ist kein Fehler.</b> Sie setzt kein {@code UPDATE} ab — eine {@code
   * IN}-Liste ohne Werte waere entweder ein Syntaxfehler oder ein {@code UPDATE} ohne Bedingung —
   * und antwortet trotzdem, mit lauter Nullen. „Es gibt nichts zu uebernehmen" ist eine Auskunft.
   */
  @Transactional
  public VorschlagsuebernahmeResponse uebernehmeVorschlaege(
      AngemeldeterNutzer nutzer, MandantContext mandant, String modusRoh, String ip) {
    Massenmodus modus = Massenmodus.ausText(modusRoh);
    List<UebernehmbareZeile> betroffen = repository.findeUebernehmbareVorschlaege(mandant);

    int regelA = zaehle(betroffen, VorschlagHerkunft.REGEL_A);
    int regelB = zaehle(betroffen, VorschlagHerkunft.REGEL_B);

    if (modus == Massenmodus.AUSFUEHREN) {
      if (!betroffen.isEmpty()) {
        List<String> kennungen = betroffen.stream().map(UebernehmbareZeile::processId).toList();
        int geschrieben =
            repository.uebernehmeVorschlaege(mandant, kennungen, jetztUtc(), nutzer.username());
        if (geschrieben != kennungen.size()) {
          // Nach aussen geht die Groesse der gelesenen Liste, damit Vorschau und Ausfuehrung
          // dieselbe Zahl nennen. Die Abweichung ist trotzdem ein Befund: Lesung und Schreiben
          // laufen auf verschiedenen Verbindungen (PROJEKTBESCHREIBUNG.md §6 — eine Lesung
          // innerhalb einer @Transactional-Methode ist nicht Teil dieser Transaktion).
          log.warn(
              "Vorschlagsuebernahme fuer Mandant {}: gelesen wurden {} Zeilen, das UPDATE meldet"
                  + " {}. Lesung und Schreiben laufen auf verschiedenen Verbindungen; die"
                  + " Abweichung ist ein Befund und kein Rauschen.",
              mandant.mandantId(),
              kennungen.size(),
              geschrieben);
        }
      }
      protokolliere(
          AuditEventType.KATALOG_VORSCHLAEGE_UEBERNOMMEN,
          nutzer,
          mandant,
          mandant.mandantId(),
          "Vorschlagsuebernahme: "
              + betroffen.size()
              + " Zeilen auf GEPFLEGT gesetzt (Regel A "
              + regelA
              + ", Regel B "
              + regelB
              + ")",
          ip);
    }
    return new VorschlagsuebernahmeResponse(modus, betroffen.size(), regelA, regelB);
  }

  private static int zaehle(List<UebernehmbareZeile> zeilen, VorschlagHerkunft herkunft) {
    return (int) zeilen.stream().filter(zeile -> zeile.vorschlagHerkunft() == herkunft).count();
  }

  /**
   * Leerraum ist keine Angabe. Ein Partner aus Leerzeichen waere ein gefuelltes Feld ohne Inhalt —
   * und damit ein dritter Zustand durch die Hintertuer.
   */
  private static String partnerOderNull(String roh) {
    if (roh == null || roh.isBlank()) {
      return null;
    }
    String wert = roh.trim();
    if (wert.length() > PARTNER_MAXLAENGE) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "partner-zu-lang",
          "Partner zu lang",
          "Ein Partnername darf hoechstens " + PARTNER_MAXLAENGE + " Zeichen haben.",
          "Partnername ueber der Spaltenbreite");
    }
    return wert;
  }

  /**
   * Der zu setzende Wert, je Feld verschieden geprueft: Der Partner ist freier Text, die Richtung
   * eine geschlossene Menge. Ein unbekannter Richtungswert ist {@code 400} und nicht
   * stillschweigend leer — sonst leerte ein Tippfehler ein ganzes Projekt.
   */
  private static String wertFuer(Zuordnungsfeld feld, String roh) {
    if (feld == Zuordnungsfeld.RICHTUNG) {
      Richtung richtung = Richtung.ausText(roh);
      return richtung == null ? null : richtung.name();
    }
    return partnerOderNull(roh);
  }

  /** Ein leeres Feld heisst im Protokoll „leer" und nicht {@code null}. */
  private static String beschriftung(String wert) {
    return wert == null ? "(leer)" : wert;
  }

  private LocalDateTime jetztUtc() {
    return LocalDateTime.ofInstant(systemClock.instant(), ZoneOffset.UTC);
  }

  /**
   * <b>Jede schreibende Katalogaenderung wird protokolliert</b>, nicht nur die einzelne Zuordnung:
   * Eine Massenzuordnung ueberschreibt bis zu 226 gepflegte Zeilen (E12) und ein Heuristik-Lauf
   * beruehrt jede offene Zeile eines Mandanten. Waeren die beiden nicht im Protokoll, waere
   * ausgerechnet die folgenreichste Aenderung die unsichtbarste.
   */
  private void protokolliere(
      AngemeldeterNutzer nutzer, MandantContext mandant, String ziel, String detail, String ip) {
    protokolliere(AuditEventType.KATALOG_GEAENDERT, nutzer, mandant, ziel, detail, ip);
  }

  /**
   * Dieselbe Zeile, aber mit ausdruecklich gewaehlter Ereignisart.
   *
   * <p>Die Uebernahme der Partnervorschlaege bekommt eine <b>eigene</b> Art (E24, {@link
   * AuditEventType#KATALOG_VORSCHLAEGE_UEBERNOMMEN}): Nach ihr ist „per Regel uebernommen" von „von
   * Hand kuratiert" in der Zeile selbst nicht mehr zu unterscheiden, und das Protokoll ist der
   * einzige Ort, an dem der Unterschied noch steht.
   */
  private void protokolliere(
      AuditEventType art,
      AngemeldeterNutzer nutzer,
      MandantContext mandant,
      String ziel,
      String detail,
      String ip) {
    auditLogWriter.schreibe(
        new AuditEvent(
            art,
            nutzer.id(),
            nutzer.username(),
            mandant.mandantId(),
            "process_catalog",
            ziel,
            ip,
            detail));
  }
}
