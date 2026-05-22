package messenger.bereza.web.controller;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Attachment;
import messenger.bereza.domain.AttachmentCategory;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.FileStorageService;
import messenger.bereza.web.dto.file.AttachmentView;
import messenger.bereza.web.mapper.MessageMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;
    private final CurrentUserProvider currentUser;
    private final MessageMapper messageMapper;

    /** Загрузка файла перед привязкой к сообщению. */
    @PostMapping
    public AttachmentView upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "category", required = false) AttachmentCategory category) {
        Attachment a = storage.store(file, currentUser.currentUser(), category);
        return messageMapper.toAttachmentView(a);
    }

    /** Скачать файл по id (требуется аутентификация — авторизация проверяется в сервисе при привязке к чату). */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment a = storage.getMetadata(id);
        Resource res = storage.loadAsResource(a);
        String encodedName = URLEncoder.encode(a.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedName)
                .body(res);
    }
}
