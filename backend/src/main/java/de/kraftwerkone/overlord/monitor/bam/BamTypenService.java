package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.MandantContext;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Die Belegarten, die die Suche zur Auswahl anbietet.
 *
 * <p>Wenig Fachlogik, und trotzdem eine eigene Klasse: Sie hält die eine Regel, die der Endpunkt
 * hat — {@link Typbezeichnung} —, und hält sie an derselben Stelle wie der Belegdaten-Block und die
 * Trefferliste. Ein Nachbau im Controller wäre die dritte Fassung derselben Regel, und dann
 * beschriftete irgendwann eine davon denselben fehlenden Typ anders.
 *
 * <p><b>Eine leere Liste ist eine Antwort und kein Fehler.</b> {@code EDITIONLINGERI}, {@code
 * SYSTEM} und {@code WOC} haben keinen konfigurierten Typ (M40); für sie ist die Auswahl leer, und
 * die Oberfläche bietet dann gar keine an. Kein Platzhalter, kein Ersatztyp — dieselbe Regel wie
 * bei der leeren Spalte in {@code docs/nachrichtenliste.md} §8.1.
 */
@Service
public class BamTypenService {

  private final BamTypenRepository bamTypenRepository;

  BamTypenService(BamTypenRepository bamTypenRepository) {
    this.bamTypenRepository = bamTypenRepository;
  }

  /** Die konfigurierten Belegarten des aktiven Mandanten, in ihrer Reihenfolge. */
  public List<BamTypResponse> typen(MandantContext mandant) {
    return bamTypenRepository.findeKonfigurierteTypen(mandant).stream()
        .map(
            zeile ->
                new BamTypResponse(
                    zeile.typ(),
                    Typbezeichnung.fuer(zeile.typ(), zeile.bezeichnung()),
                    zeile.sortIndex()))
        .toList();
  }
}
