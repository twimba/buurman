package com.buurman.exception;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ProblemDetail.forStatus;
import static org.springframework.http.ProblemDetail.forStatusAndDetail;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ForbiddenException.class)
  public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, ex.getMessage());
    problem.setTitle("Forbidden");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(BadRequestException.class)
  public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, ex.getMessage());
    problem.setTitle("Bad Request");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ProblemDetail handleExternalService(
      ExternalServiceException ex, HttpServletRequest request) {
    log.error("External service error: {}", ex.getMessage(), ex);
    ProblemDetail problem = forStatusAndDetail(BAD_GATEWAY, ex.getMessage());
    problem.setTitle("Bad Gateway");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatus(BAD_REQUEST);
    problem.setTitle("Validation Failed");
    problem.setInstance(URI.create(request.getRequestURI()));

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }
    problem.setProperty("fieldErrors", fieldErrors);
    problem.setDetail("Validation failed for " + fieldErrors.size() + " field(s)");
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatus(BAD_REQUEST);
    problem.setTitle("Validation Failed");
    problem.setInstance(URI.create(request.getRequestURI()));

    Map<String, String> fieldErrors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    ConstraintViolation::getMessage,
                    (a, b) -> a,
                    LinkedHashMap::new));
    problem.setProperty("fieldErrors", fieldErrors);
    problem.setDetail("Validation failed for " + fieldErrors.size() + " field(s)");
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, "Malformed request body");
    problem.setTitle("Bad Request");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, ex.getMessage());
    problem.setTitle("Bad Request");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, "Access denied");
    problem.setTitle("Forbidden");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    String message = extractDataIntegrityMessage(ex);
    ProblemDetail problem = forStatusAndDetail(CONFLICT, message);
    problem.setTitle("Conflict");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(org.jooq.exception.IntegrityConstraintViolationException.class)
  public ProblemDetail handleJooqIntegrityConstraint(
      org.jooq.exception.IntegrityConstraintViolationException ex, HttpServletRequest request) {
    log.warn("JOOQ integrity constraint violation: {}", ex.getMessage());
    String message = extractJooqConstraintMessage(ex);
    ProblemDetail problem = forStatusAndDetail(CONFLICT, message);
    problem.setTitle("Conflict");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem =
        forStatusAndDetail(INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal Server Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  private String extractJooqConstraintMessage(
      org.jooq.exception.IntegrityConstraintViolationException ex) {
    String msg = ex.getMessage();
    if (msg == null) {
      return "A data conflict occurred";
    }
    String lower = msg.toLowerCase();
    if (lower.contains("chk_cpi_dates")) {
      return "The effective date range conflicts with an existing payment instruction";
    }
    if (lower.contains("chk_mortgage_dates_valid")) {
      return "Mortgage end date must be after the start date";
    }
    if (lower.contains("chk_cpi_custom_method") || lower.contains("chk_pi_payment_method")) {
      return "The selected payment method is not supported";
    }
    if (lower.contains("duplicate key") || lower.contains("unique constraint")) {
      return "A record with this information already exists";
    }
    return "A data conflict occurred";
  }

  private String extractDataIntegrityMessage(DataIntegrityViolationException ex) {
    String cause = ex.getMostSpecificCause().getMessage();
    if (cause == null) {
      return "A data conflict occurred";
    }

    String lowerCause = cause.toLowerCase();
    if (lowerCause.contains("uq_contract_parties_contract_tenant")) {
      return "This tenant is already a party to this contract";
    }
    if (lowerCause.contains("uq_tenants_team_email")) {
      return "A tenant with this email address already exists";
    }
    if (lowerCause.contains("duplicate key") || lowerCause.contains("unique constraint")) {
      return "A record with this information already exists";
    }
    if (lowerCause.contains("foreign key") || lowerCause.contains("is not present in table")) {
      return "This action cannot be completed because it references data that does not exist";
    }
    return "A data conflict occurred";
  }
}
