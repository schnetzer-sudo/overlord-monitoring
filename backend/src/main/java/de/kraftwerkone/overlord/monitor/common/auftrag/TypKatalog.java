package de.kraftwerkone.overlord.monitor.common.auftrag;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.auftrag.AuftragstypRepository.Typzeile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Die Typen, die einem Mandanten angeboten werden: aktiv, fuer ihn freigeschaltet und gueltig
 * (E‑1020, {@code docs/tasks.md} §5.6). Was nicht angeboten wird, existiert fuer den Mandanten
 * nicht — {@code 404} beim Anlegen, nicht in der Typliste.
 *
 * <p><b>Aenderungen per SQL wirken ohne Neustart</b> (E‑1019). Gelesen wird bei jedem Zugriff, mit
 * einem Statement; geprueft wird eine Definition nur einmal je {@code (task_typ, geaendert_am)}.
 * {@code geaendert_am} setzt die Datenbank bei jeder Aenderung der Zeile ({@code ON UPDATE}), eine
 * geaenderte, neue oder geloeschte Zeile wirkt damit beim naechsten Zugriff.
 *
 * <p><b>Eine ungueltige Definition steht einmal je Fassung als {@code WARN} im Log</b>, mit allen
 * Gruenden — nicht bei jedem Zugriff. Sichtbar ist sie nur dort (Punkt 1005).
 *
 * <p><b>Kein Zwischenspeicher mit Ablaufzeit:</b> Er hielte eine deaktivierte Definition bis zum
 * Ablauf im Angebot.
 *
 * <p><b>Liegt in {@code common}</b>, weil ihn neben {@code task} der Assistent braucht (E‑1037,
 * {@code docs/assistent.md} §4.1).
 */
@Component
public class TypKatalog {

  private static final Logger log = LoggerFactory.getLogger(TypKatalog.class);

  private final AuftragstypRepository repository;
  private final JsonMapper json;

  /** Je Typ die zuletzt gepruefte Fassung. Eine geaenderte Zeile ersetzt ihren Eintrag. */
  private final ConcurrentMap<String, Fassung> geprueft = new ConcurrentHashMap<>();

  private record Fassung(LocalDateTime geaendertAm, DefinitionsPruefung.Ergebnis ergebnis) {}

  /**
   * Ein angebotener Typ samt seiner Struktur, wie sie in der Zeile steht — fuer die Kopie im
   * Auftrag (E‑1023).
   */
  public record Angebot(TypDefinition definition, String struktur) {}

  TypKatalog(AuftragstypRepository repository, JsonMapper json) {
    this.repository = repository;
    this.json = json;
  }

  /** Die angebotenen Typen des Mandanten, nach Kennung sortiert. */
  public List<Angebot> angeboten(MandantContext mandant) {
    List<Angebot> angeboten = new ArrayList<>();
    for (Typzeile zeile : repository.angeboteneTypen(mandant)) {
      DefinitionsPruefung.Ergebnis ergebnis = geprueft(zeile);
      if (ergebnis.gueltig()) {
        angeboten.add(new Angebot(ergebnis.definition(), zeile.struktur()));
      }
    }
    return angeboten;
  }

  /**
   * Ein angebotener Typ. Unbekannt, inaktiv, ungueltig und nicht freigeschaltet sind dasselbe:
   * dasselbe Statement, dieselbe leere Antwort. Die Kennung wird genau verglichen.
   */
  public Optional<Angebot> angeboten(MandantContext mandant, String typ) {
    return angeboten(mandant).stream()
        .filter(angebot -> angebot.definition().typ().equals(typ))
        .findFirst();
  }

  private DefinitionsPruefung.Ergebnis geprueft(Typzeile zeile) {
    Fassung bekannt = geprueft.get(zeile.typ());
    if (bekannt != null && bekannt.geaendertAm().equals(zeile.geaendertAm())) {
      return bekannt.ergebnis();
    }
    DefinitionsPruefung.Ergebnis ergebnis =
        DefinitionsPruefung.pruefe(
            zeile.typ(), zeile.name(), zeile.beschreibung(), lies(zeile.struktur()));
    if (!ergebnis.gueltig()) {
      log.warn(
          "Auftragstyp {} (Fassung vom {}) ist ungueltig und wird nicht angeboten: {}",
          zeile.typ(),
          zeile.geaendertAm(),
          String.join(" | ", ergebnis.gruende()));
    }
    geprueft.put(zeile.typ(), new Fassung(zeile.geaendertAm(), ergebnis));
    return ergebnis;
  }

  /** {@code JSON_VALID} haelt die Spalte gueltig; {@code null} meldet die Pruefung als Grund. */
  private JsonNode lies(String struktur) {
    if (struktur == null) {
      return null;
    }
    try {
      return json.readTree(struktur);
    } catch (JacksonException fehler) {
      return null;
    }
  }
}
