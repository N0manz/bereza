package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messenger.bereza.config.BerezaProperties;
import messenger.bereza.domain.Attachment;
import messenger.bereza.domain.AttachmentCategory;
import messenger.bereza.domain.User;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.AttachmentRepository;
import org.apache.tika.Tika;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final AttachmentRepository attachmentRepository;
    private final BerezaProperties props;
    private final Tika tika = new Tika();

    @Transactional
    public Attachment store(MultipartFile file, User uploader, AttachmentCategory category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл не предоставлен");
        }
        long maxBytes = props.getStorage().getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Файл превышает лимит " + props.getStorage().getMaxFileSizeMb() + "MB");
        }

        String mime;
        try (InputStream is = file.getInputStream()) {
            mime = tika.detect(is, file.getOriginalFilename());
        } catch (IOException e) {
            throw new BadRequestException("Не удалось прочитать файл");
        }
        if (!props.getStorage().getAllowedMime().contains(mime)) {
            throw new BadRequestException("Запрещённый тип файла: " + mime);
        }

        try {
            Path root = Paths.get(props.getStorage().getRoot()).toAbsolutePath();
            LocalDate today = LocalDate.now();
            String key = "%04d/%02d/%s-%s".formatted(
                    today.getYear(), today.getMonthValue(),
                    UUID.randomUUID(),
                    sanitize(file.getOriginalFilename()));
            Path target = root.resolve(key);
            Files.createDirectories(target.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String checksum = sha256(target);
            AttachmentCategory cat = category != null
                    ? category
                    : inferCategory(mime);

            Attachment a = Attachment.builder()
                    .uploader(uploader)
                    .storageKey(key)
                    .originalName(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename())
                    .mimeType(mime)
                    .sizeBytes(file.getSize())
                    .checksumSha256(checksum)
                    .category(cat)
                    .build();
            return attachmentRepository.save(a);
        } catch (IOException e) {
            log.error("File store failed", e);
            throw new BadRequestException("Ошибка сохранения файла");
        }
    }

    @Transactional(readOnly = true)
    public Attachment getMetadata(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
    }

    public Resource loadAsResource(Attachment a) {
        Path root = Paths.get(props.getStorage().getRoot()).toAbsolutePath();
        Path path = root.resolve(a.getStorageKey());
        if (!Files.exists(path)) {
            throw new NotFoundException("Файл отсутствует на диске");
        }
        return new FileSystemResource(path);
    }

    private static String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String sha256(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static AttachmentCategory inferCategory(String mime) {
        if (mime == null) return AttachmentCategory.GENERIC;
        if (mime.startsWith("image/"))    return AttachmentCategory.IMAGE;
        if (mime.equals("application/pdf")) return AttachmentCategory.PDF;
        return AttachmentCategory.GENERIC;
    }
}
