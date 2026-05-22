package messenger.bereza.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "bereza")
@Data
public class BerezaProperties {

    private Security security = new Security();
    private Storage storage = new Storage();
    private Geo geo = new Geo();
    private Websocket websocket = new Websocket();

    @Data
    public static class Security {
        private int bcryptStrength = 12;
        private String rememberMeKey = "change-me";
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class Storage {
        private String root = "./var/storage";
        private int maxFileSizeMb = 25;
        private List<String> allowedMime = List.of();
    }

    @Data
    public static class Geo {
        private int defaultSrid = 4326;
    }

    @Data
    public static class Websocket {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }
}
