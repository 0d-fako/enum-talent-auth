package com.enumm.exception;

import com.enumm.dtos.response.ErrorResponse;
import com.enumm.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        String traceId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        ErrorResponse.ErrorDetail errorDetail = ErrorResponse.ErrorDetail.builder()
                .code(ex.getErrorCode().name())
                .message(ex.getMessage())
                .details(ex.getDetails() != null ? (List<ErrorResponse.FieldError>) ex.getDetails() : null)
                .traceId(traceId)
                .build();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorDetail)
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(ErrorResponse.FieldError.builder()
                    .field(error.getField())
                    .rule("format")
                    .format(error.getDefaultMessage())
                    .build());
        }

        ErrorResponse.ErrorDetail errorDetail = ErrorResponse.ErrorDetail.builder()
                .code(ErrorCode.VALIDATION_ERROR.name())
                .message("Validation failed. Check your details and try again.")
                .details(fieldErrors)
                .traceId(traceId)
                .build();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorDetail)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = "req_" + UUID.randomUUID().toString().substring(0, 8);

        ErrorResponse.ErrorDetail errorDetail = ErrorResponse.ErrorDetail.builder()
                .code(ErrorCode.INTERNAL_ERROR.name())
                .message("An unexpected error occurred. Please try again later.")
                .traceId(traceId)
                .build();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(errorDetail)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}