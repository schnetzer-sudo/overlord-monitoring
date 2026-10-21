package de.kraftwerkone.overlord.monitor.common.auftrag;

import de.kraftwerkone.overlord.monitor.common.MandantContext;
import de.kraftwerkone.overlord.monitor.common.error.RessourceNichtGefundenException;
import java.util.List;
import java.util.function.Predicate;

/**
 * <b>Die eine Stelle, die entscheidet, ob der Assistent fuer den aktiven Mandanten verfuegbar
 * ist</b> ({@code docs/tasks.md} §3.9, E‑1126, E‑1130): eingeschaltet — Schalter an <b>und</b>
 * Schluessel gesetzt (E‑1030) — und mindestens ein Typ, der dem Mandanten angeboten wird (aktiv,
 * freigeschaltet, gueltig, E‑1020) und den der Assistent anbieten kann (E‑1054).
 *
 * <p><b>Die Freischaltung ist datengetrieben</b> (E‑1126): Es gibt keinen zweiten Weg, keine Rolle
 * und keine Mandantenliste in der Konfiguration. Ein Mandant bekommt den Assistenten, sobald fuer
 * ihn eine Zeile in {@code task_type_mandant} steht.
 *
 * <p><b>Nicht verfuegbar ist {@code 404}</b>, wie bei einem nicht zulaessigen Mandanten: derselbe
 * feste Rumpf wie fuer jede unbekannte Ressource. Fragen die Gespraeche oder das Anlegen hier nach,
 * geht fuer einen nicht freigeschalteten Mandanten kein Aufruf an den Modellanbieter und kein
 * Auftrag in {@code task}.
 *
 * <p><b>Ausgeschaltet ohne Datenbankzugriff.</b> Sonst liest jede Entscheidung ein Statement, das
 * des Typkatalogs ({@code AuftragstypRepository.angeboteneTypen}, gemessen in M1000).
 *
 * <p><b>Liegt in {@code common}</b>, weil {@code task} und {@code assistent} sie brauchen und
 * einander nicht kennen. Was „eingeschaltet“ und „anbietbar“ heisst, weiss nur der Assistent;
 * beides reicht {@code config.AssistentConfig} herein.
 */
public class AssistentFreigabe {

  private final boolean eingeschaltet;
  private final TypKatalog typKatalog;
  private final Predicate<TypDefinition> anbietbar;

  /**
   * @param eingeschaltet Schalter an und Schluessel gesetzt
   * @param anbietbar ob der Assistent einen angebotenen Typ als Werkzeug anbieten kann
   */
  public AssistentFreigabe(
      boolean eingeschaltet, TypKatalog typKatalog, Predicate<TypDefinition> anbietbar) {
    this.eingeschaltet = eingeschaltet;
    this.typKatalog = typKatalog;
    this.anbietbar = anbietbar;
  }

  /**
   * Was der Assistent diesem Mandanten anbietet, nach Kennung sortiert. <b>Leer heisst nicht
   * verfuegbar</b> — fuer die Selbstauskunft des Assistenten, die nicht {@code 404} antwortet.
   */
  public List<TypDefinition> angebot(MandantContext mandant) {
    if (!eingeschaltet) {
      return List.of();
    }
    return anbietbare(typKatalog.angeboten(mandant));
  }

  /**
   * Wie {@link #angebot}, aber nicht verfuegbar ist {@code 404}. Fuer die Gespraeche.
   *
   * @throws RessourceNichtGefundenException wenn der Assistent fuer den Mandanten nicht verfuegbar
   *     ist
   */
  public List<TypDefinition> verlangeAngebot(MandantContext mandant) {
    List<TypDefinition> angebot = angebot(mandant);
    if (angebot.isEmpty()) {
      throw nichtVerfuegbar();
    }
    return angebot;
  }

  /**
   * <b>Der ganze Typkatalog des Mandanten</b>, wenn der Assistent fuer ihn verfuegbar ist; sonst
   * {@code 404}. Fuer das Anlegen und die Pruefung ohne Anlegen: Sie waehlen den Typ der Anfrage
   * aus dem Katalog und nicht aus dem Angebot des Assistenten, damit ein Typ, den der Assistent
   * nicht anbieten kann, anlegbar bleibt, solange der Mandant den Assistenten hat (E‑1054).
   *
   * @throws RessourceNichtGefundenException wenn der Assistent fuer den Mandanten nicht verfuegbar
   *     ist
   */
  public List<TypKatalog.Angebot> verlangeKatalog(MandantContext mandant) {
    if (!eingeschaltet) {
      throw nichtVerfuegbar();
    }
    List<TypKatalog.Angebot> katalog = typKatalog.angeboten(mandant);
    if (anbietbare(katalog).isEmpty()) {
      throw nichtVerfuegbar();
    }
    return katalog;
  }

  private List<TypDefinition> anbietbare(List<TypKatalog.Angebot> katalog) {
    return katalog.stream().map(TypKatalog.Angebot::definition).filter(anbietbar).toList();
  }

  private static RessourceNichtGefundenException nichtVerfuegbar() {
    return new RessourceNichtGefundenException(
        "Assistent fuer diesen Mandanten nicht verfuegbar (abgeschaltet, ohne Schluessel oder"
            + " ohne freigeschalteten Typ)");
  }
}
