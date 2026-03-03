package com.buurman.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.BroadcastMessage;
import com.buurman.dto.response.BroadcastMessageResponse;
import com.buurman.repository.BroadcastMessageDismissalRepository;
import com.buurman.repository.BroadcastMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BroadcastMessageService {

  private final BroadcastMessageRepository repository;
  private final BroadcastMessageDismissalRepository dismissalRepository;

  @Transactional(readOnly = true)
  public List<BroadcastMessageResponse> getActiveMessages(UUID userId, Optional<UUID> teamId) {
    return repository.findActiveForUser(userId, teamId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<BroadcastMessageResponse> getPublicMessages(String context) {
    boolean login = "login".equalsIgnoreCase(context);
    boolean register = "register".equalsIgnoreCase(context);

    return repository.findActivePublic(login, register).stream().map(this::toResponse).toList();
  }

  @Transactional
  public void dismiss(UUID userId, String identifier) {
    BroadcastMessage message = repository.getByIdentifier(identifier);
    dismissalRepository.save(message.getId(), userId);
  }

  private BroadcastMessageResponse toResponse(BroadcastMessage msg) {
    return new BroadcastMessageResponse(
        msg.getIdentifier(),
        msg.getTitle(),
        msg.getBody(),
        msg.getSeverity(),
        msg.getStartAt(),
        msg.getEndAt());
  }
}
