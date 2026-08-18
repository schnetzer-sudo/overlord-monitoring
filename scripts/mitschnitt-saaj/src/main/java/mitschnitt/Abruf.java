package mitschnitt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.kraftwerkone.filestore.client.FilestoreClient;
import de.kraftwerkone.filestore.client.FilestoreFile;

/**
 * M71 — Ein Abruf mit dem echten Client gegen die Ablage, die der Verweis nennt.
 *
 * Stellt die Kette aus JsonServlet.java:784–:797 nach:
 *   - Verweis an der Pipe zerlegen (Q1)
 *   - gesendet wird die NACKTE GUID dahinter
 *   - Endpunkt ist der ServiceConnectString der Ablage, unveraendert und ohne
 *     Anhaengsel
 *
 * EIN Lauf, EIN Verweis. Keine Wiederholung, kein zweiter Verweis.
 *
 * ── G1, Stufe 2 ────────────────────────────────────────────────────────────
 * Dieses Programm gibt NIEMALS Dateiinhalt aus, und auch keinen Dateinamen,
 * keinen ZIP-Eintragsnamen, keine GUID und keine Adresse. Ausgegeben werden
 * ausschliesslich Zahlen, Ja/Nein-Werte und der Inhalt des Attributs
 * `Response` aus dem SOAP-Rumpf — letzteres ist ein Statusvermerk des Dienstes,
 * kein Nutzdatum.
 *
 * Die abgerufene Datei wird nicht geoeffnet, um sie anzusehen. Der erste
 * ZIP-Eintrag wird gelesen, um Bytes zu ZAEHLEN; kein Byte davon erreicht die
 * Ausgabe.
 *
 * Aufruf: Abruf <endpunkt> <verweis-mit-pipe> <retrieveDir>
 */
public final class Abruf {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("FEHLER: erwartet <endpunkt> <verweis> <retrieveDir>");
            System.exit(2);
        }
        String endpunkt = args[0];
        String verweis = args[1];
        Path retrieveDir = Paths.get(args[2]).toAbsolutePath();

        // Wegwerfverzeichnis leeren, damit "ist eine Datei entstanden?" eine
        // Aussage ueber DIESEN Lauf ist und nicht ueber einen frueheren.
        Files.createDirectories(retrieveDir);
        try (var s = Files.list(retrieveDir)) {
            for (Path p : s.toList()) {
                Files.deleteIfExists(p);
            }
        }

        // ── Zerlegung nach Q1, wie JsonServlet.java:784–:786 ──────────────────
        String[] split = verweis.split("\\|");
        String filestore = split[0];
        String messagePayloadGUID = split[1];

        System.out.println("Java:            " + System.getProperty("java.version"));
        System.out.println("Ablage:          " + filestore);          // freigegeben (G1)
        System.out.println("GUID gesendet:   nackt, " + messagePayloadGUID.length() + " Zeichen");
        System.out.println("Endpunkt:        ServiceConnectString unveraendert, "
                           + endpunkt.length() + " Zeichen");         // Wert selbst gesperrt (G1)
        System.out.println("Zielverzeichnis: Wegwerfverzeichnis, vor dem Lauf geleert");

        // ── Die Kette aus JsonServlet.java:793–:797 ───────────────────────────
        FilestoreFile filestoreFile = new FilestoreFile(
                messagePayloadGUID, null, FilestoreFile.filestoreAction.RETRIEVE, messagePayloadGUID);

        FilestoreClient filestoreClient =
                new FilestoreClient(new java.net.URL(endpunkt), retrieveDir + File.separator);
        filestoreClient.addFileStoreAction(filestoreFile);
        long t0 = System.nanoTime();
        boolean ergebnis = filestoreClient.requestFilestoreActions();
        long dauerMs = (System.nanoTime() - t0) / 1_000_000L;
        // ─────────────────────────────────────────────────────────────────────

        System.out.println();
        System.out.println("=== Antwort ===");
        System.out.println("requestFilestoreActions() : " + ergebnis);
        System.out.println("Dauer                     : " + dauerMs + " ms");

        // `Response` aus dem Rumpf — der Client legt es je Datei ab
        // (FilestoreClient.java:102). Statusvermerk des Dienstes, kein Nutzdatum.
        String response = filestoreClient.getFilestoreFiles().get(0).getResponse();
        System.out.println("Attribut Response         : "
                           + (response == null ? "<nicht gesetzt>" : "\"" + response + "\""));

        // Anzahl der Anhaenge und Groesse des Umschlags: nur ueber das private
        // Feld erreichbar. Gelesen wird per Reflexion, damit FilestoreClient.java
        // byteidentisch bleibt.
        int anhaenge = -1;
        int umschlagBytes = -1;
        try {
            var feld = FilestoreClient.class.getDeclaredField("soapMessageResponse");
            feld.setAccessible(true);
            Object o = feld.get(filestoreClient);
            if (o instanceof javax.xml.soap.SOAPMessage antwort) {
                anhaenge = antwort.countAttachments();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                antwort.writeTo(bos);
                umschlagBytes = bos.size();
            }
        } catch (Throwable t) {
            System.out.println("Reflexion fehlgeschlagen  : " + t.getClass().getSimpleName());
        }
        System.out.println("Anhaenge in der Antwort   : " + (anhaenge < 0 ? "nicht ermittelbar" : anhaenge));
        System.out.println("Antwort neu serialisiert  : "
                           + (umschlagBytes < 0 ? "nicht ermittelbar" : umschlagBytes + " Byte"));

        // ── Ist eine Datei entstanden? ───────────────────────────────────────
        System.out.println();
        System.out.println("=== Wegwerfverzeichnis ===");
        Path[] dateien;
        try (var s = Files.list(retrieveDir)) {
            dateien = s.toArray(Path[]::new);
        }
        System.out.println("Dateien entstanden        : " + dateien.length);
        if (dateien.length == 0) {
            System.out.println("Keine Datei — Auswertung entfaellt.");
            return;
        }
        Path datei = dateien[0];  // Name wird NICHT ausgegeben (G1)
        long groesse = Files.size(datei);
        System.out.println("Groesse                   : " + groesse + " Byte");
        System.out.println("Endung .zip               : " + datei.getFileName().toString().endsWith(".zip"));

        // ── ZIP-Inhaltsverzeichnis: Zahl und Groessen, KEINE Namen ───────────
        System.out.println();
        System.out.println("=== ZIP-Verzeichnis (Q4) ===");
        try (ZipFile zip = new ZipFile(datei.toFile())) {
            int n = 0;
            Enumeration<? extends ZipEntry> e = zip.entries();
            ZipEntry erster = null;
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                n++;
                if (erster == null) {
                    erster = ze;
                }
                System.out.println("  Eintrag " + n
                                   + ": entpackt " + ze.getSize()
                                   + " Byte, gepackt " + ze.getCompressedSize()
                                   + " Byte, Verzeichnis=" + ze.isDirectory());
            }
            System.out.println("Eintraege gesamt          : " + n);

            if (erster != null && !erster.isDirectory()) {
                zaehleBytes(zip, erster);
            }
        }
    }

    /**
     * Zaehlt Byteklassen des ersten ZIP-Eintrags. Liest den Eintrag, gibt
     * ausschliesslich Zahlen aus. Kein Byte erreicht die Ausgabe.
     */
    private static void zaehleBytes(ZipFile zip, ZipEntry eintrag) throws Exception {
        byte[] bytes;
        try (InputStream in = zip.getInputStream(eintrag)) {
            bytes = in.readAllBytes();
        }

        long ueber7f = 0, nullbytes = 0, druckbar = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v > 0x7F) { ueber7f++; }
            if (v == 0x00) { nullbytes++; }
            // druckbar: ASCII 0x20..0x7E plus Tab, CR, LF
            if ((v >= 0x20 && v <= 0x7E) || v == 0x09 || v == 0x0A || v == 0x0D) { druckbar++; }
        }

        boolean utf8Gueltig;
        try {
            var dec = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            dec.decode(java.nio.ByteBuffer.wrap(bytes));
            utf8Gueltig = true;
        } catch (CharacterCodingException ex) {
            utf8Gueltig = false;
        }

        int n = bytes.length;
        System.out.println();
        System.out.println("=== Erster Eintrag, Byteklassen (nur Zahlen) ===");
        System.out.println("Bytes gesamt              : " + n);
        System.out.println("Bytes ueber 0x7F          : " + ueber7f + anteil(ueber7f, n));
        System.out.println("Nullbytes                 : " + nullbytes + anteil(nullbytes, n));
        System.out.println("druckbare Zeichen         : " + druckbar + anteil(druckbar, n));
        System.out.println("vollstaendig gueltiges UTF-8 : " + utf8Gueltig);
        System.out.println("Einschaetzung binaer      : " + (nullbytes > 0 || druckbar * 100L < n * 90L));
    }

    private static String anteil(long teil, int gesamt) {
        if (gesamt == 0) { return ""; }
        return String.format(" (%.2f %%)", teil * 100.0 / gesamt);
    }
}
