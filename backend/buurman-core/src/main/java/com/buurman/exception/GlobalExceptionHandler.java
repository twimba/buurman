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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(NotFoundException.class)
  public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(NOT_FOUND, ex.getMessage());
    problem.setTitle(msg("error.not-found.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ForbiddenException.class)
  public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, ex.getMessage());
    problem.setTitle(msg("error.forbidden.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ImpersonationRestrictionException.class)
  public ProblemDetail handleImpersonationRestriction(
      ImpersonationRestrictionException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, ex.getMessage());
    problem.setTitle(msg("error.impersonation.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ReauthenticationRequiredException.class)
  public ProblemDetail handleReauthRequired(
      ReauthenticationRequiredException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, ex.getMessage());
    problem.setTitle(msg("error.reauth.title"));
    problem.setProperty("error", msg("error.reauth.detail"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(CONFLICT, ex.getMessage());
    problem.setTitle(msg("error.conflict.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(LeaseNotAvailableException.class)
  public ProblemDetail handleLeaseNotAvailable(
      LeaseNotAvailableException ex, HttpServletRequest request) {
    ProblemDetail problem = handleBusinessRule(ex, request);
    problem.setProperty("code", ex.getCode());
    return problem;
  }

  @ExceptionHandler(BadRequestException.class)
  public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, ex.getMessage());
    problem.setTitle(msg("error.bad-request.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(ExternalServiceException.class)
  public ProblemDetail handleExternalService(
      ExternalServiceException ex, HttpServletRequest request) {
    log.error("External service error: {}", ex.getMessage(), ex);
    ProblemDetail problem = forStatusAndDetail(BAD_GATEWAY, ex.getMessage());
    problem.setTitle(msg("error.bad-gateway.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(DocumentRenderException.class)
  public ProblemDetail handleDocumentRender(
      DocumentRenderException ex, HttpServletRequest request) {
    log.error("Document rendering failed: {}", ex.getMessage(), ex);
    ProblemDetail problem =
        forStatusAndDetail(INTERNAL_SERVER_ERROR, msg("error.document-render.detail"));
    problem.setTitle(msg("error.document-render.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatus(BAD_REQUEST);
    problem.setTitle(msg("error.validation.title"));
    problem.setInstance(URI.create(request.getRequestURI()));

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }
    problem.setProperty("fieldErrors", fieldErrors);
    problem.setDetail(msg("error.validation.detail", fieldErrors.size()));
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatus(BAD_REQUEST);
    problem.setTitle(msg("error.validation.title"));
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
    problem.setDetail(msg("error.validation.detail", fieldErrors.size()));
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, msg("error.malformed-request.detail"));
    problem.setTitle(msg("error.bad-request.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(NOT_FOUND, ex.getMessage());
    problem.setTitle(msg("error.not-found.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(BAD_REQUEST, ex.getMessage());
    problem.setTitle(msg("error.bad-request.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    ProblemDetail problem = forStatusAndDetail(FORBIDDEN, msg("error.access-denied.detail"));
    problem.setTitle(msg("error.forbidden.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    String message = extractDataIntegrityMessage(ex);
    ProblemDetail problem = forStatusAndDetail(CONFLICT, message);
    problem.setTitle(msg("error.conflict.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(org.jooq.exception.IntegrityConstraintViolationException.class)
  public ProblemDetail handleJooqIntegrityConstraint(
      org.jooq.exception.IntegrityConstraintViolationException ex, HttpServletRequest request) {
    log.warn("JOOQ integrity constraint violation: {}", ex.getMessage());
    String message = extractJooqConstraintMessage(ex);
    ProblemDetail problem = forStatusAndDetail(CONFLICT, message);
    problem.setTitle(msg("error.conflict.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem = forStatusAndDetail(INTERNAL_SERVER_ERROR, msg("error.internal.detail"));
    problem.setTitle(msg("error.internal.title"));
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }

  private String extractJooqConstraintMessage(
      org.jooq.exception.IntegrityConstraintViolationException ex) {
    String rawMsg = ex.getMessage();
    if (rawMsg == null) {
      return msg("error.data-conflict.default");
    }
    String lower = rawMsg.toLowerCase(Locale.ROOT);
    if (lower.contains("chk_cpi_dates")) {
      return msg("error.constraint.payment-instruction-dates");
    }
    if (lower.contains("chk_mortgage_dates_valid")) {
      return msg("error.constraint.mortgage-dates");
    }
    if (lower.contains("chk_cpi_custom_method") || lower.contains("chk_pi_payment_method")) {
      return msg("error.constraint.payment-method");
    }
    if (lower.contains("duplicate key") || lower.contains("unique constraint")) {
      return msg("error.data-conflict.duplicate");
    }
    return msg("error.data-conflict.default");
  }

  private String extractDataIntegrityMessage(DataIntegrityViolationException ex) {
    String cause = ex.getMostSpecificCause().getMessage();
    if (cause == null) {
      return msg("error.data-conflict.default");
    }

    String lowerCause = cause.toLowerCase(Locale.ROOT);
    if (lowerCause.contains("uq_contract_parties_contract_contact")) {
      return msg("error.data-conflict.contract-party");
    }
    if (lowerCause.contains("uq_contacts_team_email")) {
      return msg("error.data-conflict.contact-email");
    }
    if (lowerCause.contains("duplicate key") || lowerCause.contains("unique constraint")) {
      return msg("error.data-conflict.duplicate");
    }
    if (lowerCause.contains("foreign key") || lowerCause.contains("is not present in table")) {
      return msg("error.data-conflict.foreign-key");
    }
    return msg("error.data-conflict.default");
  }

  private String msg(String code) {
    return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
  }

  private String msg(String code, Object... args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }
}
