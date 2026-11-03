package kr.tansong.demo.facilityclaim;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import kr.tansong.demo.common.ImageStorageService;
import kr.tansong.demo.common.NotFoundException;

/**
 * 사업자등록증 등 증빙 서류. /uploads 와 달리 공개 경로로 내보내지 않는 폴더에 저장합니다.
 * 파일명은 UUID 로 새로 만들고 형식은 파일 내용으로 판별합니다.
 */
@Component
public class ClaimDocumentStorage {

    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("jpg", "png", "pdf");
    private static final Pattern KEY = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|pdf)$");

    private final Path root;

    public ClaimDocumentStorage(@Value("${app.upload.private-dir:./private-uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("증빙 서류를 첨부해 주세요.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("증빙 서류는 20MB 이하만 업로드할 수 있습니다.");
        }
        try {
            String type = ImageStorageService.detectType(file);
            if (!TYPES.contains(type)) {
                throw new IllegalArgumentException("증빙 서류는 JPG, PNG, PDF 파일만 업로드할 수 있습니다.");
            }
            String key = UUID.randomUUID() + "." + type;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, root.resolve(key));
            }
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("증빙 서류를 저장하지 못했습니다.", e);
        }
    }

    public Path path(String key) {
        if (key == null || !KEY.matcher(key).matches() || !Files.exists(root.resolve(key))) {
            throw new NotFoundException("서류를 찾을 수 없습니다.");
        }
        return root.resolve(key);
    }

    public MediaType mediaType(String key) {
        if (key.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    public void delete(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(key));
        } catch (IOException ignored) {
            // 삭제 실패가 승인·거절 결과를 되돌리지는 않음
        }
    }
}
