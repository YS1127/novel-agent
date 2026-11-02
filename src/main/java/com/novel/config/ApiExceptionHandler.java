package com.novel.config;

import com.novel.ai.AiServiceException;
import com.novel.story.StoryConflictException;
import com.novel.story.StoryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String code, String message) {}

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> badRequest(Exception error) {
        String message = error instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult().getFieldErrors().stream().findFirst()
                    .map(field -> field.getField() + "：" + field.getDefaultMessage())
                    .orElse("请求参数无效")
                : error.getMessage();
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", message));
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<ApiError> notFound(StoryNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", error.getMessage()));
    }

    @ExceptionHandler(StoryConflictException.class)
    public ResponseEntity<ApiError> conflict(StoryConflictException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("CONFLICT", error.getMessage()));
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiError> aiFailure(AiServiceException error) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError("AI_FAILURE", error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> missingConfiguration(IllegalStateException error) {
        return ResponseEntity.badRequest().body(new ApiError("CONFIGURATION_REQUIRED", error.getMessage()));
    }
}
