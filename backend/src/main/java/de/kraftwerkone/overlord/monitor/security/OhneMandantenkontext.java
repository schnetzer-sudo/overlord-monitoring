package de.kraftwerkone.overlord.monitor.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert die wenigen Methoden, die auf {@code jooq.glassfish} zugreifen und <b>keinen</b> {@link
 * MandantContext} als ersten Parameter haben duerfen — weil sie den Kontext erst <i>herstellen</i>.
 *
 * <p>Ohne diese Markierung waere Regel M2 nicht umsetzbar: Die Menge der zulaessigen Mandanten muss
 * gelesen werden, <b>bevor</b> feststeht, welcher Mandant aktiv ist. Ein Kontext, der sich selbst
 * voraussetzt, existiert nicht.
 *
 * <p><b>Die Markierung ist eine Ausnahme, kein Werkzeug.</b> {@code PaketstrukturTest} laesst sie
 * ausschliesslich in {@link MandantRepository} zu und wird rot, sobald sie anderswo auftaucht —
 * genauso wie die Liste der zwei Endpunkt-Ausnahmen in {@code docs/mandantentrennung.md}
 * vollstaendig bleiben muss. Wer sie setzt, gibt eine Begruendung an.
 *
 * <p>Keine so markierte Methode liefert jemals fachliche Daten (Nachrichten, Prozesse, Projekte).
 * Sie liefern ausschliesslich Stammdaten ueber Mandanten selbst.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OhneMandantenkontext {

  /** Warum diese Methode ohne Mandantenkontext auskommen muss. */
  String begruendung();
}
