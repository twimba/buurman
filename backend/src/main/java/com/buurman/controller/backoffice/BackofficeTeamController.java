package com.buurman.controller.backoffice;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.UpdateTeamNameRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamDetailResponse;
import com.buurman.dto.response.backoffice.BackofficeTeamResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.backoffice.BackofficeTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backoffice/teams")
@Tag(name = "Backoffice - Teams", description = "Platform-wide team management")
@SecurityRequirement(name = "bearer-jwt")
public class BackofficeTeamController {

    private final BackofficeTeamService backofficeTeamService;

    public BackofficeTeamController(BackofficeTeamService backofficeTeamService) {
        this.backofficeTeamService = backofficeTeamService;
    }

    @Operation(summary = "List teams", description = "Get all teams with optional name search and pagination")
    @GetMapping
    public PageResponse<BackofficeTeamResponse> listTeams(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction) {
        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        return backofficeTeamService.listTeams(pageRequest, search);
    }

    @Operation(summary = "Get team", description = "Get detailed team overview by identifier")
    @GetMapping("/{identifier}")
    public BackofficeTeamDetailResponse getTeam(@PathVariable String identifier) {
        return backofficeTeamService.getTeam(identifier);
    }

    @Operation(summary = "Update team name", description = "Update the name of a team")
    @PutMapping("/{identifier}")
    public BackofficeTeamResponse updateTeamName(
            @PathVariable String identifier,
            @Valid @RequestBody UpdateTeamNameRequest request,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        return backofficeTeamService.updateTeamName(identifier, request, principal);
    }

    @Operation(summary = "Delete team", description = "Soft delete a team")
    @DeleteMapping("/{identifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(
            @PathVariable String identifier,
            @AuthenticationPrincipal BackofficePrincipal principal) {
        backofficeTeamService.deleteTeam(identifier, principal);
    }
}
