package de.kraftwerkone.overlord.monitor.security;

import static de.kraftwerkone.overlord.monitor.jooq.glassfish.Tables.MANDANT;
import static de.kraftwerkone.overlord.monitor.jooq.monitor.Tables.APP_USER_MANDANT;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

/**
 * Die Menge der Mandanten, aus der ein Nutzer waehlen darf.
 *
 * <p><b>Diese Klasse ist die einzige Ausnahme von Regel M2</b> und deshalb durchgehend mit {@link
 * OhneMandantenkontext} markiert: Sie stellt den Mandantenkontext erst <i>her</i> und kann ihn
 * folglich nicht voraussetzen. Sie liefert ausschliesslich Stammdaten ueber Mandanten selbst,
 * niemals fachliche Daten. {@code PaketstrukturTest} laesst die Markierung nur hier zu.
 *
 * <p><b>Berechtigung ist eine Menge, keine Rolle:</b> <i>Wer fuer einen Mandanten berechtigt ist,
 * darf zu ihm wechseln.</i> ADMIN ist fuer alle berechtigt und damit kein Sonderfall, sondern der
 * Nutzer mit der groessten Menge. Ein MANDANT-Nutzer mit zwei Mandanten funktioniert dadurch ohne
 * Zusatzbau — was bei {@code NEXANS}/{@code NXHBE} und {@code IBIS}/{@code IBISGUS} absehbar ist.
 *
 * <p>Gelesen wird ueber {@code glassfishDsl}: {@link #findeFuerNutzer(long)} joint
 * schemauebergreifend {@code GlassfishDB.Mandant} gegen {@code overlord_monitor.app_user_mandant},
 * und das geht nur ueber eine einzige Verbindung, also den Lese-Pool.
 *
 * <p>Die technischen Mandanten {@code SYSTEM} und {@code WOC} („Without Contract") sind keine
 * Kunden. Sie werden hier bewusst <b>nicht</b> herausgefiltert — die Menge ist genau die Menge der
 * Berechtigung, nicht eine kuratierte Kundenliste. Siehe {@code docs/mandantentrennung.md}.
 */
@Repository
public class MandantRepository {

  private final DSLContext glassfishDsl;

  MandantRepository(@Qualifier("glassfishDsl") DSLContext glassfishDsl) {
    this.glassfishDsl = glassfishDsl;
  }

  /** Sortierschluessel: Anzeigename, ersatzweise der Code — damit die Reihenfolge stabil ist. */
  private static Field<String> anzeigename() {
    return DSL.coalesce(MANDANT.MANDANTNAME, MANDANT.MANDANTID);
  }

  @OhneMandantenkontext(
      begruendung =
          "Die waehlbare Menge fuer ADMIN. Sie wird gelesen, bevor feststeht, welcher Mandant"
              + " aktiv ist — ein Kontext, der sich selbst voraussetzt, existiert nicht.")
  public List<MandantResponse> findeAlle() {
    return glassfishDsl
        .select(MANDANT.MANDANTID, anzeigename())
        .from(MANDANT)
        .orderBy(anzeigename(), MANDANT.MANDANTID)
        .fetch(satz -> new MandantResponse(satz.value1(), satz.value2()));
  }

  @OhneMandantenkontext(
      begruendung =
          "Die waehlbare Menge fuer die Rolle MANDANT. Gleicher Grund wie bei findeAlle: die"
              + " Zuordnung wird gebraucht, um den Kontext ueberhaupt setzen zu koennen.")
  public List<MandantResponse> findeFuerNutzer(long userId) {
    return glassfishDsl
        .select(MANDANT.MANDANTID, anzeigename())
        .from(MANDANT)
        .join(APP_USER_MANDANT)
        .on(APP_USER_MANDANT.MANDANT_ID.eq(MANDANT.MANDANTID))
        .where(APP_USER_MANDANT.USER_ID.eq(userId))
        .orderBy(anzeigename(), MANDANT.MANDANTID)
        .fetch(satz -> new MandantResponse(satz.value1(), satz.value2()));
  }

  @OhneMandantenkontext(
      begruendung =
          "Pruefung beim Anlegen eines Kontos (POST /api/admin/users). Dort wird ein Konto"
              + " definiert und kein Datenausschnitt abgefragt; ein aktiver Mandant des Admins"
              + " sagt nichts darueber aus, fuer welchen Mandanten das neue Konto gilt.")
  public boolean existiert(String mandantId) {
    return glassfishDsl.fetchExists(
        glassfishDsl.selectOne().from(MANDANT).where(MANDANT.MANDANTID.eq(mandantId)));
  }
}
