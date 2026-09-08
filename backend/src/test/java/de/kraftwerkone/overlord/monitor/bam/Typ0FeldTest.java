package de.kraftwerkone.overlord.monitor.bam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Die Abbildung der Typ‑0‑Namen — <b>ohne Datenbank</b>. Was sie gegen die Konfigurationstabelle
 * leistet, prüft {@code Typ0AbbildungDbIT}; hier steht, was ohne Tabelle feststeht.
 */
class Typ0FeldTest {

  /**
   * <b>Acht Einträge, aus M155 abgeleitet</b> — nicht mehr und nicht weniger. Kommt ein neunter
   * dazu, ist das ein Bauauftrag mit Messung und nicht eine Zeile in diesem Enum.
   */
  @Test
  @DisplayName("Die Abbildung hat genau die acht Namen aus M155")
  void die_acht_namen_aus_m155() {
    assertThat(Typ0Feld.values()).hasSize(8);
    assertThat(List.of(Typ0Feld.values()).stream().map(Typ0Feld::feldname))
        .containsExactlyInAnyOrder(
            "Message.MessageID",
            "Message.MessageIDSource",
            "Message.MessageIDTarget",
            "Message.ProcessID",
            "Message.ProcessName",
            "Message.SOSID",
            "Message.SOSName",
            "Message.Status");
  }

  /**
   * <b>Nur drei von acht sind wörtlich</b> (M155) — die Gegenprobe dafür, dass die Abbildung nicht
   * durch eine Namensregel ersetzbar ist.
   */
  @Test
  @DisplayName("Fuenf der acht Namen zeigen auf eine anders benannte Spalte")
  void fuenf_namen_sind_nicht_woertlich() {
    long woertlich =
        List.of(Typ0Feld.values()).stream()
            .filter(feld -> feld.feldname().equals(feld.zielspalte()))
            .count();

    assertThat(woertlich).isEqualTo(3);
    assertThat(Typ0Feld.PROCESS_NAME.zielspalte()).startsWith("Process.");
    assertThat(Typ0Feld.SOS_NAME.zielspalte()).startsWith("SOS.");
    assertThat(Typ0Feld.STATUS.zielspalte()).isEqualTo("Message.MessageStatus");
  }

  /** Der Vergleich folgt der Kollation der Spalte ({@code utf8mb4_general_ci}, M153). */
  @Test
  @DisplayName("Der Name wird ohne Ruecksicht auf Gross- und Kleinschreibung gefunden")
  void gross_und_kleinschreibung_sind_gleich() {
    assertThat(Typ0Feld.fuer("message.status")).contains(Typ0Feld.STATUS);
    assertThat(Typ0Feld.fuer("MESSAGE.PROCESSNAME")).contains(Typ0Feld.PROCESS_NAME);
  }

  /** Ein unbekannter Name ist leer — und was daraus folgt, sichert {@code Typ0AbbildungDbIT}. */
  @Test
  @DisplayName("Ein unbekannter Name, ein leerer und null ergeben kein Feld")
  void unbekannt_ist_leer() {
    assertThat(Typ0Feld.fuer("Message.Foo")).isEmpty();
    assertThat(Typ0Feld.fuer("")).isEmpty();
    assertThat(Typ0Feld.fuer(null)).isEmpty();
    assertThat(Typ0Feld.fuer("Message.GUID")).as("ein Typ-1-Name ist keine Spalte").isEmpty();
  }
}
