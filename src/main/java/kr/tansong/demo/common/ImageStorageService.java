package kr.tansong.demo.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 시설 사진을 서버 디스크에 저장합니다.
 * - 파일명은 UUID 로 새로 만들고 확장자는 "내용(매직 바이트)"으로 판별합니다.
 *   (사용자가 보낸 파일명/Content-Type 을 믿지 않음 -> 경로 조작, 위장 파일 방지)
 */
@Service
public class ImageStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public ImageStorageService(@Value("${app.upload.dir:./uploads}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path getRoot() {
        return root;
    }

    /** 저장 후 공개 경로(/uploads/...)를 반환 */
    public String storeFacilityImage(Long facilityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지를 선택해 주세요.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("이미지는 5MB 이하만 업로드할 수 있습니다.");
        }

        try {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            }
            if (read < head.length) {
                throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
            }

            String ext = detectExtension(head);

            Path dir = root.resolve("facilities").resolve(String.valueOf(facilityId)).normalize();
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + "." + ext;

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename));
            }

            return "/uploads/facilities/" + facilityId + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", e);
        }
    }

    public void deleteQuietly(String publicPath) {
        if (publicPath == null || !publicPath.startsWith("/uploads/")) {
            return; // 외부 URL(샘플 이미지 등)은 파일이 없으므로 무시
        }

        Path target = root.resolve(publicPath.substring("/uploads/".length())).normalize();

        if (!target.startsWith(root)) {
            return; // 업로드 폴더 밖은 절대 지우지 않음
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 파일 삭제 실패는 사용자 요청 실패로 보지 않음
        }
    }

    private String detectExtension(byte[] h) {
        boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';

        if (jpeg) return "jpg";
        if (png) return "png";
        if (webp) return "webp";

        throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
    }
}
