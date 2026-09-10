package de.kraftwerkone.overlord.monitor.dashboard;

/**
 * Warum die Ablagenkachel {@link Ablagenzustand#UNGEKLAERT} zeigt — <b>ein benannter Grund, nie ein
 * leeres Feld</b>.
 *
 * <p>Bei {@link Ablagenzustand#ERREICHBAR} und {@link Ablagenzustand#NICHT_ERREICHBAR} ist er
 * {@code null}: Dort gibt es nichts zu erklaeren, die Pruefung hat geantwortet.
 *
 * <h2>Entscheidung E‑125: „ungeklaert" ist nicht „rot"</h2>
 *
 * <p><b>Grün darf seinen Beleg nicht ueberleben</b> — aber Rot darf es genauso wenig. Ein Stand,
 * fuer den es keinen frischen Beleg gibt, sagt <i>nichts</i> ueber die Ablagen, und zwar in <b>jede
 * Richtung</b>. Eine Kachel, die nach einem Ausfall der Pruefung stumm auf dem letzten roten Stand
 * stehen bliebe, behauptete eine Stoerung, die niemand mehr geprueft hat; eine, die auf dem letzten
 * gruenen stehen bliebe, behauptete das Gegenteil. <b>Beides ist dieselbe falsche Auskunft.</b>
 */
public enum Ablagengrund {

  /**
   * Die Pruefung ist abgeschaltet — {@code overlord.ablagenpruefung.aktiv} ist nicht {@code true},
   * oder der Schluessel fehlt.
   *
   * <p><b>Das ist der Normalfall im Profil {@code dev}</b> und im reinen Rollup-Prozess: Ein
   * zweiter Prozess derselben Anwendung soll nicht im Minutentakt fremde Knoten anfragen, nur weil
   * er dieselbe {@code jar} startet ({@code docs/dienste.md} §14).
   */
  ABGESCHALTET,

  /**
   * Die Pruefung laeuft, hat aber noch keinen Durchgang beendet.
   *
   * <p>Der Fall dauert hoechstens einen Takt und tritt nach jedem Neustart auf. <b>Er ist etwas
   * anderes als „abgeschaltet"</b>: Hier kommt gleich eine Antwort, dort nie.
   */
  NOCH_KEIN_DURCHGANG,

  /**
   * Der letzte Stand ist aelter als <b>zwei Takte</b>.
   *
   * <p>Zwei und nicht einer: Ein Durchgang, der etwas laenger braucht als sein Takt, ist kein
   * Befund — ein Abruf gegen eine abgeschaltete Ablage dauert allein rund 2,7 Sekunden (M174,
   * Befund 3). Zwei Takte lassen einen vollstaendigen Durchgang ausfallen, bevor die Kachel
   * schweigt.
   *
   * <p><b>Das ist die Stelle, an der Gruen seinen Beleg verliert</b> — und zwar von selbst, ohne
   * dass jemand etwas abschalten muesste.
   */
  STAND_VERALTET,

  /**
   * Es ist kein Ziel eingetragen: {@code ServiceDefaultFileStore} ist in <b>jeder</b> Zeile von
   * {@code Service} leer.
   *
   * <p><b>Eine Kachel ohne Ziel darf nicht gruen sein.</b> „Kein Ziel nicht erreichbar" ist formal
   * wahr und als Auskunft wertlos — sie saehe genauso aus wie „alle Ablagen antworten".
   */
  KEIN_ZIEL_EINGETRAGEN,

  /**
   * Mindestens ein Ziel hat auf die Null-UUID <b>Daten geliefert</b>.
   *
   * <p>Erwartet ist „Datei nicht vorhanden" (M174). Kommt stattdessen ein Anhang, verhaelt sich der
   * Knoten anders als gemessen — die Kachel sagt das, statt es zu deuten (Regel Q4).
   */
  ZIEL_UNGEKLAERT
}
