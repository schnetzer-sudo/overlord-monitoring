package de.kraftwerkone.overlord.monitor.config;

import de.kraftwerkone.overlord.monitor.security.PasswortwechselInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Verdrahtung der Web-Schicht. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final PasswortwechselInterceptor passwortwechselInterceptor;

  WebConfig(PasswortwechselInterceptor passwortwechselInterceptor) {
    this.passwortwechselInterceptor = passwortwechselInterceptor;
  }

  /**
   * Der Aenderungszwang gilt fuer alles unter {@code /api} — die drei Ausnahmen kennt der
   * Interceptor selbst. Bewusst nicht hier aufgezaehlt: Sonst stuende die Liste an zwei Stellen und
   * eine davon liefe irgendwann der anderen hinterher.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(passwortwechselInterceptor).addPathPatterns("/api/**");
  }
}
