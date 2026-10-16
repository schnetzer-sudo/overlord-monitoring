package de.kraftwerkone.overlord.monitor.task;

import de.kraftwerkone.overlord.monitor.security.Zugriffszaehler;
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
 * Haengt einen {@link Zugriffszaehler} an den <b>Schreib</b>-Kontext, ohne seine vorhandenen
 * Listener zu ersetzen — das Gegenstueck zu {@code security/Zugriffszaehlung}, die nur den
 * Lese-Kontext kennt.
 *
 * <p><b>Warum hier der Schreib-Kontext.</b> Die Auftraege liegen in {@code overlord_monitor} und
 * werden ausschliesslich ueber {@code monitorDsl} gelesen. Die Zusage „fremd und unbekannt sind
 * ununterscheidbar“ haengt deshalb an den Statements dort ({@code docs/mandantentrennung.md} §5,
 * „Woran die Zusage tatsaechlich haengt“). Die Sitzung schreibt Spring Session ueber JDBC und nicht
 * ueber jOOQ; sie erscheint im Zaehler nicht.
 *
 * <p>Angebracht ueber {@code @Import(SchreibkontextZaehlung.class)}; das kostet einen eigenen
 * Anwendungskontext, und kein anderer Test schleppt den Zaehler mit. Oeffentlich, weil seit A2 auch
 * {@code AssistentIsolationDbIT} ihn nutzt.
 */
@TestConfiguration
public class SchreibkontextZaehlung {

  @Bean
  Zugriffszaehler zugriffszaehler() {
    return new Zugriffszaehler();
  }

  @Bean
  static BeanPostProcessor zaehlerAnDenSchreibkontext(ObjectProvider<Zugriffszaehler> zaehler) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if ("monitorDsl".equals(name) && bean instanceof DSLContext dsl) {
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
