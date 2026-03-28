package com.buurman.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.SegmentAttribute;
import com.buurman.domain.SegmentCondition;
import com.buurman.domain.SegmentDefinition;
import com.buurman.domain.SegmentOperator;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.FeatureFlagOverrideRepository;
import com.buurman.repository.SegmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SegmentAdminService {

  private final SegmentRepository segmentRepository;
  private final FeatureFlagOverrideRepository overrideRepository;
  private final SegmentEvaluator segmentEvaluator;

  public List<SegmentDefinition> listSegments() {
    return segmentRepository.findAll();
  }

  public SegmentDefinition getSegment(String key) {
    return segmentRepository
        .findByKey(key)
        .orElseThrow(() -> new NotFoundException("Segment '%s' not found".formatted(key)));
  }

  public SegmentDefinition getSegmentById(UUID id) {
    return segmentRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Segment not found"));
  }

  @Transactional
  public SegmentDefinition createSegment(
      String key,
      String name,
      String description,
      int priority,
      List<ConditionInput> conditions,
      UUID actorId) {

    segmentRepository
        .findByKey(key)
        .ifPresent(
            existing -> {
              throw new BusinessRuleException(
                  "Segment with key '%s' already exists".formatted(key));
            });

    SegmentDefinition segment =
        SegmentDefinition.builder()
            .key(key)
            .name(name)
            .description(java.util.Optional.ofNullable(description))
            .priority(priority)
            .build();

    SegmentDefinition saved = segmentRepository.save(segment, actorId);

    List<SegmentCondition> domainConditions =
        conditions.stream()
            .map(
                c ->
                    SegmentCondition.builder()
                        .segmentId(saved.getId())
                        .attribute(SegmentAttribute.fromDbValue(c.attribute()))
                        .operator(SegmentOperator.fromDbValue(c.operator()))
                        .value(c.value())
                        .build())
            .toList();

    segmentRepository.replaceConditions(saved.getId(), domainConditions, actorId);
    saved.setConditions(domainConditions);

    segmentEvaluator.invalidateCache();
    return saved;
  }

  @Transactional
  public SegmentDefinition updateSegment(
      String key,
      String name,
      String description,
      int priority,
      List<ConditionInput> conditions,
      UUID actorId) {

    SegmentDefinition segment = getSegment(key);
    segment.setName(name);
    segment.setDescription(java.util.Optional.ofNullable(description));
    segment.setPriority(priority);

    SegmentDefinition saved = segmentRepository.save(segment, actorId);

    List<SegmentCondition> domainConditions =
        conditions.stream()
            .map(
                c ->
                    SegmentCondition.builder()
                        .segmentId(saved.getId())
                        .attribute(SegmentAttribute.fromDbValue(c.attribute()))
                        .operator(SegmentOperator.fromDbValue(c.operator()))
                        .value(c.value())
                        .build())
            .toList();

    segmentRepository.replaceConditions(saved.getId(), domainConditions, actorId);
    saved.setConditions(domainConditions);

    segmentEvaluator.invalidateCache();
    return saved;
  }

  @Transactional
  public void deleteSegment(String key, UUID actorId) {
    SegmentDefinition segment = getSegment(key);
    overrideRepository.softDeleteBySegmentKey(segment.getKey(), actorId);
    segmentRepository.softDelete(segment.getId(), actorId);
    segmentEvaluator.invalidateCache();
  }

  public long countMatchingTeams(String key) {
    SegmentDefinition segment = getSegment(key);
    return segmentRepository.countMatchingTeams(segment);
  }

  public long countMatchingUsers(String key) {
    SegmentDefinition segment = getSegment(key);
    return segmentRepository.countMatchingUsers(segment);
  }

  public List<SegmentRepository.MatchingTeam> findMatchingTeams(String key) {
    SegmentDefinition segment = getSegment(key);
    return segmentRepository.findMatchingTeams(segment);
  }

  public List<SegmentRepository.MatchingUser> findMatchingUsers(String key) {
    SegmentDefinition segment = getSegment(key);
    return segmentRepository.findMatchingUsers(segment);
  }

  public record ConditionInput(String attribute, String operator, String value) {}
}
