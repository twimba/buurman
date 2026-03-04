package com.buurman.service.backoffice;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.BroadcastMessage;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.User;
import com.buurman.domain.identifier.BroadcastMessageIdentifier;
import com.buurman.dto.request.backoffice.CreateBroadcastMessageRequest;
import com.buurman.dto.request.backoffice.UpdateBroadcastMessageRequest;
import com.buurman.dto.response.backoffice.BackofficeBroadcastMessageResponse;
import com.buurman.repository.BroadcastMessageRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.BackofficePrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeBroadcastMessageService {

  private final BroadcastMessageRepository repository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<BackofficeBroadcastMessageResponse> list() {
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public BackofficeBroadcastMessageResponse create(
      CreateBroadcastMessageRequest request, BackofficePrincipal principal) {
    String scope = request.scope();
    validateScope(
        scope,
        request.targetTeamIdentifiers().orElse(List.of()),
        request.targetUserIdentifiers().orElse(List.of()));

    List<UUID> teamIds = resolveTeamIds(request.targetTeamIdentifiers().orElse(List.of()));
    List<UUID> userIds = resolveUserIds(request.targetUserIdentifiers().orElse(List.of()));

    BroadcastMessage message =
        BroadcastMessage.builder()
            .title(request.title())
            .body(request.body())
            .severity(request.severity())
            .scope(scope)
            .startAt(request.startAt())
            .endAt(request.endAt())
            .showOnLogin(request.showOnLogin())
            .showOnRegister(request.showOnRegister())
            .showInApp(request.showInApp())
            .targetTeamIds(teamIds)
            .targetUserIds(userIds)
            .build();

    BroadcastMessage saved = repository.save(message);

    log.info(
        "Backoffice user {} created broadcast message {} (title={}, scope={})",
        principal.getEmail().orElse("unknown"),
        saved.getIdentifier().orElseThrow(),
        saved.getTitle(),
        saved.getScope());

    return toResponse(saved);
  }

  @Transactional
  public BackofficeBroadcastMessageResponse update(
      BroadcastMessageIdentifier identifier,
      UpdateBroadcastMessageRequest request,
      BackofficePrincipal principal) {
    String scope = request.scope();
    validateScope(
        scope,
        request.targetTeamIdentifiers().orElse(List.of()),
        request.targetUserIdentifiers().orElse(List.of()));

    List<UUID> teamIds = resolveTeamIds(request.targetTeamIdentifiers().orElse(List.of()));
    List<UUID> userIds = resolveUserIds(request.targetUserIdentifiers().orElse(List.of()));

    BroadcastMessage existing = repository.getByIdentifier(identifier);

    existing.setTitle(request.title());
    existing.setBody(request.body());
    existing.setSeverity(request.severity());
    existing.setScope(scope);
    existing.setStartAt(request.startAt());
    existing.setEndAt(request.endAt());
    existing.setShowOnLogin(request.showOnLogin());
    existing.setShowOnRegister(request.showOnRegister());
    existing.setShowInApp(request.showInApp());
    existing.setTargetTeamIds(teamIds);
    existing.setTargetUserIds(userIds);

    BroadcastMessage updated = repository.update(existing);

    log.info(
        "Backoffice user {} updated broadcast message {} (title={}, scope={})",
        principal.getEmail().orElse("unknown"),
        updated.getIdentifier().orElseThrow(),
        updated.getTitle(),
        updated.getScope());

    return toResponse(updated);
  }

  @Transactional
  public void delete(BroadcastMessageIdentifier identifier, BackofficePrincipal principal) {
    BroadcastMessage existing = repository.getByIdentifier(identifier);
    repository.delete(existing.getId());

    log.info(
        "Backoffice user {} deleted broadcast message {} (title={})",
        principal.getEmail().orElse("unknown"),
        existing.getIdentifier().orElseThrow(),
        existing.getTitle());
  }

  private BackofficeBroadcastMessageResponse toResponse(BroadcastMessage msg) {
    List<String> teamIdentifiers = resolveTeamIdentifiers(msg.getTargetTeamIds());
    List<String> userIdentifiers = resolveUserIdentifiers(msg.getTargetUserIds());

    return new BackofficeBroadcastMessageResponse(
        msg.getIdentifier().orElseThrow(),
        msg.getTitle(),
        msg.getBody(),
        msg.getSeverity(),
        msg.getScope(),
        msg.getStartAt(),
        msg.getEndAt(),
        msg.isShowOnLogin(),
        msg.isShowOnRegister(),
        msg.isShowInApp(),
        teamIdentifiers,
        userIdentifiers,
        msg.getCreatedAt(),
        msg.getUpdatedAt());
  }

  private void validateScope(
      String scope, List<String> teamIdentifiers, List<String> userIdentifiers) {
    if ("TEAMS".equals(scope) && teamIdentifiers.isEmpty()) {
      throw new IllegalArgumentException("TEAMS scope requires at least one target team");
    }
    if ("USERS".equals(scope) && userIdentifiers.isEmpty()) {
      throw new IllegalArgumentException("USERS scope requires at least one target user");
    }
  }

  private List<UUID> resolveTeamIds(List<String> identifiers) {
    return identifiers.stream()
        .map(id -> teamRepository.getByIdentifierForBackoffice(Sid.of(id)).getId())
        .toList();
  }

  private List<UUID> resolveUserIds(List<String> identifiers) {
    return identifiers.stream()
        .map(id -> userRepository.getByIdentifierUnscoped(Sid.of(id)).getId())
        .toList();
  }

  private List<String> resolveTeamIdentifiers(List<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<UUID, Team> teams =
        teamRepository.findByIds(ids).stream()
            .collect(Collectors.toMap(Team::getId, Function.identity()));
    return ids.stream()
        .map(
            id ->
                teams.containsKey(id)
                    ? teams.get(id).getIdentifier().map(Sid::value).orElse(id.toString())
                    : id.toString())
        .toList();
  }

  private List<String> resolveUserIdentifiers(List<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    Map<UUID, User> users =
        userRepository.findByIds(ids).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    return ids.stream()
        .map(
            id ->
                users.containsKey(id)
                    ? users.get(id).getIdentifier().map(Sid::value).orElse(id.toString())
                    : id.toString())
        .toList();
  }
}
