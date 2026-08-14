package de.kraftwerkone.overlord.monitor.bam;

import de.kraftwerkone.overlord.monitor.security.AngemeldeterNutzer;
import de.kraftwerkone.overlord.monitor.security.MandantContext;
import de.kraftwerkone.overlord.monitor.security.MandantService;
import de.kraftwerkone.overlord.monitor.security.SitzungsVerwaltung;
import java.beans.PropertyEditorSupport;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die BAM-Suche — <b>wer eine Belegnummer hat, findet die Nachrichten, auf denen sie steht</b>.
 * Auch ohne die führende Null, und auch mit mehreren Nummern gleichzeitig.
 *
 * <p><b>Er nimmt keine Mandanten-ID entgegen</b> (Regel M1). Der Mandant kommt aus der Sitzung, und
 * zwar über {@code MandantService}: Der prüft den Sitzungswert gegen die zulässige Menge, statt ihm
 * zu glauben. Die Ausnahmeliste in {@code docs/mandantentrennung.md} §3 bleibt bei zwei Einträgen
 * und wächst hier nicht.
 *
 * <p><b>Die Parameter sind roh und werden hier nicht ausgewertet.</b> Geprüft wird an einer Stelle,
 * in {@link BamSuchfilter#aus}; alles Unbrauchbare ist dort {@code 400} mit eigenem Problemtyp. Der
 * Controller übersetzt nur HTTP.
 *
 * <p><b>Die eine Ausnahme davon ist die Bindung des Parameters {@code begriff}</b>, und sie steht
 * hier, weil sie hier gebrochen war: Ein Komma im Wert ist Teil des Werts und kein Trennzeichen
 * ({@link #einParameterIstEinBegriff}). Das ist die <i>Annahme</i> des Parameters und nicht seine
 * Auswertung — geprüft wird weiterhin nur in {@code BamSuchfilter}.
 *
 * <p><b>Warum {@code /api/bam/suche} und nicht {@code /api/nachrichten/suche}.</b> Der Endpunkt
 * beantwortet eine eigene fachliche Frage — <i>wo steht diese Nummer</i> — und nicht eine Spielart
 * der Liste. Er hat kein Zeitraum-Kürzel, keinen Status- und keinen Prozessfilter, keinen Cursor
 * und ein anderes Standardfenster; unter {@code /api/nachrichten} sähe er aus wie derselbe Endpunkt
 * mit anderen Parametern. <b>Der Belegdaten-Block aus Teil 1 liegt aus dem umgekehrten Grund unter
 * {@code /api/nachrichten}</b>: Er beantwortet eine Frage <i>zu einer benannten Nachricht</i>.
 */
@RestController
public class BamSucheController {

  private final BamSucheService bamSucheService;
  private final MandantService mandantService;
  private final SitzungsVerwaltung sitzungsVerwaltung;
  private final Clock anwendungsuhr;

  BamSucheController(
      BamSucheService bamSucheService,
      MandantService mandantService,
      SitzungsVerwaltung sitzungsVerwaltung,
      Clock anwendungsuhr) {
    this.bamSucheService = bamSucheService;
    this.mandantService = mandantService;
    this.sitzungsVerwaltung = sitzungsVerwaltung;
    this.anwendungsuhr = anwendungsuhr;
  }

  /**
   * <b>Ein Anfrageparameter ist ein Begriff — auch wenn sein Wert ein Komma trägt.</b>
   *
   * <p>Das ist keine Feinheit, sondern die Behebung eines Defekts. Spring bindet einen
   * {@code @RequestParam} auf eine Liste, indem es einen <i>einzeln</i> gesetzten Parameter am
   * <b>Komma</b> zerlegt; ein BAM-Wert mit Komma zerfiel damit in zwei Begriffe, von denen der
   * zweite keinen Pflichttrenner mehr trug — die Antwort war {@code 400
   * suchbegriff-ohne-typtrenner}. <b>M51 zählt, wen das traf</b>: 55.989 Werte des Bestands (0,363
   * %), davon <b>54.096 unter dem einen Typ 9003</b> („Material-Nr. beim Lieferanten"). Der Defekt
   * lebte seit Teil 2b und betraf beide Suchmodi; er steht in {@code docs/annahmen-korrekturen.md}.
   *
   * <p><b>Mehrere Begriffe kommen weiterhin als mehrfach gesetzter Parameter</b> — {@code
   * ?begriff=…&begriff=…}, unverändert. Der Editor greift nur, wenn Spring einen <i>einzelnen</i>
   * Wert vor sich hat; bei zwei gesetzten Parametern liegt bereits ein {@code String[]} vor, und er
   * bleibt unangetastet.
   *
   * <p><b>Der Schnitt ist eng, und zwar mit Absicht.</b> {@code @InitBinder} gilt für <i>diesen</i>
   * Controller und für keinen anderen; dieser Controller hat genau einen Endpunkt und genau einen
   * Listenparameter. <b>Die Nachrichtenliste trennt weiterhin am Komma</b> ({@code status}, {@code
   * prozess}) — eine projektweite Umstellung träfe Anmeldung, Liste und Administration mit, und
   * keiner dieser Pfade hat darum gebeten. Warum das dort folgenlos ist, steht in {@code
   * docs/bam-suche.md} §9.
   *
   * <p><b>Am Statement ändert sich nichts.</b> Das Komma ist in {@code =} wie in {@code LIKE} ein
   * gewöhnliches Zeichen und kein Platzhalter; maskiert werden nach wie vor nur {@code \}, {@code
   * %} und {@code _} ({@link Suchbedingung#alsMuster}).
   */
  @InitBinder
  void einParameterIstEinBegriff(WebDataBinder binder) {
    binder.registerCustomEditor(String[].class, new EinWertEinEintrag());
  }

  /**
   * Der Editor, der die Kommazerlegung ersetzt: <b>ein Parameterwert wird ein Eintrag</b>, Zeichen
   * für Zeichen.
   *
   * <p>Er steht als geschachtelte Klasse und nicht daneben, damit sichtbar bleibt, wie weit er
   * reicht — <b>nur so weit wie dieser Controller</b>. Wer ihn anderswo braucht, soll ihn dort
   * bewusst registrieren und nicht versehentlich erben.
   */
  private static final class EinWertEinEintrag extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      setValue(new String[] {text});
    }
  }

  /**
   * @param begriff <b>wiederholt</b>, je in der Form {@code <typ>:<wert>}. Der Doppelpunkt ist
   *     Pflicht; ohne Typ lautet der Parameter {@code :4711815}. Mindestens einer, höchstens {@link
   *     BamSuchfilter#HOECHSTENS_BEGRIFFE}. <b>Ein Komma im Wert ist Teil des Werts</b> und kein
   *     Trennzeichen — siehe {@link #einParameterIstEinBegriff}
   * @param von Beginn des Zeitfensters, ISO 8601 in UTC — nur zusammen mit {@code bis}
   * @param bis Ende des Zeitfensters. Fehlen beide, gilt {@link BamSuchfilter#FENSTER_VORGABE}; das
   *     tatsächlich verwendete Fenster steht in der Antwort
   * @param modus {@code exakt} (Vorgabe) oder {@code praefix}. <b>Er kommt roh als Zeichenkette und
   *     nicht als Aufzählungstyp herein</b>, damit ein unbekannter Wert der Fehler dieses Endpunkts
   *     ist — mit eigenem Problemtyp und deutschem Text — und nicht die Typumwandlung von Spring.
   *     <b>Er gilt für die ganze Suche und nicht je Begriff</b>: Ein Kennzeichen im Begriff
   *     bräuchte ein zweites Trennzeichen, und ob BAM-Werte es enthalten, wäre nach Regel Q4
   *     geraten (M49‑4 zählt 585 Werte mit Doppelpunkt)
   */
  @GetMapping("/api/bam/suche")
  public BamSucheResponse suche(
      @RequestParam(required = false) String[] begriff,
      @RequestParam(required = false) String von,
      @RequestParam(required = false) String bis,
      @RequestParam(required = false) String modus) {

    MandantContext mandant = mandantService.aktuellerKontext(erforderlicherNutzer());
    List<String> begriffe = begriff == null ? null : Arrays.asList(begriff);
    return bamSucheService.suche(
        mandant, BamSuchfilter.aus(begriffe, von, bis, modus, anwendungsuhr));
  }

  private AngemeldeterNutzer erforderlicherNutzer() {
    return sitzungsVerwaltung
        .aktuellerNutzer()
        .orElseThrow(() -> new IllegalStateException("Endpunkt ohne Anmeldung erreicht"));
  }
}
