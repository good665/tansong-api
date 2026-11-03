package kr.tansong.demo.facilityclaim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import kr.tansong.demo.common.NotFoundException;

/** 사업자등록증 등 증빙 서류: 공개 폴더 밖에 저장, 형식은 내용으로 판별 */
class ClaimDocumentStorageTest {

    private static final byte[] PDF = "%PDF-1.7 content here".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path dir;

    private ClaimDocumentStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        storage = new ClaimDocumentStorage(dir.toString());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, null, content);
    }

    @Test
    void PDF를_저장하고_읽고_지운다() {
        String key = storage.store(file("../../evil.pdf", PDF));

        assertThat(key).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(storage.path(key)).exists().startsWith(dir);

        storage.delete(key);

        assertThatThrownBy(() -> storage.path(key)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void 확장자만_pdf인_파일은_거절() {
        assertThatThrownBy(() -> storage.store(file("a.pdf", "MZ-not-a-real-pdf".getBytes(StandardCharsets.US_ASCII))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 비어_있거나_20MB를_넘으면_거절() {
        assertThatThrownBy(() -> storage.store(file("a.pdf", new byte[0]))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.store(file("a.pdf", Arrays.copyOf(PDF, 20 * 1024 * 1024 + 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20MB");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret.pdf", "..\\secret.pdf", "a/b.pdf", "x.exe", ""})
    void 저장_형식이_아닌_키는_읽지도_지우지도_않는다(String key) throws IOException {
        Path outside = dir.resolveSibling("secret.pdf");
        Files.write(outside, PDF);
        try {
            assertThatThrownBy(() -> storage.path(key)).isInstanceOf(NotFoundException.class);
            storage.delete(key);
            assertThat(outside).exists();
        } finally {
            Files.deleteIfExists(outside);
        }
    }
}
