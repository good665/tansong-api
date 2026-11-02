package kr.tansong.demo.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    @Test
    void 업로드_한도_초과는_사진과_도면도_제한을_함께_안내한다() {
        var response = new GlobalExceptionHandler().handleTooLarge(new MaxUploadSizeExceededException(1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("사진은 5MB").contains("도면도는 20MB");
    }
}
