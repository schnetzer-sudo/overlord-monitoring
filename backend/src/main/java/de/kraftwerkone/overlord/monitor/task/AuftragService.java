package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.audit.AuditEvent;
import de.kraftwerkone.overlord.monitor.audit.AuditEventType;
import de.kraftwerkone.overlord.monitor.audit.AuditLogWriter;
import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.error.FachlicheAusnahme;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.task.AuftragRepository.Auftragszeile;
import de.kraftwerkone.overlord.monitor.task.AuftragRepository.NeuerAuftrag;
import de.kraftwerkone.overlord.monitor.task.AuftragResponse.EintragResponse;
import de.kraftwerkone.overlord.monitor.task.Rueckgabe.Zeile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Die Fachlogik der Auftraege: anbieten, pruefen, anlegen, lesen ({@code docs/tasks.md} §6).
 *
 * <p><b>Der Monitor schreibt den Status nie ueber {@code OFFEN} hinaus.</b> Abholen und
 * Zurueckmelden sind Sache der Plattform ({@code docs/task-schnittstelle.md}).
 */
@Service
public class AuftragService {

  /** Hoechstzahl der Auftraege in {@code GET /api/tasks}. */
  static final int LISTE_HOECHSTENS = 50;

  /** {@code task.parameter} ist {@code TEXT}. */
  static final int PARAMETER_HOECHSTENS_BYTES = 65_535;

  static final String PROBLEMTYP = "eingabe-ungueltig";

  private static final Pattern UUID_FORM =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  private static final TypeReference<List<LinkedHashMap<String, String>>> EINTRAGSLISTE =
      new TypeReference<>() {};

  private final AuftragRepository repository;
  private final TypKatalog typKatalog;
  private final AuditLogWriter auditLogWriter;
  private final JsonMapper json;

  AuftragService(
      AuftragRepository repository,
      TypKatalog typKatalog,
      AuditLogWriter auditLogWriter,
      JsonMapper json) {
    this.repository = repository;
    this.typKatalog = typKatalog;
    this.auditLogWriter = auditLogWriter;
    this.json = json;
  }

  /** Das Ergebnis des Anlegens: der Auftrag und ob er neu ist ({@code 201}) oder schon da war. */
  public record Anlage(AuftragResponse auftrag, boolean neu) {}

  public List<TypResponse> typen(MandantContext mandant) {
    return typKatalog.angeboten(mandant).stream()
        .map(angebot -> TypResponse.aus(angebot.definition()))
        .toList();
  }

  /**
   * Legt einen Auftrag an. Die Reihenfolge ist Absicht ({@code docs/tasks.md} §6.2): erst die
   * {@code anfrageId}, dann die Wiederholung, dann der Typ, dann die Eintraege.
   *
   * <p><b>Ein nicht angebotener Typ ist {@code 404}</b>, gleich ob unbekannt, inaktiv, ungueltig
   * oder fuer diesen Mandanten nicht freigeschaltet — dasselbe Statement, derselbe Rumpf. Sonst
   * liesse sich die Freischaltung anderer Mandanten abfragen.
   */
  public Anlage lege(
      AngemeldeterNutzer nutzer, MandantContext mandant, AuftragRequest anfrage, String ip) {
    String anfrageId = anfrageId(anfrage.anfrageId());

    Optional<Auftragszeile> vorhanden = repository.findeNachAnfrage(mandant, anfrageId);
    if (vorhanden.isPresent()) {
      return new Anlage(antwort(vorhanden.get()), false);
    }

    TypKatalog.Angebot angebot =
        typKatalog
            .angeboten(mandant, anfrage.typ())
            .orElseThrow(
                () ->
                    new RessourceNichtGefundenException(
                        "Auftragstyp fuer diesen Mandanten nicht angeboten: " + anfrage.typ()));
    TypDefinition definition = angebot.definition();

    List<Map<String, String>> eintraege =
        anfrage.eintraege() == null ? List.of() : anfrage.eintraege();
    List<Prueffehler> fehler = AuftragsPruefung.pruefe(definition, anfrage.aktion(), eintraege);
    if (!fehler.isEmpty()) {
      throw abgelehnt(fehler, "Pruefung des Auftrags: " + fehler.size() + " Fehler");
    }

    List<Map<String, String>> geprueft = geordnet(definition, eintraege);
    String parameter = Parametertext.baue(definition, anfrage.aktion(), geprueft);
    if (parameter.getBytes(StandardCharsets.UTF_8).length > PARAMETER_HOECHSTENS_BYTES) {
      throw abgelehnt(
          List.of(
              new Prueffehler(
                  null,
                  null,
                  Fehlercode.ZU_LANG,
                  "Der Auftrag ist zu umfangreich. Teile ihn auf mehrere Aufträge auf.")),
          "Parametertext ueber " + PARAMETER_HOECHSTENS_BYTES + " Bytes");
    }

    String taskId = UUID.randomUUID().toString();
    boolean angelegt =
        repository.legeAn(
            mandant,
            new NeuerAuftrag(
                taskId,
                definition.typ(),
                kopie(angebot),
                anfrage.aktion(),
                json.writeValueAsString(geprueft),
                anfrageId,
                parameter,
                nutzer.username()));
    if (!angelegt) {
      // Eine gleiche Anfrage war schneller. Ihr Auftrag ist der Auftrag dieser Anfrage.
      return new Anlage(
          antwort(
              repository
                  .findeNachAnfrage(mandant, anfrageId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Doppelte anfrageId gemeldet, aber kein Auftrag zu lesen"))),
          false);
    }

    auditLogWriter.schreibe(
        new AuditEvent(
            AuditEventType.AUFTRAG_ANGELEGT,
            nutzer.id(),
            nutzer.username(),
            mandant.mandantId(),
            "task",
            taskId,
            ip,
            "Typ "
                + definition.typ()
                + ", Aktion "
                + anfrage.aktion()
                + ", "
                + geprueft.size()
                + (geprueft.size() == 1 ? " Eintrag" : " Einträge")));

    return new Anlage(
        antwort(
            repository
                .finde(mandant, taskId)
                .orElseThrow(
                    () -> new IllegalStateException("Eben angelegter Auftrag nicht lesbar"))),
        true);
  }

  /** Die laufenden Auftraege und die der letzten 7 Tage, neueste zuerst. */
  public List<AuftragResponse> aktuelle(MandantContext mandant) {
    return repository.findeAktuelle(mandant, LISTE_HOECHSTENS).stream().map(this::antwort).toList();
  }

  /** Ein Auftrag. Fremd und unbekannt sind ununterscheidbar {@code 404}. */
  public AuftragResponse einzeln(MandantContext mandant, String taskId) {
    return repository
        .finde(mandant, taskId)
        .map(this::antwort)
        .orElseThrow(
            () -> new RessourceNichtGefundenException("Auftrag nicht vorhanden oder fremd"));
  }

  // ─── Anlegen ───────────────────────────────────────────────────────────────────────────────

  /** Eine UUID in beliebiger Schreibweise; gespeichert wird sie klein. */
  private static String anfrageId(String roh) {
    if (!TypDefinition.hatWert(roh)) {
      throw abgelehnt(
          List.of(
              new Prueffehler(
                  null, "anfrageId", Fehlercode.PFLICHT_FEHLT, "Die Kennung der Anfrage fehlt.")),
          "anfrageId fehlt");
    }
    if (!UUID_FORM.matcher(roh).matches()) {
      throw abgelehnt(
          List.of(
              new Prueffehler(
                  null, "anfrageId", Fehlercode.FORMAT, "Die Kennung der Anfrage ist keine UUID.")),
          "anfrageId ist keine UUID");
    }
    return roh.toLowerCase(Locale.ROOT);
  }

  /**
   * Die Eintraege, wie sie gespeichert werden: nur Felder mit Wert, in der Reihenfolge der
   * Definition. Kein Wert wird veraendert. Ein {@code null}-Eintrag ist ein leerer, wie in der
   * Pruefung — bei einem Typ ohne Pflichtfeld kommt er bis hierher.
   */
  private static List<Map<String, String>> geordnet(
      TypDefinition definition, List<Map<String, String>> eintraege) {
    List<Map<String, String>> geordnet = new ArrayList<>();
    for (Map<String, String> eintrag : eintraege) {
      Map<String, String> werte = new LinkedHashMap<>();
      for (TypDefinition.Feld feld : definition.felder()) {
        String wert = eintrag == null ? null : eintrag.get(feld.name());
        if (TypDefinition.hatWert(wert)) {
          werte.put(feld.name(), wert);
        }
      }
      geordnet.add(werte);
    }
    return geordnet;
  }

  /**
   * Die Kopie von Name und Struktur, mit der der Auftrag angelegt wird (E‑1023). Die Struktur kommt
   * inhaltlich unveraendert aus der Typzeile — dieselbe Fassung, die eben geprueft wurde —, nicht
   * aus der gelesenen Definition neu geschrieben.
   */
  private String kopie(TypKatalog.Angebot angebot) {
    ObjectNode kopie = json.createObjectNode();
    kopie.put("name", angebot.definition().name());
    kopie.set("struktur", json.readTree(angebot.struktur()));
    return json.writeValueAsString(kopie);
  }

  private static FachlicheAusnahme abgelehnt(List<Prueffehler> fehler, String interneUrsache) {
    return new FachlicheAusnahme(
        HttpStatus.BAD_REQUEST,
        PROBLEMTYP,
        "Auftrag nicht angenommen",
        "Bitte prüfe die genannten Angaben.",
        interneUrsache,
        Map.of("errors", fehler));
  }

  // ─── Lesen ─────────────────────────────────────────────────────────────────────────────────

  /**
   * Die Antwort aus der Zeile und ihrer <b>Kopie</b> der Definition. Die Kopie wird auch gelesen,
   * wenn eine spaetere Fassung dieses Codes sie strenger pruefte — sie war gueltig, als der Auftrag
   * entstand.
   */
  private AuftragResponse antwort(Auftragszeile zeile) {
    JsonNode kopie = json.readTree(zeile.definition());
    JsonNode name = kopie.get("name");
    TypDefinition definition =
        DefinitionsPruefung.pruefe(
                zeile.typ(),
                name != null && name.isString() ? name.stringValue() : null,
                null,
                kopie.get("struktur"))
            .definition();

    List<LinkedHashMap<String, String>> eintraege =
        json.readValue(zeile.eintraege(), EINTRAGSLISTE);
    Optional<Rueckgabe> rueckgabe = Rueckgabe.lies(zeile.rueckgabe(), definition.rueckgaben());
    List<Zeile> zeilen = rueckgabe.map(Rueckgabe::zeilen).orElse(List.of());

    List<EintragResponse> antworten = new ArrayList<>();
    for (int index = 0; index < eintraege.size(); index++) {
      Map<String, String> werte = eintraege.get(index);
      Zeile rueck = index < zeilen.size() ? zeilen.get(index) : null;
      antworten.add(
          new EintragResponse(
              werte,
              definition.schluesselText(werte),
              rueck == null ? null : rueck.wert(),
              rueck == null ? null : rueck.ergebnis()));
    }

    return new AuftragResponse(
        zeile.taskId(),
        zeile.typ(),
        definition.name(),
        zeile.aktion(),
        zeile.status(),
        TaskStatusClassifier.einordnen(zeile.status(), rueckgabe, eintraege.size()),
        utc(zeile.eingestelltAm()),
        utc(zeile.abgeholtAm()),
        utc(zeile.ausgefuehrtAm()),
        zeile.fehlercode(),
        zeile.fehlertext(),
        antworten);
  }

  /** Im eigenen Schema steht UTC; die Datenbank hat es mit {@code UTC_TIMESTAMP(3)} gesetzt. */
  private static Instant utc(LocalDateTime wert) {
    return wert == null ? null : wert.toInstant(ZoneOffset.UTC);
  }
}
