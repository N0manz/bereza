package messenger.bereza.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageInitializer implements CommandLineRunner {

    private final BerezaProperties props;

    @Override
    public void run(String... args) throws IOException {
        Path root = Paths.get(props.getStorage().getRoot()).toAbsolutePath();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
            log.info("Создана директория файлового хранилища: {}", root);
        }
    }
}
