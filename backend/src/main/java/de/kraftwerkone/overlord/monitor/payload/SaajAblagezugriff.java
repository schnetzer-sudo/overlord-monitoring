package de.kraftwerkone.overlord.monitor.payload;

import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Der SOAP-{@code RETRIEVE} gegen eine Ablage, ueber SAAJ.
 *
 * <h2>Warum SAAJ und nicht ein eigener Envelope</h2>
 *
 * <p>Weil es gemessen ist. M59 (2) hat den Envelope von Hand gebaut und mit {@code curl} gesendet —
 * der Empfaenger antwortete mit einer leeren {@code FileList}, ohne Anhang und ohne {@code Fault}.
 * M71 hat denselben Verweis mit dem echten Client geholt und bekam die Datei. Zwischen beiden
 * liegen <b>zwoelf</b> Unterschiede in der Transportform (M70); <b>welcher davon den Ausschlag
 * gibt, ist nicht gemessen</b>. Solange das so ist, wird die Transportform nicht nachgebaut, auch
 * nicht mit einem HTTP-Client, der bequemere Zeitgrenzen haette.
 *
 * <h2>Warum der jakarta-Zweig</h2>
 *
 * <p>M72 hat beide Zweige gegen denselben lokalen Lauscher laufen lassen und die Bytes verglichen:
 * Anfragezeile, acht Kopfzeilen in gesendeter Reihenfolge und 276 Byte Rumpf sind identisch, die
 * einzige Abweichung liegt in der Portnummer des Lauschers. Dazu kommt ein Grund, der erst beim
 * Bauen sichtbar wird: <b>{@code javax.xml.soap.SOAPConnection} kennt {@code setReadTimeout}
 * ueberhaupt nicht.</b> Erst {@code jakarta.xml.soap} 3.x hat die beiden Methoden, und {@code
 * saaj-impl} 3.x reicht sie an die {@code HttpURLConnection} durch. Mit dem javax-Zweig gaebe es
 * eine Zeitgrenze nur JVM-weit ueber Systemeigenschaften.
 *
 * <h2>Was hier nachgestellt wird — und was nicht</h2>
 *
 * <p>Die Nachricht entsteht in derselben Reihenfolge wie in {@code FilestoreClient.java:65}–{@code
 * :77}: Kopf abtrennen, {@code m:FileList} im Namensraum {@code http://filestore.kraftwerkone.de},
 * darin ein {@code File} mit {@code Counter}, {@code ID} und {@code Action}. Dass die Attribute auf
 * der Leitung <i>alphabetisch</i> erscheinen und nicht in dieser Reihenfolge, ist eine Eigenschaft
 * der Serialisierung und aus dem Quelltext nicht ableitbar (M70, Befund 2) — genau deshalb wird
 * hier die Reihenfolge des Originals beibehalten und nicht „korrigiert".
 *
 * <p><b>Nicht nachgestellt wird der Schreibpfad.</b> Der Alt-Client legt den Anhang selbst als
 * Datei ab ({@code :112}–{@code :117}); dieses Werkzeug schreibt nichts auf Platte. Ebenso wenig
 * uebernommen wird {@code CREATE}: Dieses Werkzeug sendet ausschliesslich {@code RETRIEVE}, und die
 * Aussage von M72 gilt auch nur dafuer — bei {@code CREATE} entstuende eine mehrteilige
 * MIME-Nachricht, und die ist nicht gemessen.
 *
 * <h2>Hostnamen</h2>
 *
 * <p>Der {@code ServiceConnectString} enthaelt einen Hostnamen (Regel G1). Er erscheint in keiner
 * Fehlerantwort und in keiner Protokollzeile oberhalb von {@code DEBUG}. Die Zeilen auf {@code
 * WARN} nennen die Ablagenkennung nicht mit — die steht im Verweis und damit ebenfalls unter G1.
 */
@Component
public class SaajAblagezugriff implements Ablagezugriff {

  private static final Logger log = LoggerFactory.getLogger(SaajAblagezugriff.class);

  /** Der Namensraum der Ablage, aus {@code FilestoreClient.java:66}. */
  private static final String NAMENSRAUM = "http://filestore.kraftwerkone.de";

  private static final String ELEMENT_FILELIST = "FileList";
  private static final String ELEMENT_FILE = "File";
  private static final String ATTRIBUT_RESPONSE = "Response";

  /** Die einzige Operation, die dieses Werkzeug sendet. */
  private static final String AKTION_RETRIEVE = "RETRIEVE";

  private final RohdatenEigenschaften eigenschaften;
  private final SOAPConnectionFactory verbindungsfabrik;
  private final MessageFactory nachrichtenfabrik;

  SaajAblagezugriff(RohdatenEigenschaften eigenschaften) throws SOAPException {
    this.eigenschaften = eigenschaften;
    // Beide Fabriken sind threadsicher und teuer genug, um sie nicht je Abruf zu bauen.
    // Schlaegt das hier fehl, startet die Anwendung gar nicht erst — besser als ein Endpunkt,
    // der beim ersten Aufruf mit 500 antwortet.
    this.verbindungsfabrik = SOAPConnectionFactory.newInstance();
    this.nachrichtenfabrik = MessageFactory.newInstance();
  }

  @Override
  public Abrufergebnis hole(String verbindung, String uuid) {
    URL adresse;
    try {
      adresse = adresse(verbindung);
    } catch (MalformedURLException | URISyntaxException | IllegalArgumentException ungueltig) {
      // Eine unbrauchbare Adresse ist dasselbe wie ein Knoten, der nicht antwortet: Es gibt keinen
      // Weg zur Datei. Die Adresse selbst steht nicht in der Zeile (G1).
      log.warn("Ablage nicht erreichbar: der ServiceConnectString ist keine gueltige Adresse.");
      return Abrufergebnis.nichtErreichbar();
    }

    SOAPConnection verbindungHandle = null;
    try {
      verbindungHandle = verbindungsfabrik.createConnection();
      // Die beiden Zeilen, die es im javax-Zweig nicht gibt.
      verbindungHandle.setConnectTimeout(eigenschaften.verbindungszeitgrenzeMillis());
      verbindungHandle.setReadTimeout(eigenschaften.lesezeitgrenzeMillis());

      SOAPMessage antwort = verbindungHandle.call(anfrage(uuid), adresse);
      return auswerten(antwort);
    } catch (SOAPException | IOException fehlgeschlagen) {
      // Zeitgrenze, abgelehnte Verbindung, Status ausserhalb 2xx, unlesbare Antwort — fuer den
      // Aufrufer alles derselbe Zustand. Die Meldung geht ohne Adresse ins Protokoll; der
      // Stacktrace nur auf DEBUG, weil er den Hostnamen tragen kann.
      log.warn("Ablage nicht erreichbar: {}", fehlgeschlagen.getClass().getSimpleName());
      log.debug("Abruf fehlgeschlagen gegen {}", adresse, fehlgeschlagen);
      return Abrufergebnis.nichtErreichbar();
    } finally {
      schliesse(verbindungHandle);
    }
  }

  /**
   * Der {@code ServiceConnectString}, unveraendert.
   *
   * <p><b>Ohne Anhaengsel</b> (Q1): Er endet bei beiden gemessenen Ablagen bereits auf {@code
   * /WebApplication/FileStoreSoapReceiver} und ist die vollstaendige Adresse. Es wird nichts
   * ergaenzt, nichts abgeschnitten und keine zweite Form probiert.
   *
   * <p>Der Umweg ueber {@link URI} ist Absicht: {@code new URL(String)} ist seit Java 20 als
   * veraltet gekennzeichnet und prueft die Form nicht. Zugelassen sind ausschliesslich {@code http}
   * und {@code https} — ein {@code file:}- oder {@code jar:}-Schema in einer Stammdatenspalte waere
   * ein Weg ins eigene Dateisystem.
   */
  private static URL adresse(String verbindung) throws MalformedURLException, URISyntaxException {
    URI uri = new URI(verbindung.trim());
    String schema = uri.getScheme();
    if (schema == null || !(schema.equalsIgnoreCase("http") || schema.equalsIgnoreCase("https"))) {
      throw new IllegalArgumentException("Nur http und https sind zulaessig");
    }
    return uri.toURL();
  }

  /**
   * Die Anfrage, Zeile fuer Zeile wie {@code FilestoreClient.java:65}–{@code :77}.
   *
   * <p>Der Kopf wird abgetrennt ({@code detachNode}), sonst stuende ein leerer {@code
   * SOAP-ENV:Header} im Rumpf. Der {@code Counter} ist {@code 0}, weil je Anfrage genau eine Datei
   * geholt wird — dieses Werkzeug buendelt nicht.
   */
  private SOAPMessage anfrage(String uuid) throws SOAPException {
    SOAPMessage nachricht = nachrichtenfabrik.createMessage();
    nachricht.getSOAPHeader().detachNode();

    SOAPBody rumpf = nachricht.getSOAPBody();
    SOAPBodyElement fileList = rumpf.addBodyElement(new QName(NAMENSRAUM, ELEMENT_FILELIST, "m"));
    SOAPElement file = fileList.addChildElement(new QName(ELEMENT_FILE));
    file.addAttribute(new QName("Counter"), "0");
    file.addAttribute(new QName("ID"), uuid);
    file.addAttribute(new QName("Action"), AKTION_RETRIEVE);
    return nachricht;
  }

  /**
   * Anhang lesen, oder den Zustand benennen.
   *
   * <p><b>Kein Anhang heisst {@link Artefaktzustand#DATEI_NICHT_VORHANDEN}</b> — auch dann, wenn
   * die Antwort eine leere {@code FileList} ist. Die Ablage hat geantwortet; sie hat nur nichts
   * geliefert. Das ist der Fall, den {@code docs/rohdaten.md} §8 „Datei nicht vorhanden" nennt und
   * den M68 und M66 (2) als {@code Error (Skipped)} im Rumpf gemessen haben — nicht im HTTP-Status.
   *
   * <p>Der Wert des {@code Response}-Attributs geht ausschliesslich auf {@code DEBUG}. Er ist eine
   * Auskunft der fremden Anlage ueber sich selbst und gehoert weder in die Antwort noch ins
   * Protokoll oberhalb davon.
   */
  private Abrufergebnis auswerten(SOAPMessage antwort) throws SOAPException, IOException {
    if (antwort == null) {
      return Abrufergebnis.nichtErreichbar();
    }
    if (log.isDebugEnabled()) {
      log.debug("Antwort der Ablage: Response={}", antwortkennzeichen(antwort));
    }

    Iterator<AttachmentPart> anhaenge = antwort.getAttachments();
    if (!anhaenge.hasNext()) {
      return Abrufergebnis.nichtVorhanden();
    }

    AttachmentPart anhang = anhaenge.next();
    if (anhaenge.hasNext()) {
      // Nie beobachtet — je Anfrage geht genau eine Datei hinaus. Wird es doch einmal so weit,
      // ist der erste Anhang der zur gesendeten Kennung, und der Rest ist ein Befund fuer den
      // Betrieb, kein Grund zum Abbruch.
      log.warn("Die Ablage hat mehr als einen Anhang geliefert. Verwendet wird der erste.");
    }

    byte[] gepackt = lies(anhang);
    if (gepackt == null) {
      // Ueber der Obergrenze. Die Ablage ist erreichbar und die Datei vorhanden — was fehlt, ist
      // eine Moeglichkeit, sie zu verarbeiten. Bewusst derselbe Zustand wie „Ablage antwortet
      // nicht": In beiden Faellen liegt die Ursache ausserhalb dessen, was der Nutzer aendern kann.
      log.warn(
          "Abruf abgebrochen: der Anhang ueberschreitet die Obergrenze von {} Byte.",
          eigenschaften.maximalgroesseBytes());
      return Abrufergebnis.nichtErreichbar();
    }
    return Abrufergebnis.geholt(gepackt);
  }

  /**
   * Liest den Anhang <b>gedeckelt</b>.
   *
   * <p>Die Grenze greift waehrend des Lesens und nicht vorher. Eine Vorabpruefung ueber {@code
   * FileReader.FileProperty.Size} gaebe es nur fuer rund 69,6 % der Artefakte (M17, M60) — und eine
   * Grenze, die in einem Drittel der Faelle nicht greift, ist keine.
   *
   * @return {@code null}, wenn die Obergrenze ueberschritten ist
   */
  private byte[] lies(AttachmentPart anhang) throws SOAPException, IOException {
    long grenze = eigenschaften.maximalgroesseBytes();
    try (InputStream strom = anhang.getRawContent()) {
      ByteArrayOutputStream gesammelt = new ByteArrayOutputStream();
      byte[] puffer = new byte[8192];
      long gesamt = 0;
      int gelesen;
      while ((gelesen = strom.read(puffer)) != -1) {
        gesamt += gelesen;
        if (gesamt > grenze) {
          return null;
        }
        gesammelt.write(puffer, 0, gelesen);
      }
      return gesammelt.toByteArray();
    }
  }

  /** Das {@code Response}-Attribut des ersten {@code File}-Elements, fuer die DEBUG-Zeile. */
  private static String antwortkennzeichen(SOAPMessage antwort) {
    try {
      Iterator<?> listen =
          antwort.getSOAPBody().getChildElements(new QName(NAMENSRAUM, ELEMENT_FILELIST, "m"));
      while (listen.hasNext()) {
        Iterator<?> dateien =
            ((SOAPElement) listen.next()).getChildElements(new QName(ELEMENT_FILE));
        if (dateien.hasNext()) {
          return ((SOAPElement) dateien.next()).getAttribute(ATTRIBUT_RESPONSE);
        }
      }
      return "kein File-Element";
    } catch (SOAPException | RuntimeException unlesbar) {
      return "nicht lesbar (" + unlesbar.getClass().getSimpleName() + ")";
    }
  }

  private static void schliesse(SOAPConnection verbindung) {
    if (verbindung == null) {
      return;
    }
    try {
      verbindung.close();
    } catch (SOAPException ignoriert) {
      log.debug("Die SOAP-Verbindung liess sich nicht schliessen.", ignoriert);
    }
  }
}
