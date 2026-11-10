package com.unavaliability.backend.exception;

import com.unavaliability.backend.util.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(body(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Dados inválidos.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest request) {
        log.warn("Acesso negado [traceId={}] {} {}", currentTraceId(), request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body("Acesso negado."));
    }

    //Erros de requisição feitos pelo cliente
    
    @ExceptionHandler({
            ErrorResponseException.class,
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleClientError(Exception ex, HttpServletRequest request) {
        HttpStatusCode status = ex instanceof ErrorResponse er ? er.getStatusCode() : HttpStatus.BAD_REQUEST;
        if (status.is5xxServerError()) {
            log.error("Erro HTTP {} [traceId={}] {} {}", status.value(), currentTraceId(),
                    request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.debug("Requisição rejeitada {} [traceId={}] {} {}", status.value(), currentTraceId(),
                    request.getMethod(), request.getRequestURI());
        }
        String msg = status.value() == HttpStatus.NOT_FOUND.value()
                ? "Recurso não encontrado."
                : "Requisição inválida.";
        return ResponseEntity.status(status).body(body(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado [traceId={}] {} {}", currentTraceId(),
                request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body("Erro interno. Tente novamente."));
    }

    private static Map<String, Object> body(String message) {
        return Map.of("error", message, "traceId", currentTraceId());
    }

    private static String currentTraceId() {
        String id = MDC.get(TraceIdFilter.MDC_KEY);
        return id != null ? id : "indisponivel";
    }
}
