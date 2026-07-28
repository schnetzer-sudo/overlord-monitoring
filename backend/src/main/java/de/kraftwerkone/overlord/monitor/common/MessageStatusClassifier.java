package de.kraftwerkone.overlord.monitor.common;

import java.util.Map;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.Field;
import org.springframework.stereotype.Component;

/**
 * Die <b>einzige</b> Stelle, an der ein {@code MessageStatus} fachlich eingeordnet wird. Liste,
 * Dashboard, Rollup und spaeter der Chatbot verwenden sie und bauen sie nicht nach — sonst driftet
 * die Einordnung ueber die Ausbaustufen auseinander.
 *
 * <p>{@code MessageStatus} ist <b>freier Text</b>, kein Aufzaehlungstyp ({@code CKECKED} ist der
 * Beweis). Unbekannte Werte werden zu {@link MessageStatusKind#UNGEKLAERT}, niemals zu einem
 * geratenen Wert. Ein Datenbanktest vergleicht {@code SELECT DISTINCT MessageStatus} gegen {@link
 * #bekannteStatuswerte()} und wird rot, sobald im Altsystem ein neuer Wert auftaucht — nur deshalb
 * ist die neutrale Behandlung vertretbar. Erhebung: {@code docs/message-status.md}.
 */
@Component
public class MessageStatusClassifier {

  /**
   * Die bekannten Statuswerte samt Einordnung, Stand {@code docs/message-status.md}. {@code
   * RUNNING} ist enthalten, kommt in der Testkopie aber null Mal vor (fluechtig).
   */
  private static final Map<String, MessageStatusKind> BEKANNT =
      Map.ofEntries(
          Map.entry("FINISHED", MessageStatusKind.ABGESCHLOSSEN),
          Map.entry("EERP_RECEIVED", MessageStatusKind.QUITTIERT),
          Map.entry("COMMIT_RECEIVED", MessageStatusKind.QUITTIERT),
          Map.entry("MERGED", MessageStatusKind.ZWISCHENSCHRITT),
          Map.entry("SPLITTED", MessageStatusKind.ZWISCHENSCHRITT),
          Map.entry("SUSPENDED", MessageStatusKind.WARTEND),
          Map.entry("RUNNING", MessageStatusKind.LAEUFT),
          Map.entry("ERROR_DUPLICATE", MessageStatusKind.FEHLER),
          Map.entry("ERROR_TIMEOUT", MessageStatusKind.FEHLER),
          Map.entry("COMMIT_REJECTED", MessageStatusKind.FEHLER),
          Map.entry("COMMIT_SENT", MessageStatusKind.UNGEKLAERT),
          Map.entry("CHECKED", MessageStatusKind.UNGEKLAERT),
          Map.entry("CKECKED", MessageStatusKind.UNGEKLAERT));

  /** Menge aller bekannten Statuswerte (13). */
  public Set<String> bekannteStatuswerte() {
    return BEKANNT.keySet();
  }

  /**
   * Einordnung eines Rohwertes. Unbekanntes und {@code null} werden zu {@link
   * MessageStatusKind#UNGEKLAERT} — nie zu einem geratenen Wert.
   */
  public MessageStatusKind einordnung(String status) {
    if (status == null) {
      return MessageStatusKind.UNGEKLAERT;
    }
    return BEKANNT.getOrDefault(status, MessageStatusKind.UNGEKLAERT);
  }

  /**
   * Die <b>eine</b> wiederverwendbare Fehlerbedingung fuer SQL:
   *
   * <pre>MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'</pre>
   *
   * Nicht {@code LEFT(MessageStatus, 6) = 'ERROR_'}: diese Form kann {@code MessageStatusIDX} nicht
   * nutzen und erzwingt in Kombination mit der Oder-Bedingung einen vollen Durchlauf. Die {@code
   * LIKE}-Fassung ergibt zwei Indexbereiche, die MariaDB zusammenfuehren kann.
   *
   * <p>Das Feld wird uebergeben, damit dieser gemeinsame Baustein nicht auf generierte {@code
   * jooq.glassfish}-Typen zugreifen muss — die Fehlerbedingung bleibt in {@code common}.
   */
  public Condition fehlerBedingung(Field<String> messageStatus) {
    return messageStatus.like("ERROR\\_%", '\\').or(messageStatus.eq("COMMIT_REJECTED"));
  }
}
