package mitschnitt;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.kraftwerkone.filestore.client.FilestoreClient;
import de.kraftwerkone.filestore.client.FilestoreFile;

/**
 * M72 — Mitschnitt des SAAJ-Aufrufs, jakarta-Zweig.
 *
 * Zeile fuer Zeile dieselbe Kette wie mitschnitt.Mitschnitt aus M70
 * (JsonServlet.java:793–:797), in derselben Reihenfolge und mit denselben
 * Argumenten. Der einzige Unterschied ist der Namensraum der SOAP-API, den die
 * Diagnoseausgaben unten ansprechen — die Kette selbst ruehrt ihn nicht an.
 *
 * Die Zieladresse zeigt auf den lokalen Lauscher, NIEMALS auf einen Filestore.
 * Der ServiceConnectString aus der Datenbank wird in diesem Lauf nicht gelesen,
 * nicht aufgeloest und nicht verwendet.
 *
 * Die GUID ist FREI ERFUNDEN und dieselbe wie in M70. Sie kommt in keiner
 * Stichprobe vor und gehoert zu keiner Nachricht. Damit enthaelt der Mitschnitt
 * keinerlei echte Daten und darf im Wortlaut in die Ergebnisdatei.
 */
public final class MitschnittJakarta {

    /** Frei erfunden, syntaktisch gueltig, gehoert zu nichts. Wortgleich mit M70. */
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
            jakarta.xml.soap.MessageFactory mf = jakarta.xml.soap.MessageFactory.newInstance();
            System.out.println("SAAJ MessageFactory:        " + mf.getClass().getName());
            System.out.println("SAAJ SOAPConnectionFactory: "
                               + jakarta.xml.soap.SOAPConnectionFactory.newInstance().getClass().getName());
            Package p = mf.getClass().getPackage();
            System.out.println("SAAJ Implementation-Version: "
                               + (p == null ? "unbekannt" : String.valueOf(p.getImplementationVersion())));
            System.out.println("API geladen aus:  " + herkunft(jakarta.xml.soap.MessageFactory.class));
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
