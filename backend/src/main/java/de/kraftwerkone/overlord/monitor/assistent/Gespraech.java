package de.kraftwerkone.overlord.monitor.assistent;

import de.kraftwerkone.overlord.monitor.assistent.modell.Nachricht;
import de.kraftwerkone.overlord.monitor.assistent.modell.Werkzeug;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ein laufendes Gespraech, nur im Arbeitsspeicher (E‑1038).
 *
 * <p><b>Gebunden an Sitzung, Nutzer und aktiven Mandanten.</b> Wer es sucht, muss alle drei nennen
 * ({@link Gespraechsspeicher#finde}); der Mandant geht nie an das Modell.
 *
 * <p><b>Anweisung und Werkzeuge sind beim Eroeffnen festgelegt</b> (Festlegung F1): Anthropic
 * verlangt fuer zurueckgeschickte Denkbloecke, dass sie sich im Gespraech nicht aendern. Geprueft
 * wird jeder Vorschlag trotzdem gegen das aktuelle Angebot.
 *
 * <p><b>Der Verlauf waechst nur am Ende</b>, und erst, wenn eine Nachricht vollstaendig beantwortet
 * ist ({@link #schliesseAb}). Pro Gespraech laeuft hoechstens eine Nachricht zugleich ({@link
 * #beginne}).
 */
public final class Gespraech {

  private final UUID id;
  private final String sitzungId;
  private final long nutzerId;
  private final String mandantId;
  private final String anweisung;
  private final List<Werkzeug> werkzeuge;
  private final List<Nachricht> verlauf = new ArrayList<>();
  private final AtomicBoolean laeuft = new AtomicBoolean();
  private int kundennachrichten;
  private volatile Instant letzteAktivitaet;

  Gespraech(
      UUID id,
      String sitzungId,
      long nutzerId,
      String mandantId,
      String anweisung,
      List<Werkzeug> werkzeuge,
      Instant eroeffnet) {
    this.id = id;
    this.sitzungId = sitzungId;
    this.nutzerId = nutzerId;
    this.mandantId = mandantId;
    this.anweisung = anweisung;
    this.werkzeuge = List.copyOf(werkzeuge);
    this.letzteAktivitaet = eroeffnet;
  }

  public UUID id() {
    return id;
  }

  String sitzungId() {
    return sitzungId;
  }

  long nutzerId() {
    return nutzerId;
  }

  String mandantId() {
    return mandantId;
  }

  public String anweisung() {
    return anweisung;
  }

  public List<Werkzeug> werkzeuge() {
    return werkzeuge;
  }

  /** Eine Kopie des Verlaufs. */
  public synchronized List<Nachricht> verlauf() {
    return List.copyOf(verlauf);
  }

  public synchronized int kundennachrichten() {
    return kundennachrichten;
  }

  Instant letzteAktivitaet() {
    return letzteAktivitaet;
  }

  void beruehre(Instant jetzt) {
    letzteAktivitaet = jetzt;
  }

  /** Sperrt das Gespraech fuer eine Nachricht. {@code false}, wenn schon eine laeuft. */
  boolean beginne() {
    return laeuft.compareAndSet(false, true);
  }

  void gibFrei() {
    laeuft.set(false);
  }

  /** Haengt die vollstaendig beantwortete Nachricht an und zaehlt sie. */
  public synchronized void schliesseAb(List<Nachricht> neu) {
    verlauf.addAll(neu);
    kundennachrichten++;
  }
}
