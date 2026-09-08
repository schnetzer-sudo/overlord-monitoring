package de.kraftwerkone.overlord.monitor.bam;

import java.util.Optional;

/**
 * Die Abbildung der <b>Typ‑0‑Namen</b> aus {@code MessagePropertySearchListEntry} auf die Spalte,
 * die sie meinen — <b>im Code, unveränderlich, acht Einträge</b>.
 *
 * <h2>Warum es diese Abbildung geben muss</h2>
 *
 * <p>Die Konfigurationstabelle führt zwei Arten von Feldnamen (E‑101). Typ 1 benennt eine Zeile in
 * {@code MessageProperty} — dort wird über Name und Wert gesucht. Typ 0 benennt <b>keine</b> solche
 * Zeile: M155 hat für alle acht Typ‑0‑Namen gemessen, dass sie in {@code MessageProperty} nicht
 * vorkommen, und für alle Typ‑1‑Namen, dass sie es tun — ohne einen einzigen Gegenfall. Ein
 * Typ‑0‑Name ist eine <b>Spalte</b> von {@code Message}, {@code Process} oder {@code SOS}, und die
 * Suche baut für ihn ein Spaltenprädikat statt eines EAV-Zugriffs.
 *
 * <p><b>Welche Spalte, lässt sich aus dem Namen nicht rechnen</b> (M155): Nur drei der acht Namen
 * sind wörtliche Spaltennamen. Zwei stellen die Wortteile um ({@code MessageIDSource} gegen {@code
 * SourceMessageID}), einer heißt anders als seine Spalte ({@code Status} gegen {@code
 * MessageStatus}), und zwei tragen das Präfix {@code Message.}, obwohl sie in {@code Process}
 * beziehungsweise {@code SOS} wohnen. Deshalb eine Tabelle im Code — und keine Heuristik, die aus
 * {@code Message.X} die Spalte {@code X} riete (Regel Q4).
 *
 * <h2>Was mit einem Namen geschieht, den die Abbildung nicht kennt</h2>
 *
 * <p>Er wird <b>nicht angeboten</b> — er fällt still aus dem Angebot ({@code SuchfelderService}).
 * <b>Das ist nur deshalb vertretbar, weil {@code Typ0AbbildungDbIT} rot wird, sobald in der
 * Konfigurationstabelle ein Typ‑0‑Name steht, der hier fehlt.</b> Ohne diesen Test wäre das
 * Verschwinden unsichtbar; mit ihm ist es ein Bauauftrag mit einer Zeile. Dieselbe Bauform wie die
 * Sicherung gegen neue Statuswerte in {@code PROJEKTBESCHREIBUNG.md} §4.1.
 *
 * <p><b>Die Zuordnung ist eine Vermutung aus der Namensähnlichkeit, und sie ist als solche
 * gekennzeichnet</b> (M155, Belegvermerk): Gemessen ist, dass jede der acht Spalten existiert —
 * nicht, dass der Name sie meint. Eine fachliche Bestätigung des Auftraggebers steht aus; bis dahin
 * ist dies die einzige Lesart, die ohne Raten auskommt.
 *
 * <p><b>Verglichen wird ohne Rücksicht auf Groß- und Kleinschreibung</b>, weil die Spalte {@code
 * MessagePropertyName} unter {@code utf8mb4_general_ci} steht (M153) und der Primärschlüssel der
 * Konfigurationstabelle damit ebenfalls: Zwei Schreibweisen desselben Namens sind dort <i>ein</i>
 * Eintrag, und Java soll nicht strenger unterscheiden als die Spalte.
 */
public enum Typ0Feld {

  /** Wörtlich. {@code Message.MessageID} {@code varchar(36)}, der Primärschlüssel. */
  MESSAGE_ID("Message.MessageID", "Message.MessageID"),

  /** Umgestellt. {@code Message.SourceMessageID} {@code varchar(36)}. */
  MESSAGE_ID_SOURCE("Message.MessageIDSource", "Message.SourceMessageID"),

  /** Umgestellt. {@code Message.TargetMessageID} {@code varchar(36)}. */
  MESSAGE_ID_TARGET("Message.MessageIDTarget", "Message.TargetMessageID"),

  /** Wörtlich. {@code Message.ProcessID} {@code varchar(36)}. */
  PROCESS_ID("Message.ProcessID", "Message.ProcessID"),

  /**
   * Andere Tabelle. {@code Process.ProcessName} {@code varchar(255)}, über {@code ProcessID}.
   *
   * <p><b>Seit dem 08.09.2026 nicht mehr als Join, sondern vorab aufgelöst</b> (E‑109): Das
   * Repository sucht die Kennungen des Namens in {@code Process} und filtert {@code Message} dann
   * über {@code ProcessID} — dieselbe Menge, 3 ms statt 4.592 ms über 30 Tage ({@code
   * docs/property-suche.md} §10). Die Zielspalte bleibt, wie sie ist: Verglichen wird weiterhin
   * gegen {@code Process.ProcessName}, nur an einer anderen Stelle des Ablaufs.
   */
  PROCESS_NAME("Message.ProcessName", "Process.ProcessName"),

  /** Wörtlich. {@code Message.SOSID} {@code varchar(36)}. */
  SOS_ID("Message.SOSID", "Message.SOSID"),

  /** Andere Tabelle. {@code SOS.SOSName} {@code varchar(255)}, über {@code SOSID}. */
  SOS_NAME("Message.SOSName", "SOS.SOSName"),

  /** Umbenannt. {@code Message.MessageStatus} {@code varchar(30)} — der Rohwert, kein Eimer. */
  STATUS("Message.Status", "Message.MessageStatus");

  private final String feldname;
  private final String zielspalte;

  Typ0Feld(String feldname, String zielspalte) {
    this.feldname = feldname;
    this.zielspalte = zielspalte;
  }

  /** Der Name, wie er in {@code MessagePropertySearchListEntry} steht. */
  public String feldname() {
    return feldname;
  }

  /**
   * Die vermutete Spalte, als {@code Tabelle.Spalte} — <b>für Dokumentation und Tests</b>. Das
   * Prädikat selbst baut {@code BamSucheRepository}, weil nur Repository-Klassen die generierten
   * Tabellen des Quellschemas anfassen dürfen.
   */
  public String zielspalte() {
    return zielspalte;
  }

  /**
   * Das Feld zu einem konfigurierten Namen, oder leer, wenn die Abbildung ihn nicht kennt.
   *
   * <p>Ein leeres Ergebnis heißt für das Angebot „nicht anbieten" und für die Suche „kein
   * Spaltenprädikat, sondern EAV-Zugriff" — und beides ist nur deshalb harmlos, weil {@code
   * Typ0AbbildungDbIT} den fehlenden Namen meldet.
   */
  public static Optional<Typ0Feld> fuer(String name) {
    if (name == null) {
      return Optional.empty();
    }
    for (Typ0Feld feld : values()) {
      if (feld.feldname.equalsIgnoreCase(name)) {
        return Optional.of(feld);
      }
    }
    return Optional.empty();
  }
}
