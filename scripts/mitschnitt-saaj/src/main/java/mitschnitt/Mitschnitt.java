package mitschnitt;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.kraftwerkone.filestore.client.FilestoreClient;
import de.kraftwerkone.filestore.client.FilestoreFile;

/**
 * M70 — Mitschnitt des SAAJ-Aufrufs.
 *
 * Stellt die Kette aus JsonServlet.java:793–:797 nach, in derselben Reihenfolge
 * und mit denselben Argumenten. Der einzige Unterschied zum Altsystem ist die
 * Zieladresse: Sie zeigt auf den lokalen Lauscher, NIEMALS auf einen Filestore.
 *
 * Der ServiceConnectString aus der Datenbank wird in diesem Lauf nicht gelesen,
 * nicht aufgeloest und nicht verwendet.
 *
 * Die GUID ist FREI ERFUNDEN. Sie kommt in keiner Stichprobe vor und gehoert zu
 * keiner Nachricht. Damit enthaelt der Mitschnitt keinerlei echte Daten und darf
 * im Wortlaut in die Ergebnisdatei.
 */
public final class Mitschnitt {

    /** Frei erfunden, syntaktisch gueltig, gehoert zu nichts. */
    private static final String ERFUNDENE_GUID = "deadbeef-0000-4000-8000-0123456789ab";

    private static String herkunft(Class<?> klasse) {
        try {
            java.security.CodeSource cs = klasse.getProtectionDomain().getCodeSource();
            return cs == null ? "unbekannt" : Paths.get(cs.getLocation().toURI()).getFileName().toString();
        } catch (Exception ex) {
            return "unbekannt (" + ex + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        Path basis = Paths.get("").toAbsolutePath();
        Path ziel = basis.resolve("mitschnitt").resolve("anfrage.bin");
        Path retrieveDir = basis.resolve("retrieve");
        java.nio.file.Files.createDirectories(retrieveDir);

        System.out.println("Java:  " + System.getProperty("java.version")
                           + "  (" + System.getProperty("java.vendor") + ")");

        try (Lauscher lauscher = new Lauscher(ziel)) {
            lauscher.starten();

            // Ausschliesslich die Loopback-Adresse. Kein Filestore.
            String adresse = "http://127.0.0.1:" + lauscher.port() + "/WebApplication/FileStoreSoapReceiver";
            System.out.println("Ziel:  " + adresse);
            System.out.println("GUID:  " + ERFUNDENE_GUID + "  (frei erfunden)");

            // Welche SAAJ-Fassung tatsaechlich geladen wird — die Zahl gehoert
            // zum Befund, weil die Serialisierung eine Eigenschaft genau dieser
            // Fassung ist.
            javax.xml.soap.MessageFactory mf = javax.xml.soap.MessageFactory.newInstance();
            System.out.println("SAAJ MessageFactory:        " + mf.getClass().getName());
            System.out.println("SAAJ SOAPConnectionFactory: "
                               + javax.xml.soap.SOAPConnectionFactory.newInstance().getClass().getName());
            Package p = mf.getClass().getPackage();
            System.out.println("SAAJ Implementation-Version: "
                               + (p == null ? "unbekannt" : String.valueOf(p.getImplementationVersion())));
            // Zwei Jars auf dem Klassenpfad liefern das Paket javax.xml.soap
            // (javax.xml.soap-api:1.4.0 und transitiv jakarta.xml.soap-api:1.4.2,
            // dessen Hauptversion 1.x noch das javax-Paket traegt). Welches
            // tatsaechlich geladen wird, gehoert zum Befund.
            System.out.println("API geladen aus:  " + herkunft(javax.xml.soap.MessageFactory.class));
            System.out.println("Impl geladen aus: " + herkunft(mf.getClass()));

            // ── Die Kette aus JsonServlet.java:793–:797, unveraendert ──────────
            FilestoreFile filestoreFile = new FilestoreFile(
                    ERFUNDENE_GUID, null, FilestoreFile.filestoreAction.RETRIEVE, ERFUNDENE_GUID);

            FilestoreClient filestoreClient =
                    new FilestoreClient(new java.net.URL(adresse), retrieveDir.toString() + java.io.File.separator);
            filestoreClient.addFileStoreAction(filestoreFile);
            boolean ergebnis = filestoreClient.requestFilestoreActions();
            // ──────────────────────────────────────────────────────────────────

            System.out.println("requestFilestoreActions() = " + ergebnis);
            lauscher.warten();
        }
    }
}
