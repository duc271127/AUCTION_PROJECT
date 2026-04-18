package com.team.backend.exception;

import com.team.backend.dto.ErrorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler extended to cover common Spring MVC, validation,
 * security and persistence exceptions. Returns ErrorDto payloads with
 * appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidBidException.class)
    public ResponseEntity<ErrorDto> handleInvalidBid(InvalidBidException ex) {
        log.debug("InvalidBidException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorDto("INVALID_BID", ex.getMessage()));
    }

    @ExceptionHandler(AuctionClosedException.class)
    public ResponseEntity<ErrorDto> handleAuctionClosed(AuctionClosedException ex) {
        log.debug("AuctionClosedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("AUCTION_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(ResourceNotFoundException ex) {
        log.debug("ResourceNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorDto> handleBusiness(BusinessRuleException ex) {
        log.debug("BusinessRuleException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("BUSINESS_RULE", ex.getMessage()));
    }

    /**
     * Handle Spring Validation errors from @Valid on request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        String message = String.join("; ", fieldErrors);
        log.debug("MethodArgumentNotValidException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("VALIDATION_ERROR", message));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }

    /**
     * Handle javax.validation constraint violations (e.g., @PathVariable, @RequestParam).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));
        log.debug("ConstraintViolationException: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("CONSTRAINT_VIOLATION", message));
    }

    private String formatConstraintViolation(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath() == null ? "" : cv.getPropertyPath().toString();
        return path + ": " + cv.getMessage();
    }

    /**
     * Handle missing or malformed request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("MALFORMED_REQUEST", "Request body is malformed or unreadable"));
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParam(MissingServletRequestParameterException ex) {
        String msg = ex.getParameterName() + " parameter is missing";
        log.debug("MissingServletRequestParameterException: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("MISSING_PARAMETER", msg));
    }

    /**
     * Handle type mismatch for request parameters or path variables.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue());
        log.debug("MethodArgumentTypeMismatchException: {}", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("TYPE_MISMATCH", msg));
    }

    /**
     * Handle Spring Data integrity violations (e.g., unique constraint).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException: {}", ex.getMessage());
        String msg = "Data integrity violation";
        if (ex.getMostSpecificCause() != null) {
            msg = ex.getMostSpecificCause().getMessage();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("DATA_INTEGRITY_VIOLATION", msg));
    }

    /**
     * Handle authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorDto> handleAuthentication(AuthenticationException ex) {
        log.debug("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto("UNAUTHORIZED", "Authentication failed"));
    }

    /**
     * Handle access denied (authorization) failures.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex) {
        log.debug("AccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorDto("ACCESS_DENIED", "You do not have permission to perform this action"));
    }

    /**
     * Handle unsupported media types.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        String msg = "Unsupported media type: " + ex.getContentType();
        log.debug("HttpMediaTypeNotSupportedException: {}", msg);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ErrorDto("UNSUPPORTED_MEDIA_TYPE", msg));
    }

    /**
     * Fallback handler for all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto("INTERNAL_ERROR", message));
    }
}
