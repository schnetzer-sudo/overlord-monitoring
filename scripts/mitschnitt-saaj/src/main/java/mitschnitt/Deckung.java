package mitschnitt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.kraftwerkone.filestore.client.FilestoreClient;
import de.kraftwerkone.filestore.client.FilestoreFile;

/**
 * M66 - Decken sich die beiden Kopien?
 *
 * Liest die Stichprobe aus m66-auswahl.sql und ruft JEDEN Verweis EINMAL ab,
 * SEQUENZIELL, bei der Ablage, die er nennt. Kein Rueckfall auf die andere
 * Ablage, auch nicht bei Misserfolg.
 *
 * ── G1, Stufe 2 ────────────────────────────────────────────────────────────
 * Kein Dateiinhalt, kein Dateiname, kein ZIP-Eintragsname, keine GUID und keine
 * Adresse erreichen die Ausgabe. Ausgegeben werden Zaehlungen, Bytegroessen und
 * der Inhalt des Attributs `Response` - ein Statusvermerk des Dienstes.
 * KEIN ZIP-Eintrag wird geoeffnet; gezaehlt wird nur das Inhaltsverzeichnis.
 * Jede geholte Datei wird unmittelbar nach dem Zaehlen geloescht.
 *
 * Aufruf: Deckung <auswahl.tsv> <arbeitsverzeichnis>
 */
public final class Deckung {

    private record Zeile(String scheibe, String ablage, String familie, String verweis, String adresse) { }

    private static final class Zaehler {
        int versuche, dateiDa, keineDatei, fehler, anhaengeSumme, zipEintraege;
        long bytesSumme;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("FEHLER: erwartet <auswahl.tsv> <arbeitsverzeichnis>");
            System.exit(2);
        }

        // Ohne Zeitlimit haengt ein Abruf gegen eine nicht laufende Ablage
        // unbegrenzt. Gesetzt ueber Systemeigenschaften, damit
        // FilestoreClient.java byteidentisch bleibt.
        System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
        System.setProperty("sun.net.client.defaultReadTimeout", "20000");

        Path tsv = Paths.get(args[0]);
        Path arbeit = Paths.get(args[1]).toAbsolutePath();
        Path ziel = arbeit.resolve("abruf");
        Files.createDirectories(ziel);
        leeren(ziel);

        List<Zeile> zeilen = new ArrayList<>();
        List<String> roh = Files.readAllLines(tsv, StandardCharsets.UTF_8);
        for (String z : roh) {
            String[] f = z.split("\t", -1);
            if (f.length != 5 || "scheibe".equals(f[0]) || !f[3].contains("|")) {
                continue;   // Kopfzeilen und der read_only-Nachweis
            }
            zeilen.add(new Zeile(f[0], f[1], f[2], f[3], f[4]));
        }

        System.out.println("Java:                " + System.getProperty("java.version"));
        System.out.println("Verbindungszeitlimit: 3000 ms, Lesezeitlimit 20000 ms");
        System.out.println("Verweise in der Stichprobe: " + zeilen.size());
        System.out.println();

        Map<String, Zaehler> nachScheibeAblageArt = new LinkedHashMap<>();
        Map<String, Long> dauerJeScheibe = new LinkedHashMap<>();
        Map<String, Integer> responseWerte = new LinkedHashMap<>();

        long tGesamt0 = System.nanoTime();
        String letzteScheibe = null;
        long tScheibe0 = System.nanoTime();
        int i = 0;

        for (Zeile z : zeilen) {
            if (!z.scheibe().equals(letzteScheibe)) {
                if (letzteScheibe != null) {
                    dauerJeScheibe.put(letzteScheibe, (System.nanoTime() - tScheibe0) / 1_000_000L);
                }
                letzteScheibe = z.scheibe();
                tScheibe0 = System.nanoTime();
                System.out.println("--- Zeitscheibe " + z.scheibe() + " ---");
            }
            i++;

            String art = "Message.Payload.GUID".equals(z.familie()) ? "nutzdaten" : "protokoll";
            String schluessel = z.scheibe() + " | " + z.ablage() + " | " + art;
            Zaehler c = nachScheibeAblageArt.computeIfAbsent(schluessel, k -> new Zaehler());
            c.versuche++;

            String guid = z.verweis().split("\\|")[1];
            boolean ok = false;
            String response = null;
            int anhaenge = 0;

            try {
                FilestoreFile ff = new FilestoreFile(
                        guid, null, FilestoreFile.filestoreAction.RETRIEVE, guid);
                FilestoreClient fc = new FilestoreClient(
                        new java.net.URL(z.adresse()), ziel + File.separator);
                fc.addFileStoreAction(ff);
                ok = fc.requestFilestoreActions();
                response = fc.getFilestoreFiles().get(0).getResponse();
                anhaenge = anhaenge(fc);
            } catch (Throwable t) {
                response = "<Ausnahme: " + t.getClass().getSimpleName() + ">";
            }

            responseWerte.merge(response == null ? "<nicht gesetzt>" : response, 1, Integer::sum);
            c.anhaengeSumme += Math.max(anhaenge, 0);

            Path[] entstanden = dateien(ziel);
            if (!ok) {
                c.fehler++;
            } else if (entstanden.length == 0) {
                c.keineDatei++;
            } else {
                c.dateiDa++;
                c.bytesSumme += Files.size(entstanden[0]);
                c.zipEintraege += zipEintraege(entstanden[0]);
            }
            // Sofort loeschen: produktive Nutzdaten (G1 Stufe 2).
            leeren(ziel);

            if (i % 25 == 0) {
                System.out.println("  " + i + " von " + zeilen.size() + " abgerufen");
            }
        }
        if (letzteScheibe != null) {
            dauerJeScheibe.put(letzteScheibe, (System.nanoTime() - tScheibe0) / 1_000_000L);
        }
        long gesamtMs = (System.nanoTime() - tGesamt0) / 1_000_000L;

        System.out.println();
        System.out.println("=== Ergebnis je Zeitscheibe, Ablage und Art ===");
        System.out.printf("%-34s %8s %8s %10s %7s %10s %6s%n",
                "Scheibe | Ablage | Art", "Versuche", "Datei", "keineDatei", "Fehler", "Bytes", "ZIP");
        for (Map.Entry<String, Zaehler> e : nachScheibeAblageArt.entrySet()) {
            Zaehler c = e.getValue();
            System.out.printf("%-34s %8d %8d %10d %7d %10d %6d%n",
                    e.getKey(), c.versuche, c.dateiDa, c.keineDatei, c.fehler, c.bytesSumme, c.zipEintraege);
        }

        System.out.println();
        System.out.println("=== Werte des Attributs Response ===");
        responseWerte.forEach((k, v) -> System.out.println("  " + v + " x  \"" + k + "\""));

        System.out.println();
        System.out.println("=== Dauer ===");
        dauerJeScheibe.forEach((k, v) -> System.out.println("  " + k + ": " + v + " ms"));
        System.out.println("  gesamt: " + gesamtMs + " ms fuer " + zeilen.size() + " Verweise"
                           + (zeilen.isEmpty() ? "" : "  (" + (gesamtMs / zeilen.size()) + " ms je Verweis)"));

        System.out.println();
        System.out.println("Zielverzeichnis nach dem Lauf leer: " + (dateien(ziel).length == 0));
    }

    private static int anhaenge(FilestoreClient fc) {
        try {
            var feld = FilestoreClient.class.getDeclaredField("soapMessageResponse");
            feld.setAccessible(true);
            Object o = feld.get(fc);
            if (o instanceof javax.xml.soap.SOAPMessage m) {
                return m.countAttachments();
            }
        } catch (Throwable ignore) {
            // Reflexion ist Zugabe, kein Messwert. Faellt sie aus, bleibt es bei 0.
        }
        return 0;
    }

    /** Zaehlt die Eintraege im ZIP-Inhaltsverzeichnis. Oeffnet KEINEN Eintrag. */
    private static int zipEintraege(Path datei) {
        try (ZipFile zip = new ZipFile(datei.toFile())) {
            int n = 0;
            var e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry unused = e.nextElement();
                n++;
            }
            return n;
        } catch (Exception ex) {
            return -1;   // keine gueltige ZIP-Datei
        }
    }

    private static Path[] dateien(Path dir) throws Exception {
        try (var s = Files.list(dir)) {
            return s.toArray(Path[]::new);
        }
    }

    private static void leeren(Path dir) throws Exception {
        for (Path p : dateien(dir)) {
            Files.deleteIfExists(p);
        }
    }
}
