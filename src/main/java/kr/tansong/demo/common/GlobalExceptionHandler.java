package kr.tansong.demo.common;

import java.util.Map;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 예외를 {success:false, message} 형태로 통일 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<?> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                Map.of("success", false, "message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 파라미터 누락 / 타입 불일치 / JSON 형식 오류 -> 500 이 아니라 400
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class })
    public ResponseEntity<?> handleMalformed(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 존재하지 않는 /uploads/** 경로 등 정적 리소스 404 (Exception.class 에 잡혀 500 이 되지 않도록)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResource(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.");
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    // 같은 시설·시간대에 동시 예약이 몰려 DB 교착 상태(deadlock)로 트랜잭션이 희생된 경우.
    // 요청 자체는 유효하므로 500 이 아니라, 다시 시도하면 대부분 성공하는 409 로 안내합니다.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<?> handleConcurrencyFailure(ConcurrencyFailureException e) {
        return body(HttpStatus.CONFLICT, "요청이 일시적으로 몰렸습니다. 잠시 후 다시 시도해 주세요.");
    }

    // FK 위반(없는 userId/facilityId 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleIntegrity(DataIntegrityViolationException e) {
        return body(HttpStatus.BAD_REQUEST, "요청한 정보를 확인해 주세요.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception e) {
        e.printStackTrace();
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
    }
}
