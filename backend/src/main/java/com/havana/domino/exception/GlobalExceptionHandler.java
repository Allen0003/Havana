package com.havana.domino.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全域例外處理器。
 * 使用 RFC 7807 {@link ProblemDetail} 作為統一錯誤回應格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — 房間不存在 */
    @ExceptionHandler(RoomNotFoundException.class)
    public ProblemDetail handleRoomNotFound(RoomNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Room Not Found");
        detail.setProperty("error", "ROOM_NOT_FOUND");
        return detail;
    }

    /** 409 — 房間已滿 */
    @ExceptionHandler(RoomFullException.class)
    public ProblemDetail handleRoomFull(RoomFullException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Room Full");
        detail.setProperty("error", "ROOM_FULL");
        return detail;
    }

    /** 400 — Bean Validation 失敗 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setTitle("Validation Failed");
        detail.setProperty("error", "VALIDATION_ERROR");
        return detail;
    }

    /** 400 — 非法出牌 / 邏輯錯誤 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Bad Request");
        detail.setProperty("error", "INVALID_REQUEST");
        return detail;
    }
}
