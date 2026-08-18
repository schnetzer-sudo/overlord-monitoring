package mitschnitt;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.kraftwerkone.filestore.client.FilestoreClient;
import de.kraftwerkone.filestore.client.FilestoreFile;

/**
 * Gemeinsamer Abrufteil fuer M60, M61, M63, M64, M65, M66 (2), M67 und M68.
 *
 * Liest eine Stichprobe (TSV) und ruft JEDEN Verweis EINMAL ab, SEQUENZIELL,
 * bei der Ablage, die er nennt. Kein Rueckfall, auch nicht bei Misserfolg.
 * Operation ausschliesslich RETRIEVE.
 *
 * Legt je Verweis <nr>.zip im Arbeitsverzeichnis ab und schreibt zuordnung.csv.
 * Die Dateien bleiben liegen, bis auswertung.ps1 sie gezaehlt hat; geloescht
 * werden sie vom aufrufenden Skript.
 *
 * ── G1, Stufe 2 ────────────────────────────────────────────────────────────
 * Dieses Programm oeffnet KEINEN ZIP-Eintrag und gibt weder Inhalt noch
 * Dateinamen, GUID oder Adresse aus. Auf den Bildschirm kommen Zaehlungen,
 * Bytegroessen und der Statusvermerk `Response`.
 *
 * TSV-Spalten: label \t ablage \t familie \t verweis \t adresse \t zusatz
 * Aufruf:      Stichprobe <tsv> <arbeitsverzeichnis>
 *
 * Sonderfall M68 (Kreuzabruf): Traegt die Spalte `zusatz` den Wert `KREUZ`,
 * wird die Adresse verwendet, wie sie in der Zeile steht - dort steht dann
 * bewusst die Adresse der ANDEREN Ablage. Das ist die einzige vorgesehene
 * Ausnahme von der Holregel und im Auftrag als M68 benannt.
 */
public final class Stichprobe {

    private record Zeile(String label, String ablage, String familie,
                         String verweis, String adresse, String zusatz) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("FEHLER: erwartet <tsv> <arbeitsverzeichnis>");
            System.exit(2);
        }
        System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
        System.setProperty("sun.net.client.defaultReadTimeout", "20000");

        Path tsv = Paths.get(args[0]);
        Path arbeit = Paths.get(args[1]).toAbsolutePath();
        Path ablageDir = arbeit.resolve("dateien");
        Path scratch = arbeit.resolve("scratch");
        Files.createDirectories(ablageDir);
        Files.createDirectories(scratch);
        leeren(ablageDir);
        leeren(scratch);

        List<Zeile> zeilen = new ArrayList<>();
        for (String z : Files.readAllLines(tsv, StandardCharsets.UTF_8)) {
            String[] f = z.split("\t", -1);
            if (f.length < 5 || !f[3].contains("|") || "label".equals(f[0])) {
                continue;
            }
            zeilen.add(new Zeile(f[0], f[1], f[2], f[3], f[4], f.length > 5 ? f[5] : ""));
        }

        System.out.println("Verweise in der Stichprobe : " + zeilen.size());
        System.out.println("Verbindungszeitlimit 3000 ms, Lesezeitlimit 20000 ms");
        System.out.println();

        StringBuilder csv = new StringBuilder("nr,label,familie,ablage,art,zusatz,ok,response,zip_bytes,zip_eintraege\n");
        Map<String, Integer> responseWerte = new LinkedHashMap<>();
        int nr = 0, dateiDa = 0, keineDatei = 0, fehler = 0;
        long bytesSumme = 0;
        long t0 = System.nanoTime();

        for (Zeile z : zeilen) {
            nr++;
            String art = z.familie().endsWith(".Log.GUID") ? "protokoll" : "nutzdaten";
            String guid = z.verweis().split("\\|")[1];
            boolean ok = false;
            String response = null;

            try {
                FilestoreFile ff = new FilestoreFile(
                        guid, null, FilestoreFile.filestoreAction.RETRIEVE, guid);
                FilestoreClient fc = new FilestoreClient(
                        new java.net.URL(z.adresse()), scratch + File.separator);
                fc.addFileStoreAction(ff);
                ok = fc.requestFilestoreActions();
                response = fc.getFilestoreFiles().get(0).getResponse();
            } catch (Throwable t) {
                response = "<Ausnahme: " + t.getClass().getSimpleName() + ">";
            }
            responseWerte.merge(response == null ? "<nicht gesetzt>" : response, 1, Integer::sum);

            long zipBytes = -1;
            int eintraege = -1;
            Path[] da = dateien(scratch);
            if (!ok) {
                fehler++;
            } else if (da.length == 0) {
                keineDatei++;
            } else {
                dateiDa++;
                Path ziel = ablageDir.resolve(nr + ".zip");
                Files.move(da[0], ziel, StandardCopyOption.REPLACE_EXISTING);
                zipBytes = Files.size(ziel);
                bytesSumme += zipBytes;
                eintraege = eintraege(ziel);
            }
            leeren(scratch);

            csv.append(nr).append(',').append(csvFeld(z.label())).append(',')
               .append(csvFeld(z.familie())).append(',').append(csvFeld(z.ablage())).append(',')
               .append(art).append(',').append(csvFeld(z.zusatz())).append(',')
               .append(ok).append(',').append(csvFeld(response == null ? "" : response)).append(',')
               .append(zipBytes).append(',').append(eintraege).append('\n');

            if (nr % 50 == 0) {
                System.out.println("  " + nr + " von " + zeilen.size() + " abgerufen");
            }
        }
        long msGesamt = (System.nanoTime() - t0) / 1_000_000L;
        Files.writeString(arbeit.resolve("zuordnung.csv"), csv.toString(), StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("=== Abruf ===");
        System.out.println("Verweise           : " + zeilen.size());
        System.out.println("Datei geliefert    : " + dateiDa);
        System.out.println("keine Datei        : " + keineDatei);
        System.out.println("Fehler             : " + fehler);
        System.out.println("ZIP-Bytes gesamt   : " + bytesSumme);
        System.out.println("Dauer              : " + msGesamt + " ms"
                + (zeilen.isEmpty() ? "" : "  (" + (msGesamt / Math.max(zeilen.size(), 1)) + " ms je Verweis)"));
        System.out.println();
        System.out.println("=== Werte des Attributs Response ===");
        responseWerte.forEach((k, v) -> System.out.println("  " + v + " x  \"" + k + "\""));

        Files.deleteIfExists(scratch);
    }

    /** Zaehlt Eintraege im ZIP-Inhaltsverzeichnis. Oeffnet KEINEN Eintrag. */
    private static int eintraege(Path datei) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(datei.toFile())) {
            int n = 0;
            var e = zip.entries();
            while (e.hasMoreElements()) { e.nextElement(); n++; }
            return n;
        } catch (Exception ex) {
            return -1;
        }
    }

    private static String csvFeld(String s) {
        if (s == null) { return ""; }
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static Path[] dateien(Path dir) throws Exception {
        try (var s = Files.list(dir)) { return s.toArray(Path[]::new); }
    }

    private static void leeren(Path dir) throws Exception {
        for (Path p : dateien(dir)) { Files.deleteIfExists(p); }
    }
}
