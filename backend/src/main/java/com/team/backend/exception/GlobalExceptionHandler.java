package com.team.backend.exception;

import com.team.backend.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidBidException.class)
    public ResponseEntity<ErrorDto> handleInvalidBid(InvalidBidException ex) {
        return ResponseEntity.badRequest().body(new ErrorDto("INVALID_BID", ex.getMessage()));
    }

    @ExceptionHandler(AuctionClosedException.class)
    public ResponseEntity<ErrorDto> handleAuctionClosed(AuctionClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto("AUCTION_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorDto> handleBusiness(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("BUSINESS_RULE", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto("INTERNAL_ERROR", ex.getMessage()));
    }
}
