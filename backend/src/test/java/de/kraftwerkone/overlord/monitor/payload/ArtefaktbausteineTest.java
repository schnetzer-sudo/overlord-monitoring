package de.kraftwerkone.overlord.monitor.payload;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die kleinen Bausteine des Rohdatenzugriffs: Namensmuster, Verweisform, Binaererkennung,
 * ZIP-Entnahme, Dateiname.
 *
 * <p>Alle ohne Datenbank und ohne Filestore. Die Bytes hier sind erfunden.
 */
class ArtefaktbausteineTest {

  @Nested
  @DisplayName("Artefaktnamen")
  class Namen {

    @Test
    @DisplayName("Die beiden gemessenen Muster werden erkannt (M54)")
    void muster() {
      assertThat(Artefaktnamen.art("FileReader.Payload.GUID")).isEqualTo(Artefaktart.NUTZDATEN);
      assertThat(Artefaktnamen.art("FTPSender.Log.GUID")).isEqualTo(Artefaktart.PROTOKOLL);
      assertThat(Artefaktnamen.art("Message.Payload.GUID")).isEqualTo(Artefaktart.NUTZDATEN);
    }

    @Test
    @DisplayName("Alles andere ist kein Artefakt")
    void kein_artefakt() {
      assertThat(Artefaktnamen.art("Message.SendingPartner")).isNull();
      assertThat(Artefaktnamen.art("FileReader.FileProperty.Size")).isNull();
      assertThat(Artefaktnamen.art(".Payload.GUID")).as("ohne Familie").isNull();
      assertThat(Artefaktnamen.art(null)).isNull();
      assertThat(Artefaktnamen.istArtefakt("Message.Payload.GUID")).isTrue();
      assertThat(Artefaktnamen.istArtefakt("Message.SplitCount")).isFalse();
    }

    @Test
    @DisplayName("Die Familie ist der Teil vor dem Muster — unveraendert, ohne Deutung")
    void familie() {
      assertThat(Artefaktnamen.familie("FileReader.Payload.GUID")).isEqualTo("FileReader");
      assertThat(Artefaktnamen.familie("FTPSender.Log.GUID")).isEqualTo("FTPSender");
      assertThat(Artefaktnamen.familie("Message.Payload.GUID")).isEqualTo("Message");
      assertThat(Artefaktnamen.familie("Message.SplitCount")).isNull();
    }

    @Test
    @DisplayName("Der Eingang ist genau Message.Payload.GUID, schreibungsempfindlich")
    void eingang() {
      assertThat(Artefaktnamen.istEingang("Message.Payload.GUID")).isTrue();
      assertThat(Artefaktnamen.istEingang("message.payload.guid")).isFalse();
      assertThat(Artefaktnamen.istEingang("Converter.Payload.GUID")).isFalse();
    }

    @Test
    @DisplayName("Die LIKE-Muster enthalten kein _ — das waere ein zweiter Platzhalter")
    void keine_unterstriche() {
      assertThat(Artefaktnamen.likeMuster())
          .allSatisfy(muster -> assertThat(muster).doesNotContain("_"));
    }
  }

  @Nested
  @DisplayName("Artefaktverweis")
  class Verweis {

    @Test
    @DisplayName("Zerlegt wird an der Pipe (M54)")
    void zerlegen() {
      assertThat(Artefaktverweis.zerlege("BEISPIELPROD42|deadbeef-0000-4000-8000-0123456789ab"))
          .contains(new Artefaktverweis("BEISPIELPROD42", "deadbeef-0000-4000-8000-0123456789ab"));
    }

    @Test
    @DisplayName("Was nicht der Form entspricht, wird nicht erraten")
    void unbrauchbar() {
      assertThat(Artefaktverweis.zerlege(null)).isEmpty();
      assertThat(Artefaktverweis.zerlege("")).isEmpty();
      assertThat(Artefaktverweis.zerlege("ohne-pipe")).isEmpty();
      assertThat(Artefaktverweis.zerlege("|nur-uuid")).isEmpty();
      assertThat(Artefaktverweis.zerlege("nur-ablage|")).isEmpty();
      assertThat(Artefaktverweis.zerlege("a|b|c")).as("mehr als eine Pipe").isEmpty();
    }

    @Test
    @DisplayName("Er verraet sich nicht ueber toString")
    void nicht_im_protokoll() {
      Artefaktverweis verweis = new Artefaktverweis("BEISPIELPROD42", "eine-uuid");

      assertThat(verweis.toString()).doesNotContain("BEISPIELPROD42").doesNotContain("eine-uuid");
    }
  }

  @Nested
  @DisplayName("Binaerpruefung")
  class Binaer {

    @Test
    @DisplayName("Ein Nullbyte genuegt")
    void nullbyte() {
      byte[] daten = "Text mit\0Nullbyte".getBytes(StandardCharsets.ISO_8859_1);

      assertThat(Binaerpruefung.istBinaer(daten)).isTrue();
    }

    @Test
    @DisplayName("Reiner ASCII-Text ist kein Binaerinhalt")
    void ascii() {
      byte[] daten =
          "2025-12-29 10:00:00 ERFUNDEN-Zeile\nnoch eine\n".getBytes(StandardCharsets.ISO_8859_1);

      assertThat(Binaerpruefung.istBinaer(daten)).isFalse();
    }

    @Test
    @DisplayName(
        "Umlaute in ISO-8859-1 sind kein Binaerinhalt — sonst waere die Mehrheit der"
            + " Protokolle eine Binaerdatei (M61)")
    void umlaute() {
      byte[] daten = "Erfundene Grüße aus Köln\n".getBytes(StandardCharsets.ISO_8859_1);

      assertThat(Binaerpruefung.istBinaer(daten)).isFalse();
    }

    @Test
    @DisplayName("Ueberwiegend Steuerzeichen ist Binaerinhalt")
    void steuerzeichen() {
      byte[] daten = new byte[100];
      for (int i = 0; i < daten.length; i++) {
        // 0x01 bis 0x1F ohne TAB/LF/CR: unbelegte Steuerzeichen.
        daten[i] = (byte) (0x01 + (i % 8));
      }

      assertThat(Binaerpruefung.istBinaer(daten)).isTrue();
    }

    @Test
    @DisplayName("Eine leere Datei ist keine Binaerdatei — das kleinste Artefakt hat 2 Byte (M60)")
    void leer() {
      assertThat(Binaerpruefung.istBinaer(new byte[0])).isFalse();
      assertThat(Binaerpruefung.istBinaer(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("Zipentnahme")
  class Zip {

    private static byte[] zip(String... eintraege) throws IOException {
      ByteArrayOutputStream gesammelt = new ByteArrayOutputStream();
      try (ZipOutputStream strom = new ZipOutputStream(gesammelt)) {
        for (int i = 0; i < eintraege.length; i++) {
          strom.putNextEntry(new ZipEntry("erfunden-" + i + ".dat"));
          strom.write(eintraege[i].getBytes(StandardCharsets.ISO_8859_1));
          strom.closeEntry();
        }
      }
      return gesammelt.toByteArray();
    }

    @Test
    @DisplayName("Ein Eintrag — der gemessene Regelfall (693 von 693)")
    void ein_eintrag() throws IOException {
      Zipentnahme.Inhalt inhalt = Zipentnahme.ersterEintrag(zip("Erfundener Inhalt"), 1024);

      assertThat(inhalt).isNotNull();
      assertThat(inhalt.eintraege()).isEqualTo(1);
      assertThat(new String(inhalt.daten(), StandardCharsets.ISO_8859_1))
          .isEqualTo("Erfundener Inhalt");
    }

    @Test
    @DisplayName("Zwei Eintraege — der erste wird verwendet, die Zahl wird vermerkt")
    void zwei_eintraege() throws IOException {
      Zipentnahme.Inhalt inhalt = Zipentnahme.ersterEintrag(zip("erster", "zweiter"), 1024);

      assertThat(inhalt).isNotNull();
      assertThat(new String(inhalt.daten(), StandardCharsets.ISO_8859_1)).isEqualTo("erster");
      assertThat(inhalt.eintraege())
          .as(
              "Das Altsystem verwirft den Rest stillschweigend (:801–:802). Hier steht die Zahl in"
                  + " der Antwort — der Fall ist in 693 Dateien nie aufgetreten")
          .isEqualTo(2);
    }

    @Test
    @DisplayName("Die Groessengrenze greift waehrend des Lesens")
    void grenze() throws IOException {
      byte[] archiv = zip("x".repeat(5000));

      assertThat(Zipentnahme.ersterEintrag(archiv, 5000)).isNotNull();
      assertThat(Zipentnahme.ersterEintrag(archiv, 4999))
          .as("Gezaehlt werden die Bytes, die herauskommen — nicht die Angabe im Archiv")
          .isNull();
    }

    @Test
    @DisplayName("Ein unlesbares oder leeres Archiv ergibt nichts")
    void unlesbar() {
      assertThat(Zipentnahme.ersterEintrag(null, 1024)).isNull();
      assertThat(Zipentnahme.ersterEintrag(new byte[0], 1024)).isNull();
      assertThat(Zipentnahme.ersterEintrag("kein ZIP".getBytes(StandardCharsets.UTF_8), 1024))
          .isNull();
    }
  }

  @Nested
  @DisplayName("Downloaddateiname")
  class Dateiname {

    @Test
    @DisplayName("Der Originalname wird verwendet, wo er vorhanden ist")
    void originalname() {
      assertThat(
              Downloaddateiname.baue(
                  "ERFUNDEN-Beleg.001", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .isEqualTo("ERFUNDEN-Beleg.001");
    }

    @Test
    @DisplayName("Sonst ein konstruierter — und nicht die MessageID allein wie im Altsystem (Q4)")
    void konstruiert() {
      String name = Downloaddateiname.baue(null, "FTPSender.Log.GUID", (short) 4, "eine-nachricht");

      assertThat(name).isEqualTo("FTPSender-schritt4-protokoll-eine-nachricht");
      assertThat(name)
          .as(
              "Keine erfundene Endung: 4.307 von 4.352 Originalnamen enden auf eine reine"
                  + " Ziffernfolge (M56 c) — die echten Namen tragen selbst keine Typangabe")
          .doesNotEndWith(".txt")
          .doesNotEndWith(".log");
    }

    @Test
    @DisplayName("Ein Zeilenumbruch im Originalnamen wuerde den Kopf spalten — er wird entfernt")
    void kopfspaltung() {
      String name =
          Downloaddateiname.baue(
              "boese\r\nX-Kopf: wert", "FileReader.Payload.GUID", (short) 1, "eine-nachricht");

      assertThat(name).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    @DisplayName("Pfadangaben im Originalnamen werden entschaerft")
    void pfadwechsel() {
      assertThat(
              Downloaddateiname.baue(
                  "../../etc/passwd", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .doesNotContain("/")
          .doesNotStartWith(".");
      assertThat(
              Downloaddateiname.baue(
                  "C:\\Windows\\system32", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .doesNotContain("\\")
          .doesNotContain(":");
    }

    @Test
    @DisplayName("Unsichtbare Formatzeichen werden entfernt — sie drehen den Namen um")
    void formatzeichen() {
      String rtlOverride = String.valueOf((char) 0x202E);
      String ohneBreite = String.valueOf((char) 0x200B);

      assertThat(
              Downloaddateiname.baue(
                  "rechnung" + rtlOverride + "gpj.exe",
                  "FileReader.Payload.GUID",
                  (short) 1,
                  "eine-nachricht"))
          .as("U+202E ist unsichtbar und laesst .exe im Speichern-Dialog wie .jpg aussehen")
          .doesNotContain(rtlOverride);
      assertThat(
              Downloaddateiname.baue(
                  "a" + ohneBreite + "b", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .as("Zeichen ohne Breite ebenso")
          .doesNotContain(ohneBreite);
      assertThat(
              Downloaddateiname.baue(
                  "Grüße.001", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .as("Umlaute bleiben — entfernt werden Formatzeichen, nicht alles Nicht-ASCII")
          .isEqualTo("Grüße.001");
    }

    @Test
    @DisplayName("Ein leerer Originalname faellt auf den konstruierten zurueck")
    void leer() {
      assertThat(
              Downloaddateiname.baue("   ", "FileReader.Payload.GUID", (short) 1, "eine-nachricht"))
          .isEqualTo("FileReader-schritt1-nutzdaten-eine-nachricht");
    }

    @Test
    @DisplayName("Der Originalname wird ueber das Muster gesucht, nicht ueber die Familie")
    void muster_statt_familie() {
      assertThat(Downloaddateiname.likeMuster())
          .as(
              "Nur die Lesedienste tragen den Namen (M56 a) — ueber die Familie des Eingangs"
                  + " gesucht (Message.FileProperty.OriginalFilename) faende man nie etwas")
          .isEqualTo("%.FileProperty.OriginalFilename");
      assertThat(Downloaddateiname.likeMuster())
          .as("Kein _ darin — das waere ein zweiter Platzhalter")
          .doesNotContain("_");
    }
  }
}
