package com.example.demo.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
@ControllerAdvice
public class GlobalExceptionHandler {

    // Bắt lỗi ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class) // <-- SỬA TÊN CLASS
    public ResponseEntity<ErrorDetails> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) { // <-- SỬA TÊN THAM SỐ
        
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            HttpStatus.NOT_FOUND.value(), // 404
            "Not Found",
            ex.getMessage(), // Lấy message từ exception
            request.getDescription(false).replace("uri=", "") // Lấy path
        );
        
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }
    // Bắt tất cả các lỗi khác (để chương trình không bị dừng đột ngột) [cite: 8]
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        
        ErrorDetails errorDetails = new ErrorDetails(
            new Date(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
            "Internal Server Error",
            ex.getMessage(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}