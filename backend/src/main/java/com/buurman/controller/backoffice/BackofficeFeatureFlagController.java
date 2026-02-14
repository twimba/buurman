package com.buurman.controller.backoffice;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/backoffice/feature-flags")
@Tag(name = "Backoffice - Feature Flags", description = "Feature flag status overview")
@SecurityRequirement(name = "bearer-jwt")
public class BackofficeFeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public BackofficeFeatureFlagController(FeatureFlagService featureFlagService,
                                           UserRepository userRepository,
                                           TeamMemberRepository teamMemberRepository,
                                           TeamRepository teamRepository) {
        this.featureFlagService = featureFlagService;
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    @Operation(summary = "Get global feature flag status", description = "Returns all flags at environment level (no identity context)")
    @GetMapping
    public Map<String, Object> getGlobalFlags() {
        return featureFlagService.getAllEnvironmentFlags();
    }

    public record TeamFlagEvaluation(
            String teamIdentifier,
            String teamName,
            String role,
            boolean isOwner,
            boolean isActive,
            Map<String, Object> flags
    ) {}

    @Operation(summary = "Get feature flags for a specific user across all teams",
               description = "Evaluates all flags for each team membership the user has")
    @GetMapping("/users/{userIdentifier}")
    public List<TeamFlagEvaluation> getUserFlags(@PathVariable String userIdentifier) {
        User user = userRepository.findByIdentifierUnscoped(userIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
        String identity = "user:" + user.getIdentifier();
        UUID activeTeamId = resolveActiveTeamId(user, memberships);

        List<TeamFlagEvaluation> evaluations = new ArrayList<>();

        for (TeamMember membership : memberships) {
            Team team = teamRepository.findById(membership.getTeamId()).orElse(null);
            if (team == null) continue;

            Map<String, Object> traits = new HashMap<>();
            traits.put("team", team.getIdentifier());
            traits.put("role", membership.getRole());
            traits.put("is_owner", membership.isOwner());

            Map<String, Object> flags = featureFlagService.getAllFlagsForIdentity(identity, traits);

            evaluations.add(new TeamFlagEvaluation(
                    team.getIdentifier(),
                    team.getName(),
                    membership.getRole(),
                    membership.isOwner(),
                    membership.getTeamId().equals(activeTeamId),
                    flags
            ));
        }

        return evaluations;
    }

    /**
     * Same priority logic as JwtAuthenticationConverter.selectActiveMembership:
     * activeTeamId → defaultTeamId → first membership.
     */
    private UUID resolveActiveTeamId(User user, List<TeamMember> memberships) {
        if (memberships.isEmpty()) return null;

        UUID activeTeamId = user.getActiveTeamId();
        if (activeTeamId != null && memberships.stream().anyMatch(m -> m.getTeamId().equals(activeTeamId))) {
            return activeTeamId;
        }

        UUID defaultTeamId = user.getDefaultTeamId();
        if (defaultTeamId != null && memberships.stream().anyMatch(m -> m.getTeamId().equals(defaultTeamId))) {
            return defaultTeamId;
        }

        return memberships.getFirst().getTeamId();
    }
}
