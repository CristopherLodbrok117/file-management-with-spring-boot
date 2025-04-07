package app.file_manager.web.exception;

import app.file_manager.domain.exception.FileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(FileException.class)
    ResponseEntity<Map<String,String>> fileExceptionHandler(FileException ex){
        return retrieveErrorResponse(HttpStatus.BAD_REQUEST,
                "file error", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> multipartMaxSizeExceptionHandler(MaxUploadSizeExceededException ex){
        return retrieveErrorResponse(HttpStatus.BAD_REQUEST,
                "Multipart file error", ex.getMessage());
    }

    ResponseEntity<Map<String, String>> retrieveErrorResponse(HttpStatus statusCode,
                                                              String errorType, String message){
        return ResponseEntity.status(statusCode)
                .body(Map.of(errorType, message));
    }
}
