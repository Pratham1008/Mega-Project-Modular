package com.megaproject.common.handler;

import com.megaproject.common.dto.ErrorResponse;
import com.megaproject.common.exception.ResourceNotFoundException;
import com.megaproject.auth.exception.AuthException;
import com.megaproject.auth.exception.EmailAlreadyInUseException;
import com.megaproject.auth.exception.TokenException;
import com.megaproject.common.exception.NotConnectedException;
import com.megaproject.profile.exception.ProfileAlreadyExistsException;
import com.megaproject.profile.exception.ProfileNotFoundException;
import com.megaproject.profile.exception.UnauthorizedProfileAccessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.megaproject")
public class GlobalExceptionHandler {

    // ── Global Catch-All ──────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, HttpServletRequest req) {
        log.error("Internal Server Error: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.", req.getRequestURI());
    }

    // ── Auth exceptions ──────────────────────────────────────────────

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyInUseException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler({AuthException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuth(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> handleToken(TokenException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI());
    }

    // ── Profile exceptions ───────────────────────────────────────────

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ProfileAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfileConflict(ProfileAlreadyExistsException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(UnauthorizedProfileAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedProfile(UnauthorizedProfileAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req.getRequestURI());
    }

    // ── Common / shared exceptions ───────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Access denied", req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    // ── Chat exceptions ──────────────────────────────────────────────

    @ExceptionHandler(NotConnectedException.class)
    public ResponseEntity<ErrorResponse> handleNotConnected(NotConnectedException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    // ── Helper ───────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path));
    }
}
