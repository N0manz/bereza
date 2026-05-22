package messenger.bereza.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableJdbcHttpSession(
        tableName = "SPRING_SESSION",
        maxInactiveIntervalInSeconds = 60 * 60 * 24 * 30  // 30 дней
)
public class SessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer s = new DefaultCookieSerializer();
        s.setCookieName("BEREZA_SESSION");
        s.setUseHttpOnlyCookie(true);
        s.setSameSite("Lax");
        s.setUseSecureCookie(Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_SECURE", "false")));
        s.setCookiePath("/");
        return s;
    }
}
