package com.evcharging.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One place that turns exceptions into a consistent JSON body: {"error": "...", "status": 400, "timestamp": "..."}.
 * The frontend reads the "error" field and shows it to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status.value());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage() == null ? f.getField() + " is invalid" : f.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return build(HttpStatus.BAD_REQUEST, message.isBlank() ? "Invalid request." : message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleUnreadable(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request data.");
    }

    @ExceptionHandler({IllegalArgumentException.class, BatteryException.class, PaymentException.class,
            WalletException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({BookingNotFoundException.class, VehicleNotFoundException.class, UserNotFoundException.class,
            ChargingStationNotFoundException.class, ChargingSessionNotFoundException.class,
            PaymentNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, DuplicateResourceException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "This action conflicts with existing data. Please refresh and try again.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage() == null ? "Access denied." : ex.getMessage());
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, Object>> handleTooMany(TooManyAttemptsException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGateway(PaymentGatewayException ex) {
        log.error("Payment gateway error", ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /** Spring's own web exceptions (404 route, 405 method, missing parameter ...) keep their proper status. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            HttpStatus safe = status == null ? HttpStatus.BAD_REQUEST : status;
            String detail = er.getBody().getDetail();
            return build(safe, detail == null ? safe.getReasonPhrase() : detail);
        }
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
    }
}
