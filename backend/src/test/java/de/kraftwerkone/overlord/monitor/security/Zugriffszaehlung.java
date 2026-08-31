package de.kraftwerkone.overlord.monitor.security;

import java.util.Arrays;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Haengt einen {@link Zugriffszaehler} an den vorhandenen Lese-Kontext, <b>ohne ihn zu
 * ersetzen</b>.
 *
 * <p><b>Nur {@code glassfishDsl}.</b> Der Schreib-Kontext bleibt aussen vor; das Sitzungs- und
 * Protokollschreiben in {@code overlord_monitor} laeuft ueber ihn und haette mit der Frage nichts
 * zu tun.
 *
 * <p><b>Angehaengt und nicht ersetzt.</b> Der {@code ReadOnlyExecuteListener} aus {@code
 * JooqConfig} ist die dritte Schicht des Schreibschutzes ({@code PROJEKTBESCHREIBUNG.md} §6) und
 * darf nicht verlorengehen; ein Ersetzen haette ihn fuer die Dauer der Testklasse stillgelegt.
 *
 * <p>Angebracht wird sie ueber {@code @Import(Zugriffszaehlung.class)} an der Testklasse. Sie steht
 * ausschliesslich in {@code src/test} und aendert am Anwendungscode nichts; sie kostet einen
 * eigenen Anwendungskontext, und das ist der Preis dafuer, dass kein anderer Test einen fremden
 * Zaehler mitschleppt.
 *
 * <h2>Warum sie hier steht und nicht zweimal in je einer Testklasse</h2>
 *
 * <p>Seit dem 31.08.2026 brauchen sie <b>zwei</b> Isolationstests — {@code BamIsolationDbIT} und
 * {@code KettenIsolationDbIT}. Zwei Abschriften desselben Mechanismus waeren zwei Stellen, an denen
 * dieselbe Bedingung steht, und eine Bedingung an zwei Stellen driftet. Der Zaehler ist ausserdem
 * kein Beiwerk, sondern traegt in beiden Faellen die Sicherheitsaussage.
 *
 * @see Zugriffszaehler
 */
@TestConfiguration
public class Zugriffszaehlung {

  @Bean
  Zugriffszaehler zugriffszaehler() {
    return new Zugriffszaehler();
  }

  @Bean
  static BeanPostProcessor zaehlerAnDenLesekontext(ObjectProvider<Zugriffszaehler> zaehler) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if ("glassfishDsl".equals(name) && bean instanceof DSLContext dsl) {
          Configuration cfg = dsl.configuration();
          ExecuteListenerProvider[] vorhanden = cfg.executeListenerProviders();
          ExecuteListenerProvider[] erweitert = Arrays.copyOf(vorhanden, vorhanden.length + 1);
          erweitert[vorhanden.length] = new DefaultExecuteListenerProvider(zaehler.getObject());
          cfg.set(erweitert);
        }
        return bean;
      }
    };
  }
}
