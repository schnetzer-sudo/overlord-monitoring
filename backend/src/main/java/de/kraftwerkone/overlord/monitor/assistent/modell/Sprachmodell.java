package de.kraftwerkone.overlord.monitor.assistent.modell;

/**
 * Ein Sprachmodell hinter einer anbieterneutralen Schnittstelle (E‑1032, {@code docs/assistent.md}
 * §4.2). Ein Aufruf ist zustandslos: Anweisung, Verlauf und Werkzeuge kommen mit jeder {@link
 * Modellanfrage}.
 *
 * <p><b>Ein neuer Anbieter ist eine neue Umsetzung dieses Interfaces</b> in einem eigenen
 * Unterpaket von {@code assistent}; Gespraechsfuehrung, Werkzeuge und Endpunkte bleiben, wie sie
 * sind ({@code docs/assistent.md} §5). Gewaehlt wird per Konfiguration in {@code
 * config.AssistentConfig}.
 */
public interface Sprachmodell {

  /**
   * Ein Aufruf des Modells.
   *
   * @throws Modellfehler wenn der Anbieter nicht oder nicht verwertbar antwortet
   */
  Modellantwort rufe(Modellanfrage anfrage);

  /**
   * Das Modell eines abgeschalteten Assistenten (E‑1030). Der Schalter wird vor jedem Aufruf
   * geprueft; kommt trotzdem einer hier an, ist das ein Fehler im Code und kein Grund, doch noch
   * einen Anbieter anzusprechen.
   */
  Sprachmodell ABGESCHALTET =
      anfrage -> {
        throw new IllegalStateException("Assistent abgeschaltet, Aufruf des Modells verweigert");
      };
}
