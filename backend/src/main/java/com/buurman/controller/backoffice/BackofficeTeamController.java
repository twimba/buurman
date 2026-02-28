package com.buurman.controller.backoffice;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.UpdateTeamNameRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeTeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/teams")
@Tag(name = "Backoffice - Teams", description = "Platform-wide team management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeTeamController {

  private final BackofficeTeamService backofficeTeamService;

  @Operation(
      summary = "List teams",
      description = "Get all teams with optional name search and pagination")
  @GetMapping
  public PageResponse<BackofficeTeamResponse> listTeams(
      @Parameter(description = "Search term") @RequestParam Optional<String> search,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          Integer page,
      @Parameter(description = "Page size", example = "25") @RequestParam(defaultValue = "25")
          Integer size,
      @Parameter(description = "Sort field name", example = "createdAt") @RequestParam
          Optional<String> sort,
      @Parameter(description = "Sort direction", example = "DESC")
          @RequestParam(defaultValue = "DESC")
          SortDirection direction) {
    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return backofficeTeamService.listTeams(pageRequest, search.orElse(null));
  }

  @Operation(summary = "Get team", description = "Get detailed team overview by identifier")
  @GetMapping("/{identifier}")
  public BackofficeTeamDetailResponse getTeam(
      @Parameter(description = "Team ULID identifier") @PathVariable String identifier) {
    return backofficeTeamService.getTeam(identifier);
  }

  @Operation(summary = "Update team name", description = "Update the name of a team")
  @PutMapping("/{identifier}")
  public BackofficeTeamResponse updateTeamName(
      @Parameter(description = "Team ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody UpdateTeamNameRequest request,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    return backofficeTeamService.updateTeamName(identifier, request, principal);
  }

  @Operation(summary = "Delete team", description = "Soft delete a team")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteTeam(
      @Parameter(description = "Team ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    backofficeTeamService.deleteTeam(identifier, principal);
  }
}
