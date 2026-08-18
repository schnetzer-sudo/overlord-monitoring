package de.kraftwerkone.overlord.monitor.payload;

/**
 * Der Dateiname des Downloads.
 *
 * <h2>Erst der Originalname, dann ein konstruierter</h2>
 *
 * <p>Vorhanden ist er fuer rund 69,6 % der <b>Nachrichten</b>, als {@code
 * <Lesedienst>.FileProperty.OriginalFilename} auf demselben Schritt (M17, M56). Das Altsystem
 * verwendet stattdessen die {@code MessageID} (Q4) — eine 36 Zeichen lange Kennung als Dateiname
 * ist unnoetig unhandlich, und sie ist fuer alle Artefakte derselben Nachricht dieselbe.
 *
 * <p><b>Nicht fuer 69,6 % der Artefakte</b>, und der Unterschied ist keiner der Wortwahl: Den Namen
 * traegt der Lesedienst, nicht jedes Artefakt. Ein {@code Converter.Payload.GUID} auf einem
 * Wandlungsschritt bekommt deshalb einen konstruierten Namen — richtigerweise, denn es ist eine
 * andere Datei als die eingegangene.
 *
 * <h2>Warum der konstruierte Name keine Endung bekommt</h2>
 *
 * <p>Weil es nichts zu erben gibt. M56 (c) hat die Endungen der Originalnamen erhoben: <b>4.307 von
 * 4.352</b> in Fenster A und 119.599 von 122.609 in Fenster B enden auf einen Punkt und eine reine
 * <i>Ziffernfolge</i> — {@code .txt} kommt viermal vor, {@code .cod} einmal. Die echten Dateinamen
 * tragen also selbst keine Typangabe. Eine zu erfinden hiesse, dem Nutzer eine Information zu
 * geben, die die Quelle nicht hat (Regel Q4). Der Typ steht ohnehin im {@code Content-Type}, und
 * der ist {@code application/octet-stream} — auch das kein Erraten.
 *
 * <h2>Bereinigung</h2>
 *
 * <p>Der Originalname kommt aus der EDI-Datei und damit <b>vom Partner</b>; er ist eine Eingabe von
 * aussen, keine Konstante. Entfernt wird deshalb dreierlei:
 *
 * <ul>
 *   <li><b>Steuerzeichen</b> — Zeilenumbrueche darin wuerden den {@code Content-Disposition}-Kopf
 *       spalten.
 *   <li><b>Pfadzeichen und fuehrende Punkte</b> — {@code ..} und {@code /} deuteten einen Pfad an.
 *   <li><b>Unicode-Formatzeichen</b> ({@code Character#FORMAT}), darunter {@code U+202E}
 *       RIGHT-TO-LEFT OVERRIDE und die Zeichen ohne Breite. Sie sind nicht sichtbar und drehen die
 *       Anzeige des Dateinamens um: aus {@code rechnung‮gpj.exe} wird im Speichern-Dialog scheinbar
 *       {@code rechnungexe.jpg}. {@code ContentDisposition.filename(name, UTF_8)} prozentkodiert
 *       sie zwar fuer die Leitung, aber der Browser dekodiert sie wieder — die Kodierung ist also
 *       kein Schutz.
 * </ul>
 */
public final class Downloaddateiname {

  private Downloaddateiname() {}

  /**
   * Der gemeinsame Namensteil aller Originaldateinamen. Vorne steht der Lesedienst: {@code
   * FileReader.FileProperty.OriginalFilename} oder {@code FTPReader.FileProperty.OriginalFilename}
   * — mehr Familien tragen ihn im gemessenen Bestand nicht (M56 a, beide Fenster).
   */
  static final String MUSTER_ORIGINALNAME = ".FileProperty.OriginalFilename";

  /**
   * Das LIKE-Muster fuer die Abfrage. Kein {@code _} darin — der waere ein Platzhalter.
   *
   * <p><b>Gesucht wird ueber das Muster und nicht ueber die Familie des Artefakts.</b> Das ist
   * nicht dasselbe, und der Unterschied faellt genau am wichtigsten Artefakt auf: Der Eingang
   * heisst {@code Message.Payload.GUID}, seine Familie also {@code Message} — aber {@code
   * Message.FileProperty.OriginalFilename} <b>gibt es nicht</b>. Den Originalnamen tragen
   * ausschliesslich die Lesedienste: {@code FileReader.FileProperty.OriginalFilename} (4.352 bzw.
   * 122.609 Zeilen) und {@code FTPReader.FileProperty.OriginalFilename} (111 bzw. 2.034), M56 (a)
   * in beiden Fenstern. Ueber die Familie gesucht bekaeme ausgerechnet die eingegangene Datei — der
   * Kopf der Liste und das naheliegendste Ziel eines Nutzers — nie ihren echten Namen, obwohl er in
   * derselben Nachricht auf demselben Schritt steht.
   *
   * <p><b>Eingegrenzt bleibt es trotzdem — auf den Schritt.</b> Der Originalname beschreibt die
   * Datei, die <i>eingegangen</i> ist. Fuer ein {@code Converter.Payload.GUID} auf Schritt 2 ist
   * das eine andere Datei; ihm den Namen des Eingangs zu geben waere eine Falschauskunft. Deshalb
   * zaehlt nur, was auf <b>demselben</b> {@code MessageActionID} liegt: Auf Schritt 0 findet das
   * den Lesedienst, auf einem Wandlungsschritt nichts — und dann greift der konstruierte Name.
   */
  static String likeMuster() {
    return "%" + MUSTER_ORIGINALNAME;
  }

  /**
   * Baut den Dateinamen.
   *
   * @param originalname der Wert aus {@code <Dienst>.FileProperty.OriginalFilename}, oder {@code
   *     null}
   * @param artefaktname {@code MessagePropertyName} des Artefakts
   * @param schritt {@code MessageActionID}
   * @param messageId die Nachricht — nur fuer den konstruierten Namen
   */
  public static String baue(
      String originalname, String artefaktname, short schritt, String messageId) {
    String bereinigt = bereinige(originalname);
    if (bereinigt != null) {
      return bereinigt;
    }
    String familie = Artefaktnamen.familie(artefaktname);
    Artefaktart art = Artefaktnamen.art(artefaktname);
    String teil =
        (familie == null ? "artefakt" : familie)
            + "-schritt"
            + schritt
            + "-"
            + (art == Artefaktart.PROTOKOLL ? "protokoll" : "nutzdaten")
            + "-"
            + messageId;
    // Auch der konstruierte Name laeuft durch die Bereinigung: die MessageID ist zwar ein
    // Schluessel aus der Datenbank und keine Partnereingabe, aber eine Ausnahme, die man sich
    // merken muss, ist eine Ausnahme zu viel.
    String sauber = bereinige(teil);
    return sauber == null ? "artefakt" : sauber;
  }

  /**
   * Entfernt alles, was in einem Dateinamen nichts zu suchen hat.
   *
   * @return {@code null}, wenn nichts Brauchbares uebrig bleibt
   */
  private static String bereinige(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    StringBuilder gebaut = new StringBuilder(name.length());
    for (int i = 0; i < name.length(); i++) {
      char zeichen = name.charAt(i);
      boolean verboten =
          zeichen < 0x20 // Steuerzeichen, darunter CR und LF: sie wuerden den Kopf spalten
              || (zeichen >= 0x7F && zeichen <= 0x9F) // DEL und der C1-Bereich
              || Character.getType(zeichen) == Character.FORMAT
              || zeichen == '/'
              || zeichen == '\\'
              || zeichen == '"'
              || zeichen == ':'
              || zeichen == '*'
              || zeichen == '?'
              || zeichen == '<'
              || zeichen == '>'
              || zeichen == '|';
      gebaut.append(verboten ? '_' : zeichen);
    }
    // Fuehrende Punkte weg: ".." waere ein Pfadwechsel, ".name" auf Unix eine versteckte Datei.
    String ergebnis = gebaut.toString().strip();
    while (ergebnis.startsWith(".")) {
      ergebnis = ergebnis.substring(1);
    }
    ergebnis = ergebnis.strip();
    if (ergebnis.isEmpty()) {
      return null;
    }
    // 200 Zeichen: der laengste gemessene Originalname hat 127 (M56, Fenster B). Die Grenze
    // schuetzt den Kopf, nicht den gemessenen Bestand.
    return ergebnis.length() > 200 ? ergebnis.substring(0, 200) : ergebnis;
  }
}
