package kr.tansong.demo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** 도면도 저장 규칙: 파일 내용으로 형식 판별, 20MB 제한. 일반 사진의 5MB 제한은 그대로 */
class ImageStorageServiceTest {

    private static final byte[] PNG_HEAD = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0};

    @TempDir
    Path dir;

    private ImageStorageService storage;

    @BeforeEach
    void setUp() throws IOException {
        storage = new ImageStorageService(dir.toString());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, null, content);
    }

    private static byte[] sized(byte[] head, int size) {
        return Arrays.copyOf(head, size);
    }

    @Test
    void PDF_내용이면_PDF로_저장() {
        var stored = storage.storeFloorPlan(1L, file("plan.pdf", "%PDF-1.7 content here".getBytes(StandardCharsets.US_ASCII)));

        assertThat(stored.fileType()).isEqualTo("PDF");
        assertThat(stored.url()).startsWith("/uploads/facilities/1/plans/").endsWith(".pdf");
    }

    @Test
    void PNG_내용이면_IMAGE로_저장() {
        var stored = storage.storeFloorPlan(1L, file("plan.png", PNG_HEAD));

        assertThat(stored.fileType()).isEqualTo("IMAGE");
        assertThat(stored.url()).endsWith(".png");
    }

    @Test
    void 확장자만_pdf인_파일은_거절() {
        assertThatThrownBy(() -> storage.storeFloorPlan(1L, file("a.pdf", "MZ-not-a-real-pdf".getBytes(StandardCharsets.US_ASCII))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void WEBP는_도면도로_거절() {
        byte[] webp = "RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> storage.storeFloorPlan(1L, file("a.webp", webp)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 도면도_20MB_초과는_거절() {
        assertThatThrownBy(() -> storage.storeFloorPlan(1L, file("big.png", sized(PNG_HEAD, 20 * 1024 * 1024 + 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20MB");
    }

    @Test
    void 기존_사진의_5MB_제한은_유지() {
        assertThatThrownBy(() -> storage.storeFacilityImage(1L, file("big.png", sized(PNG_HEAD, 6 * 1024 * 1024))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }
}
