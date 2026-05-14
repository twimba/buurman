package com.buurman.controller;

import static com.buurman.util.FeatureFlags.GOOGLE_SHEETS_EXPORT;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.GoogleAccessToken;
import com.buurman.domain.GoogleSheetExport;
import com.buurman.domain.identifier.DataTakeoutIdentifier;
import com.buurman.dto.request.GoogleSheetExportRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.GoogleSheetExportResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.TakeoutResponse;
import com.buurman.exception.ForbiddenException;
import com.buurman.generated.api.DataTakeoutApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.GoogleSheetTitleResolver;
import com.buurman.service.TakeoutGoogleSheetService;
import com.buurman.service.TakeoutService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TakeoutController implements DataTakeoutApi {

  private final TakeoutService takeoutService;
  private final TakeoutGoogleSheetService takeoutGoogleSheetService;
  private final FeatureFlagService featureFlagService;
  private final GoogleSheetTitleResolver titleResolver;

  @Override
  public TakeoutResponse requestTakeout() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.requestTakeout(
        principal.requireTeamId(), principal.requireTeamIdentifier(), principal.getUserId());
  }

  @Override
  public PageResponse listTakeouts(Optional<Integer> page, Optional<Integer> size) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.listTakeouts(
        principal.requireTeamId(),
        PageRequest.of(page.orElse(null), size.orElse(null), null, (String) null));
  }

  @Override
  public TakeoutResponse getTakeout(DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return takeoutService.getTakeout(identifier, principal.requireTeamId());
  }

  @Override
  public void deleteTakeout(DataTakeoutIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    takeoutService.deleteTakeout(identifier, principal.requireTeamId());
  }

  @Override
  public GoogleSheetExportResponse exportTakeoutGoogleSheet(GoogleSheetExportRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    if (featureFlagService.isDisabled(GOOGLE_SHEETS_EXPORT, principal)) {
      throw new ForbiddenException("Google Sheets export feature is not available");
    }
    GoogleSheetExport result =
        takeoutGoogleSheetService.export(
            new GoogleAccessToken(request.getAccessToken()),
            principal.requireTeamId(),
            titleResolver.resolveTakeout(principal));
    return new GoogleSheetExportResponse(result.spreadsheetId(), result.url());
  }
}
