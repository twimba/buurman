package com.buurman.service.export.google;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import com.buurman.exception.ForbiddenException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

/**
 * Builds short-lived Google Sheets and Drive clients from a user-supplied OAuth access token. The
 * caller is responsible for the token's lifecycle — this factory never persists or caches it.
 */
@Component
public class GoogleSheetsClientFactory {

  private static final String APPLICATION_NAME = "Buurman";

  private final HttpTransport httpTransport;

  public GoogleSheetsClientFactory() {
    try {
      this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException | java.io.IOException e) {
      throw new IllegalStateException("Failed to initialize Google HTTP transport", e);
    }
  }

  public Sheets sheets(GoogleAccessToken token) {
    return new Sheets.Builder(httpTransport, GsonFactory.getDefaultInstance(), initializer(token))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public Drive drive(GoogleAccessToken token) {
    return new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), initializer(token))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  private static HttpRequestInitializer initializer(GoogleAccessToken token) {
    AccessToken accessToken = new AccessToken(token.value(), null);
    GoogleCredentials credentials =
        GoogleCredentials.create(accessToken)
            .createScoped(List.of(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE));
    HttpRequestInitializer credentialsAdapter = new HttpCredentialsAdapter(credentials);
    return new RetryingInitializer(credentialsAdapter);
  }

  /**
   * Wraps the credentials adapter to add exponential-backoff retry on transient HTTP failures (429
   * + 5xx) and on IO errors (network blips, socket resets). Bounded by {@link ExponentialBackOff}'s
   * default 15-second elapsed cap so a single Sheets call never blocks longer than its own request
   * timeout would.
   */
  private static final class RetryingInitializer implements HttpRequestInitializer {
    private final HttpRequestInitializer delegate;

    RetryingInitializer(HttpRequestInitializer delegate) {
      this.delegate = delegate;
    }

    @Override
    public void initialize(HttpRequest request) throws IOException {
      delegate.initialize(request);
      // Configure timeouts and retry behaviour. The credentials adapter installs a request
      // interceptor that sets the Authorization header; we wrap further to add backoff.
      request.setConnectTimeout(15_000);
      request.setReadTimeout(30_000);
      request.setNumberOfRetries(4);

      ExponentialBackOff backoff =
          new ExponentialBackOff.Builder()
              .setInitialIntervalMillis(500)
              .setMaxIntervalMillis(8_000)
              .setMaxElapsedTimeMillis(30_000)
              .setMultiplier(2.0)
              .build();

      HttpBackOffUnsuccessfulResponseHandler unsuccessfulHandler =
          new HttpBackOffUnsuccessfulResponseHandler(backoff);
      // Default policy retries on 5xx; broaden to also retry on 429 (rate-limited).
      unsuccessfulHandler.setBackOffRequired(
          HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ON_SERVER_ERROR);

      HttpBackOffUnsuccessfulResponseHandler rateLimitHandler =
          new HttpBackOffUnsuccessfulResponseHandler(backoff);
      rateLimitHandler.setBackOffRequired(response -> response.getStatusCode() == 429);

      // Compose: try 5xx-backoff first, then 429-backoff, then any user-installed handler.
      var existingUnsuccessful = request.getUnsuccessfulResponseHandler();
      request.setUnsuccessfulResponseHandler(
          (req, response, supportsRetry) -> {
            if (rateLimitHandler.handleResponse(req, response, supportsRetry)) {
              return true;
            }
            if (unsuccessfulHandler.handleResponse(req, response, supportsRetry)) {
              return true;
            }
            return existingUnsuccessful != null
                && existingUnsuccessful.handleResponse(req, response, supportsRetry);
          });

      var existingIo = request.getIOExceptionHandler();
      var ioHandler = new HttpBackOffIOExceptionHandler(backoff);
      request.setIOExceptionHandler(
          (req, supportsRetry) -> {
            if (ioHandler.handleIOException(req, supportsRetry)) {
              return true;
            }
            return existingIo != null && existingIo.handleIOException(req, supportsRetry);
          });
    }
  }

  /**
   * Translates a Google API client exception into a Buurman-shaped exception. Maps Google HTTP
   * statuses to the closest Buurman exception type so {@code GlobalExceptionHandler} returns a
   * sensible status to the frontend:
   *
   * <ul>
   *   <li>401 → {@link ForbiddenException} (token expired / revoked → 403 to client; the frontend
   *       prompts the user to re-authorize)
   *   <li>403 → {@link ForbiddenException} (insufficient scope, e.g. user declined drive.file)
   *   <li>429 / 5xx / network timeouts → {@link BusinessRuleException} so the client surfaces a
   *       transient/retryable error
   *   <li>everything else → {@link ExternalServiceException} (mapped to 502 by the handler)
   * </ul>
   */
  public static RuntimeException translate(String action, Exception cause) {
    if (cause instanceof com.google.api.client.googleapis.json.GoogleJsonResponseException g) {
      int status = g.getStatusCode();
      if (status == 401) {
        return new ForbiddenException(
            "Google sign-in expired while " + action + " — please re-authorize");
      }
      if (status == 403) {
        return new ForbiddenException(
            "Google denied access while " + action + " — make sure Drive access was granted");
      }
      if (status == 429) {
        return new BusinessRuleException(
            "Google API quota exceeded while " + action + " — please retry shortly");
      }
      if (status >= 500 && status <= 599) {
        return new BusinessRuleException(
            "Google API is temporarily unavailable while " + action + " — please retry");
      }
    }
    if (cause instanceof SocketTimeoutException) {
      return new BusinessRuleException("Timed out while " + action + " — please retry");
    }
    return new ExternalServiceException("Google API error while " + action, cause);
  }
}
