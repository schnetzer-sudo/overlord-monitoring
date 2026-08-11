package de.kraftwerkone.overlord.monitor.message;

import de.kraftwerkone.overlord.monitor.common.Kettenrolle;
import de.kraftwerkone.overlord.monitor.common.MessageStatusClassifier;
import de.kraftwerkone.overlord.monitor.common.Seite;
import de.kraftwerkone.overlord.monitor.common.Seitenposition;
import de.kraftwerkone.overlord.monitor.common.Zeitpunkte;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Die Fachlogik der Verkettung: aufsteigen, absteigen, begrenzen.
 *
 * <p>Was hier <b>nicht</b> geschieht: klassifizieren und Rollen bestimmen. Die Einordnung eines
 * {@code MessageStatus} entsteht ausschliesslich im {@code MessageStatusClassifier}, die Rollen
 * ausschliesslich in {@code common/Kettenrollen}. Beides wird hier benutzt und nirgends nachgebaut
 * — sonst driftet es ueber die Ausbaustufen auseinander.
 */
@Service
public class KettenService {

  /**
   * Wie viele Ebenen der Aufstieg hoechstens aufloest.
   *
   * <p><b>Zehn, obwohl die tiefste gemessene Kette vier Glieder hat</b> (M30‑3: Stufe fuenf ist
   * ueber alle 214.330 Startzeilen aus Fenster B leer). Die Grenze schuetzt nicht vor Kosten — zehn
   * Ebenen kosten hochgerechnet unter fuenf Millisekunden —, sondern davor, dass eine kaputte Kette
   * den Aufruf nie beenden laesst. Sie greift in der Testkopie nie; dass sie trotzdem da ist, liegt
   * an derselben Ueberlegung wie beim Zyklusschutz: Die Kette entsteht durch Datenbank-Events, die
   * uns nicht gehoeren ({@code datenmodell.md} §6).
   */
  public static final int TIEFE_GRENZE = 10;

  /**
   * Wie viele Glieder der Abstieg hoechstens liefert.
   *
   * <p><b>Fuenfzig, weil 3.350 Zeilen in einem JSON-Rumpf keine Darstellung sind</b> (M30‑2). Die
   * Grenze begrenzt die <i>Antwort</i>, nicht die Arbeit der Datenbank: Das {@code LIMIT} greift
   * erst nach dem {@code filesort}, die 3.350 Zeilen werden also ohnehin gelesen (M30‑1). Sie ist
   * derselbe Wert wie {@link NachrichtenFilter#LIMIT_VORGABE} und trotzdem eine eigene Konstante —
   * die beiden begrenzen verschiedene Dinge und muessen sich nicht gemeinsam aendern.
   */
  public static final int BREITE_GRENZE = 50;

  private final KettenRepository kettenRepository;
  private final MessageStatusClassifier statusClassifier;
  private final Clock anwendungsuhr;

  KettenService(
      KettenRepository kettenRepository,
      MessageStatusClassifier statusClassifier,
      Clock anwendungsuhr) {
    this.kettenRepository = kettenRepository;
    this.statusClassifier = statusClassifier;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * Die Kette einer Nachricht: der Aufstieg vollstaendig, der Abstieg eine Ebene.
   *
   * @throws RessourceNichtGefundenException {@code 404} — sowohl fuer eine erfundene {@code
   *     MessageID} als auch fuer die eines fremden Mandanten, mit <b>derselben</b> Antwort. Beide
   *     Faelle sind dasselbe Statement mit null Zeilen; unterschieden sie sich, liesse sich der
   *     Bestand abfragen
   */
  public KetteResponse kette(MandantContext mandant, String messageId) {
    Kettengliedzeile start = erforderlichesGlied(mandant, messageId);
    Aufstieg aufstieg = steigeAuf(mandant, start);

    List<Abwaertsglied> gelesen = leseAbwaerts(mandant, messageId, null, BREITE_GRENZE);
    List<Abwaertsglied> seite =
        gelesen.size() > BREITE_GRENZE ? gelesen.subList(0, BREITE_GRENZE) : gelesen;
    List<KettengliedResponse> abwaerts = seite.stream().map(this::abwaertsGlied).toList();
    int gesamt = zaehleAbwaerts(mandant, messageId);
    boolean weitereVorhanden = gesamt > abwaerts.size();

    return new KetteResponse(
        start.messageId(),
        List.copyOf(start.rollen()),
        aufstieg.glieder(),
        abwaerts,
        gesamt,
        weitereVorhanden ? abwaertsCursor(seite) : null,
        weitereVorhanden,
        aufstieg.tiefeErreicht(),
        aufstieg.zyklusErkannt());
  }

  /**
   * Die Position, ab der {@link #abwaerts} weiterblaettert — <b>die letzte hier ausgelieferte
   * Zeile</b>, kodiert von derselben Stelle wie die Cursor des Blaetter-Endpunkts.
   *
   * <p><b>Eine Kodierung und nicht zwei.</b> Beide Wege gehen ueber {@link #position} und {@link
   * Seitenposition#kodiere()}; der Blaetter-Endpunkt kommt nur ueber {@link Seite#aus} dorthin. Ein
   * zweiter Kodierer waere genau die Drift, die das Blaettern zerlegt — er faellt erst auf, wenn
   * eine Seite Zeilen ueberspringt oder wiederholt.
   *
   * <p><b>{@code null}, wenn die letzte Zeile keinen Zeitpunkt traegt.</b> Sie hat in der Ordnung
   * {@code (MessageLastUpdate, MessageID)} keine Position — anders als beim Blaetter-Endpunkt ist
   * das hier <i>kein</i> technischer Fehler: Die Kette selbst ist vollstaendig beantwortet, nur
   * weiterblaettern laesst sich nicht. Der Aufrufer bekommt seine Antwort und keine Schaltflaeche.
   * Gemessen kommt der Fall {@code 0} von 3.341.519 Mal vor (M30‑6).
   *
   * <p>Die leere Seite ist mitbehandelt, obwohl sie neben {@code weitereVorhanden} nicht auftreten
   * kann: Zaehlung und Zeilen tragen denselben Filter. Waere sie doch einmal leer, waere ein
   * fehlender Cursor die richtige Auskunft und keine Ausnahme.
   */
  private static String abwaertsCursor(List<Abwaertsglied> seite) {
    if (seite.isEmpty()) {
      return null;
    }
    Seitenposition position = position(seite.get(seite.size() - 1));
    return position == null ? null : position.kodiere();
  }

  /**
   * Eine weitere Seite der Abwaertsglieder — <b>innerhalb der festen Wurzel</b>, cursor-basiert
   * ueber {@code (MessageLastUpdate, MessageID)}.
   *
   * <p><b>Kein Zeitfenster</b> (Regel L1 gilt fuer Listen ueber {@code Message}): Die Menge ist
   * durch die Wurzel benannt, und ein Fenster schnitte gerade die Kinder ab, die ausserhalb liegen.
   *
   * <p><b>Warum ein eigener Endpunkt und kein Filter an der Liste.</b> Naheliegend waere {@code GET
   * /api/nachrichten?wurzel=…}. Das braeche Regel L1: Die Liste verlangt ein Pflicht-Zeitfenster,
   * und die Kinder einer drei Monate alten Wurzel laegen ausserhalb jedes vernuenftigen Fensters.
   * Entweder man hoehlte L1 fuer einen Sonderfall aus, oder die Liste lieferte fuer eine gueltige
   * Anfrage nichts. Der eigene Endpunkt umgeht beides — und er ist der billigere Zugriff, weil er
   * ueber {@code SourceMessageIDIDX} einsteigt statt ueber {@code MessageLastUpdateIDX}.
   *
   * <p><b>Die Existenz wird zuerst geprueft</b> und nicht aus der Zeilenzahl geschlossen: Eine
   * leere Liste waere eine andere Auskunft als {@code 404} und damit eine Auskunft ueber den
   * Bestand. Dieselbe Reihenfolge wie beim Eigenschaften-Endpunkt.
   */
  public Seite<KettengliedResponse> abwaerts(
      MandantContext mandant, String messageId, String cursor, Integer limit) {
    erforderlichesGlied(mandant, messageId);
    int seitengroesse = seitengroesse(limit);
    Seitenposition ab = cursor == null ? null : Seitenposition.dekodiere(cursor);

    List<Abwaertsglied> gelesen = leseAbwaerts(mandant, messageId, ab, seitengroesse);
    Seite<Abwaertsglied> seite =
        Seite.aus(gelesen, seitengroesse, KettenService::erforderlichePosition);

    return new Seite<>(
        seite.items().stream().map(this::abwaertsGlied).toList(),
        seite.nextCursor(),
        seite.hasMore());
  }

  /**
   * Die Cursor-Position eines Glieds — <b>die eine Ableitung, aus der beide Cursor entstehen</b>:
   * der {@code nextCursor} des Blaetter-Endpunkts und der {@code abwaertsCursor} der
   * Ketten-Antwort.
   *
   * <p>Der Sortierschluessel ist {@code (MessageLastUpdate, MessageID)}. Eine Zeile <b>ohne</b>
   * {@code MessageLastUpdate} laesst sich damit nicht adressieren — die Spalte ist der halbe
   * Schluessel; fuer sie ist die Position {@code null}. Gemessen kommt das nicht vor: {@code 0} von
   * 3.341.519 Zeilen (M30‑6). Die Spalte laesst es aber zu, und die Produktion muss sich nicht
   * daran halten, was die Testkopie zufaellig enthaelt.
   *
   * <p><b>Warum der Fall trotzdem fast nie durchschlaegt.</b> {@code null} sortiert <b>zuerst</b> —
   * in MariaDB wie in {@link #SORTIERUNG}. Eine Zeile ohne Zeitpunkt steht damit am <i>Anfang</i>
   * der Ordnung und wird auf der ersten Seite <b>ausgeliefert</b>; der Cursor entsteht aus der
   * <i>letzten</i> Zeile der Seite, und die traegt einen Zeitpunkt, sobald irgendeine Zeile der
   * Seite einen traegt. Erst wenn eine <b>ganze Seite</b> aus Zeilen ohne Zeitpunkt bestuende, gibt
   * es keine Position, an der weitergeblaettert werden koennte.
   *
   * <p><b>Was dann geschieht, haengt am Endpunkt</b> — und die beiden Faelle sind verschieden. Der
   * Blaetter-Endpunkt kann seine Zusage nicht halten und meldet das ({@link
   * #erforderlichePosition}); die Ketten-Antwort ist vollstaendig und laesst nur den Cursor weg
   * ({@link #abwaertsCursor}).
   */
  private static Seitenposition position(Abwaertsglied eintrag) {
    if (eintrag.zeile().zeitpunkt() == null) {
      return null;
    }
    return new Seitenposition(eintrag.zeile().zeitpunkt(), eintrag.zeile().messageId());
  }

  /**
   * Dieselbe Position, aber <b>verbindlich</b> — fuer {@link Seite#aus}, das ohne sie keinen Cursor
   * bilden kann.
   *
   * <p><b>Eine ganze Seite ohne Zeitpunkt ist hier ein technischer Fehler und keine fachliche
   * Antwort.</b> Ein {@code 4xx} legte dem Aufrufer eine Verhaltensaenderung nahe, die nichts
   * aendern wuerde — er hat richtig gefragt. Ein {@code 500} mit {@code traceId} und Stacktrace ist
   * die richtige Auskunft: Die Quelle verletzt eine dokumentierte Zusicherung, und das gehoert ins
   * Protokoll und nicht in eine beschoenigte Seite. Die Meldung nennt die Zusicherung, damit im
   * Protokoll steht, <i>welche</i> gebrochen ist — sonst stuende dort nur der allgemeine Satz aus
   * {@link Seitenposition}.
   */
  private static Seitenposition erforderlichePosition(Abwaertsglied eintrag) {
    Seitenposition position = position(eintrag);
    if (position == null) {
      throw new IllegalStateException(
          "Kettenglied ohne MessageLastUpdate — der Sortierschluessel der Abwaertsglieder ist"
              + " (MessageLastUpdate, MessageID), eine Zeile ohne Zeitpunkt hat darin keine"
              + " Position. Gemessen kommt das nicht vor (M30-6: 0 von 3.341.519).");
    }
    return position;
  }

  /**
   * Das Ergebnis des Aufstiegs — die Glieder und die beiden Gruende, aus denen er abgebrochen sein
   * kann.
   */
  private record Aufstieg(
      List<KettengliedResponse> glieder, boolean tiefeErreicht, boolean zyklusErkannt) {}

  /**
   * Der Aufstieg: je Ebene ein Primaerschluesselzugriff, bis die Wurzel erreicht ist oder eine
   * Grenze greift.
   *
   * <p><b>Je Ebene gibt es genau ein Glied darueber</b>, weil keine Zeile beide ID-Spalten traegt
   * (M28‑1). Der Aufstieg ist deshalb ein Weg und kein Baum — anders als der Abstieg, der
   * auffaechert.
   *
   * <p><b>„Aufwaerts" ist der Mechanismus und nicht die Bedeutung.</b> Beim Split-Kind fuehrt der
   * Aufstieg zur Wurzel, also gegen den Datenfluss; beim Merge-Eingang folgt er {@code
   * TargetMessageID} und fuehrt damit zum <i>Ergebnis</i>, also <b>mit</b> dem Fluss. Was ein Glied
   * bedeutet, sagen {@code beziehung} und {@code ebene} — nicht die Liste, in der es steht.
   *
   * <p><b>Der Zyklus wird vor der Tiefe geprueft.</b> Traefen beide zu, waere „tief" die
   * schwaechere Auskunft: Sie beschreibt, wo abgebrochen wurde, nicht warum. Genau dafuer gibt es
   * zwei Felder und nicht eines.
   *
   * <p><b>Ein Glied, das nicht aufloest, beendet den Aufstieg still.</b> Das ist einer von zwei
   * Faellen, und beide sind richtig so behandelt: ein Verweis ins Leere (M24‑1 hat null gefunden,
   * die Quelle erzwingt es aber nicht) oder ein Glied eines <i>fremden</i> Mandanten — den darf es
   * fuer diesen Aufrufer nicht geben, auch nicht als Hinweis „hier geht es weiter". M27 hat
   * gemessen, dass der zweite Fall nicht vorkommt; der Filter steht trotzdem in jedem Statement.
   */
  private Aufstieg steigeAuf(MandantContext mandant, Kettengliedzeile start) {
    List<KettengliedResponse> glieder = new ArrayList<>();
    Set<String> besucht = new HashSet<>();
    besucht.add(start.messageId());

    Kettengliedzeile aktuell = start;
    for (int ebene = 1; ; ebene++) {
      String aufwaertsId = aktuell.aufwaertsId();
      if (aufwaertsId == null) {
        return new Aufstieg(List.copyOf(glieder), false, false);
      }
      if (!besucht.add(aufwaertsId)) {
        return new Aufstieg(List.copyOf(glieder), false, true);
      }
      if (ebene > TIEFE_GRENZE) {
        return new Aufstieg(List.copyOf(glieder), true, false);
      }
      Kettenbeziehung beziehung = aktuell.aufwaertsBeziehung();
      Kettengliedzeile darueber = kettenRepository.findeGlied(mandant, aufwaertsId);
      if (darueber == null) {
        return new Aufstieg(List.copyOf(glieder), false, false);
      }
      glieder.add(glied(darueber, -ebene, beziehung));
      aktuell = darueber;
    }
  }

  /**
   * Beide Abwaertsrichtungen in einer Liste, gemeinsam sortiert.
   *
   * <p><b>Zwei Statements und eine Zusammenfuehrung in Java, statt eines {@code UNION}.</b> Jedes
   * der beiden ist fuer sich ueber seinen Index erklaerbar und einzeln gemessen (M30‑1); ein {@code
   * UNION} waere ein Plan, den man als Ganzes messen muesste und der bei jeder Aenderung neu
   * einzuschaetzen waere. Die Zusammenfuehrung ist billig: Beide Zweige kommen bereits sortiert und
   * sind auf {@code limit + 1} begrenzt.
   *
   * <p><b>Die beiden Mengen sind ueberschneidungsfrei.</b> Ein Glied gehoerte nur dann zu beiden,
   * wenn es {@code SourceMessageID} und {@code TargetMessageID} auf denselben Wert gesetzt haette —
   * und keine Zeile traegt beide Spalten (M28‑1). Doppelt gezaehlt wird deshalb nichts, weder hier
   * noch in {@link #zaehleAbwaerts}.
   *
   * <p><b>Beide Richtungen werden immer gefragt, auch wenn die Flags {@code Source} und {@code
   * Target} sagen, es gaebe nichts.</b> E4 hat gemessen, dass die Flags sich exakt mit der
   * Verkettung decken — das macht sie zur richtigen Grundlage fuer die <i>Liste</i>, die ohne
   * Abfrage entscheiden muss, ob ein Ketten-Hinweis erscheint. Hier waeren sie ein Sprung ueber
   * einen Zugriff von einer halben Millisekunde, erkauft mit dem Vertrauen in eine Beobachtung. Und
   * er haette eine Nebenwirkung: {@link #zaehleAbwaerts} muesste dieselbe Abkuerzung nehmen, sonst
   * nennte die Antwort eine Zahl, zu der sie keine Zeilen liefert.
   */
  private List<Abwaertsglied> leseAbwaerts(
      MandantContext mandant, String messageId, Seitenposition ab, int limit) {
    List<Abwaertsglied> zusammen = new ArrayList<>();
    for (Kettengliedzeile kind : kettenRepository.findeKinder(mandant, messageId, ab, limit)) {
      zusammen.add(new Abwaertsglied(kind, Kettenbeziehung.AUFTEILUNG));
    }
    for (Kettengliedzeile eingang :
        kettenRepository.findeMergeEingaenge(mandant, messageId, ab, limit)) {
      zusammen.add(new Abwaertsglied(eingang, Kettenbeziehung.ZUSAMMENFUEHRUNG));
    }
    zusammen.sort(SORTIERUNG);
    return zusammen.size() > limit + 1
        ? List.copyOf(zusammen.subList(0, limit + 1))
        : List.copyOf(zusammen);
  }

  /**
   * Ein Glied des Abstiegs samt der Beziehung, ueber die es gefunden wurde.
   *
   * <p><b>Die Beziehung wird mitgefuehrt und nicht aus der Zeile abgelesen.</b> Sie steht fest,
   * sobald das Statement gewaehlt ist: Was ueber {@code SourceMessageID} gefunden wurde, ist ein
   * Kind; was ueber {@code TargetMessageID} gefunden wurde, ein Merge-Eingang. Sie <i>liesse</i>
   * sich aus den Spalten der Zeile ableiten, weil nie beide belegt sind (M28‑1) — aber das ist eine
   * Messung und keine Garantie, und eine Zeile mit beiden Spalten bekaeme dann in der einen der
   * beiden Listen das falsche Etikett.
   */
  private record Abwaertsglied(Kettengliedzeile zeile, Kettenbeziehung beziehung) {}

  /**
   * Die Gesamtzahl beider Abwaertsrichtungen — <b>mit demselben Mandantenfilter wie die Zeilen</b>
   * (Regel M5).
   */
  private int zaehleAbwaerts(MandantContext mandant, String messageId) {
    return kettenRepository.zaehleKinder(mandant, messageId)
        + kettenRepository.zaehleMergeEingaenge(mandant, messageId);
  }

  /**
   * Die Ordnung der Abwaertsglieder: {@code (MessageLastUpdate, MessageID)} — derselbe
   * Sortierschluessel wie in der Liste, damit derselbe Cursor-Typ passt.
   *
   * <p><b>{@code null} zuerst</b>, weil MariaDB es in {@code ORDER BY … ASC} so haelt: Die
   * Zusammenfuehrung in Java muss dieselbe Ordnung haben wie die beiden Statements, sonst stimmt
   * die Seitengrenze nicht mehr mit dem Cursor ueberein. Ein {@code MessageLastUpdate} ohne Wert
   * kommt in der Testkopie <b>kein einziges Mal</b> vor (M30‑6: {@code 0} von 3.341.519), die
   * Spalte laesst es aber zu.
   *
   * <p><b>Die Reihenfolge ist hier zugleich der Schutz.</b> Weil {@code null} vorn steht, wird eine
   * solche Zeile ausgeliefert, ohne den Cursor zu beruehren — der entsteht aus der <i>letzten</i>
   * Zeile der Seite. Stuende {@code null} hinten, braeche schon eine einzelne Zeile ohne Zeitpunkt
   * das Blaettern. Was im verbleibenden Fall geschieht, steht bei {@link #position(Abwaertsglied)}.
   */
  private static final Comparator<Abwaertsglied> SORTIERUNG =
      Comparator.comparing(
              (Abwaertsglied eintrag) -> eintrag.zeile().zeitpunkt(),
              Comparator.nullsFirst(Comparator.naturalOrder()))
          .thenComparing(eintrag -> eintrag.zeile().messageId());

  /** Ein Glied des Abstiegs — immer Ebene {@code +1}. */
  private KettengliedResponse abwaertsGlied(Abwaertsglied eintrag) {
    return glied(eintrag.zeile(), 1, eintrag.beziehung());
  }

  private KettengliedResponse glied(Kettengliedzeile zeile, int ebene, Kettenbeziehung beziehung) {
    ZoneId zone = anwendungsuhr.getZone();
    List<Kettenrolle> rollen = List.copyOf(zeile.rollen());
    return new KettengliedResponse(
        zeile.messageId(),
        zeile.status(),
        statusClassifier.einordnung(zeile.status()).name(),
        Zeitpunkte.nachUtc(zeile.zeitpunkt(), zone),
        zeile.sosName(),
        rollen,
        ebene,
        beziehung);
  }

  /**
   * Die angefragte Nachricht — oder {@code 404}. Der Text nennt keinen Grund; er geht
   * ausschliesslich ins Protokoll.
   */
  private Kettengliedzeile erforderlichesGlied(MandantContext mandant, String messageId) {
    Kettengliedzeile glied = kettenRepository.findeGlied(mandant, messageId);
    if (glied == null) {
      throw new RessourceNichtGefundenException("Nachricht nicht sichtbar oder nicht vorhanden");
    }
    return glied;
  }

  /**
   * Die Seitengroesse des Blaetter-Endpunkts — Vorgabe {@link #BREITE_GRENZE}, Maximum wie in der
   * Liste.
   */
  private static int seitengroesse(Integer limit) {
    if (limit == null) {
      return BREITE_GRENZE;
    }
    if (limit < 1 || limit > NachrichtenFilter.LIMIT_MAXIMUM) {
      throw new FachlicheAusnahme(
          HttpStatus.BAD_REQUEST,
          "limit-ungueltig",
          "Seitengroesse ungueltig",
          "Die Seitengroesse muss zwischen 1 und " + NachrichtenFilter.LIMIT_MAXIMUM + " liegen.",
          "Seitengroesse ausserhalb des zulaessigen Bereichs");
    }
    return limit;
  }
}
