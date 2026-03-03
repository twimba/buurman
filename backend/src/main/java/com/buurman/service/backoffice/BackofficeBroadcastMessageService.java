package com.buurman.service.backoffice;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.BroadcastMessage;
import com.buurman.dto.request.backoffice.CreateBroadcastMessageRequest;
import com.buurman.dto.request.backoffice.UpdateBroadcastMessageRequest;
import com.buurman.dto.response.backoffice.BackofficeBroadcastMessageResponse;
import com.buurman.repository.BroadcastMessageRepository;
import com.buurman.security.BackofficePrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeBroadcastMessageService {

  private final BroadcastMessageRepository repository;

  @Transactional(readOnly = true)
  public List<BackofficeBroadcastMessageResponse> list() {
    return repository.findAll().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public BackofficeBroadcastMessageResponse create(
      CreateBroadcastMessageRequest request, BackofficePrincipal principal) {
    BroadcastMessage message = BroadcastMessage.builder()
        .title(request.title())
        .body(request.body())
        .severity(request.severity())
        .startAt(request.startAt())
        .endAt(request.endAt())
        .showOnLogin(request.showOnLogin())
        .showOnRegister(request.showOnRegister())
        .showInApp(request.showInApp())
        .build();

    BroadcastMessage saved = repository.save(message);

    log.info(
        "Backoffice user {} created broadcast message {} (title={})",
        principal.getEmail().orElse("unknown"),
        saved.getIdentifier(),
        saved.getTitle());

    return toResponse(saved);
  }

  @Transactional
  public BackofficeBroadcastMessageResponse update(
      String identifier, UpdateBroadcastMessageRequest request, BackofficePrincipal principal) {
    BroadcastMessage existing = repository.getByIdentifier(identifier);

    existing.setTitle(request.title());
    existing.setBody(request.body());
    existing.setSeverity(request.severity());
    existing.setStartAt(request.startAt());
    existing.setEndAt(request.endAt());
    existing.setShowOnLogin(request.showOnLogin());
    existing.setShowOnRegister(request.showOnRegister());
    existing.setShowInApp(request.showInApp());

    BroadcastMessage updated = repository.update(existing);

    log.info(
        "Backoffice user {} updated broadcast message {} (title={})",
        principal.getEmail().orElse("unknown"),
        updated.getIdentifier(),
        updated.getTitle());

    return toResponse(updated);
  }

  @Transactional
  public void delete(String identifier, BackofficePrincipal principal) {
    BroadcastMessage existing = repository.getByIdentifier(identifier);
    repository.delete(existing.getId());

    log.info(
        "Backoffice user {} deleted broadcast message {} (title={})",
        principal.getEmail().orElse("unknown"),
        existing.getIdentifier(),
        existing.getTitle());
  }

  private BackofficeBroadcastMessageResponse toResponse(BroadcastMessage msg) {
    return new BackofficeBroadcastMessageResponse(
        msg.getIdentifier(),
        msg.getTitle(),
        msg.getBody(),
        msg.getSeverity(),
        msg.getStartAt(),
        msg.getEndAt(),
        msg.isShowOnLogin(),
        msg.isShowOnRegister(),
        msg.isShowInApp(),
        msg.getCreatedAt(),
        msg.getUpdatedAt());
  }
}
