package de.kraftwerkone.overlord.monitor.bam;

import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.BAM_SOLLAENGE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sicherung gegen Drift der Sollaengen-Kuratierung — gebaut wie {@code
 * DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge}.
 *
 * <p><b>Wogegen er sichert.</b> {@code bam_sollaenge} ist am 13.08.2026 aus Messung M46 befuellt
 * worden. Ein BAM-Typ kann seine Gestalt aendern, ohne dass es jemand merkt — dann fuellt die Suche
 * weiter auf eine Laenge auf, die es nicht mehr gibt, und findet still weniger. Das ist keine
 * Befuerchtung: M43 hat es an Typ 2001 innerhalb <i>einer</i> Erhebung gezeigt (100 % ueber einen
 * Monat, 67,21 % ueber den Bestand). Dieser Test macht daraus einen roten Balken statt einer
 * stillen Verschlechterung.
 *
 * <p><b>Die Garantiestufe ist begrenzt, und das ist bewusst so.</b> {@code @Tag("db")} heisst: Die
 * CI schliesst ihn aus, weil sie das interne Netz nicht erreicht. Er laeuft, wenn jemand ihn laufen
 * laesst — dieselbe Stufe wie beim Statustest, die das Projekt bereits bewusst akzeptiert. Ein
 * Test, der nur lokal laeuft, ist mehr als kein Test und weniger als eine Zusicherung.
 *
 * <p><b>Ohne Zeitfenster, und das ist hier richtig</b> (Regel L9): Gefragt ist, ob die Sollaenge
 * ueber den <i>Bestand</i> haelt. Ein Fenster blendete genau die Zeitraeume aus, in denen sie sich
 * geaendert haben koennte.
 *
 * <p><b>Eine Abfrage je kuratiertem Paar, nicht eine ueber alle.</b> Die Sammelform aus M46-2
 * kostet ueber alle kuratierten Typen 6,8 s <i>gemessen am Client</i> und ist im Lese-Pool
 * gestorben: der setzt {@code SET SESSION max_statement_time=10} (docs/datenzugriff.md §1). Je Paar
 * bleibt jede Abfrage weit darunter — und die Fehlermeldung zeigt auf genau einen Eintrag statt auf
 * die ganze Tabelle.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("db")
class BamSollaengeDriftDbIT {

  /**
   * Die Schwelle aus dem Auftrag zu Schritt 7, Teil 2a. Sie stammt aus dem 2001-Fall: 67,21 % waere
   * bei 95 % gefangen worden.
   */
  private static final BigDecimal SCHWELLE = new BigDecimal("95.00");

  /** Der Lese-Kontext — er darf <b>beide</b> Schemata lesen (docs/datenzugriff.md §3). */
  @Autowired
  @Qualifier("glassfishDsl") private DSLContext glassfishDsl;

  @Test
  @DisplayName(
      "Sollaengen-Drift: jede kuratierte Laenge dominiert ueber den Bestand noch 95 Prozent")
  void kuratierte_sollaengen_halten_die_schwelle() {
    var kuratiert =
        glassfishDsl
            .select(
                BAM_SOLLAENGE.MANDANT_ID,
                BAM_SOLLAENGE.MESSAGE_BAM_TYPE,
                BAM_SOLLAENGE.SOLLAENGE,
                BAM_SOLLAENGE.DOMINANZ_PROZENT)
            .from(BAM_SOLLAENGE)
            .where(BAM_SOLLAENGE.SOLLAENGE.isNotNull())
            .orderBy(BAM_SOLLAENGE.MANDANT_ID, BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
            .fetch();

    assertThat(kuratiert)
        .as(
            "Die Kuratierung ist leer — dann sichert dieser Test nichts. Siehe V5__bam_sollaenge.sql")
        .isNotEmpty();

    for (var zeile : kuratiert) {
      String mandant = zeile.get(BAM_SOLLAENGE.MANDANT_ID);
      Short typ = zeile.get(BAM_SOLLAENGE.MESSAGE_BAM_TYPE);
      int sollaenge = zeile.get(BAM_SOLLAENGE.SOLLAENGE).intValue();
      String paar = mandant + "/" + typ;

      // Die Laengenverteilung dieses Paares ueber den Bestand, haeufigste zuerst. Die zweite
      // Sortierstufe macht die Wahl bei Gleichstand reproduzierbar — ohne sie waere offen, welche
      // Laenge gewinnt, und der Test schlaege zufaellig an. Dieselbe Regel wie bei der Auswahl der
      // Pruefwerte in messungen-schritt7.md.
      var verteilung =
          glassfishDsl
              .resultQuery(
                  """
                  select char_length(b.MessageBAMValue) as laenge,
                         count(*)                       as zeilen
                  from MessageBAM b
                  join Message m         on m.MessageID = b.MessageID
                  join Process p         on p.ProcessID = m.ProcessID
                  join ProjectMandant pm on pm.ProjectID = p.ProjectID
                  where b.MessageBAMType = ?
                    and pm.MandantID     = ?
                  group by 1
                  order by 2 desc, 1
                  """,
                  typ,
                  mandant)
              .fetch();

      assertThat(verteilung)
          .as(
              "Fuer %s gibt es im Bestand keine Zeile mehr. Der kuratierte Eintrag zeigt ins Leere —"
                  + " pruefe docs/bam-sollaengen.md und V5__bam_sollaenge.sql.",
              paar)
          .isNotEmpty();

      long gesamt = verteilung.stream().mapToLong(z -> z.get("zeilen", Long.class)).sum();
      int haeufigsteLaenge = verteilung.get(0).get("laenge", Integer.class);
      BigDecimal dominanz =
          BigDecimal.valueOf(verteilung.get(0).get("zeilen", Long.class))
              .multiply(BigDecimal.valueOf(100))
              .divide(BigDecimal.valueOf(gesamt), 2, RoundingMode.HALF_UP);

      assertThat(haeufigsteLaenge)
          .as(
              "%s: die haeufigste Laenge im Bestand ist %s, kuratiert ist %s. Die Suche fuellt damit"
                  + " auf eine Laenge auf, die nicht mehr die haeufigste ist.",
              paar, haeufigsteLaenge, sollaenge)
          .isEqualTo(sollaenge);

      assertThat(dominanz)
          .as(
              "%s: die Dominanz der Laenge %s ist auf %s %% gefallen (kuratiert mit %s %% am"
                  + " Messtag, n = %s). Unter 95 %% traegt die Sollaenge nicht mehr.",
              paar, sollaenge, dominanz, zeile.get(BAM_SOLLAENGE.DOMINANZ_PROZENT), gesamt)
          .isGreaterThanOrEqualTo(SCHWELLE);
    }
  }

  @Test
  @DisplayName("Leerzeichen-Drift: jedes gesetzte Kennzeichen hat im Bestand noch einen Beleg")
  void kuratierte_leerzeichen_haben_noch_einen_beleg() {
    var kuratiert =
        glassfishDsl
            .select(BAM_SOLLAENGE.MANDANT_ID, BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
            .from(BAM_SOLLAENGE)
            .where(BAM_SOLLAENGE.FUEHRENDES_LEERZEICHEN.isTrue())
            .orderBy(BAM_SOLLAENGE.MANDANT_ID, BAM_SOLLAENGE.MESSAGE_BAM_TYPE)
            .fetch();

    assertThat(kuratiert).isNotEmpty();

    for (var zeile : kuratiert) {
      String mandant = zeile.get(BAM_SOLLAENGE.MANDANT_ID);
      Short typ = zeile.get(BAM_SOLLAENGE.MESSAGE_BAM_TYPE);

      // Gefragt ist die Existenz, nicht die Zahl — deshalb LIMIT 1. Ein Zaehllauf ueber 9018
      // faende 31.193 Zeilen und braeuchte dafuer die vollen 2,3 Millionen.
      //
      // LIKE ' %' und nicht LEFT(v,1) = ' ': utf8mb4_general_ci ist PAD SPACE, dort ist
      // LEFT('',1) = ' ' wahr und der Leerstring zaehlte mit. LIKE polstert nicht (M43-3, M46-3).
      Integer beleg =
          glassfishDsl
              .resultQuery(
                  """
                  select 1
                  from MessageBAM b
                  join Message m         on m.MessageID = b.MessageID
                  join Process p         on p.ProcessID = m.ProcessID
                  join ProjectMandant pm on pm.ProjectID = p.ProjectID
                  where b.MessageBAMType = ?
                    and pm.MandantID     = ?
                    and b.MessageBAMValue like ' %'
                  limit 1
                  """,
                  typ, mandant)
              .fetchOne(0, Integer.class);

      assertThat(beleg)
          .as(
              "%s/%s traegt im Bestand keinen Wert mit fuehrendem Leerzeichen mehr. Das Kennzeichen"
                  + " kostet dann eine Suchvariante ohne Gegenwert — siehe docs/bam-sollaengen.md.",
              mandant, typ)
          .isNotNull();
    }
  }
}
