package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTACT_TAGS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoContactTagGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(55);

  private static final ContactTag[] ALL_TAGS = ContactTag.values();

  // Tags more likely for INDIVIDUAL contacts
  private static final ContactTag[] INDIVIDUAL_WEIGHTED_TAGS = {
    ContactTag.VIP,
    ContactTag.PROSPECT,
    ContactTag.LATE_PAYER,
    ContactTag.LONG_TERM,
    ContactTag.KEY_HOLDER,
    ContactTag.DO_NOT_CONTACT,
    ContactTag.FORMER_TENANT,
    ContactTag.REFERRED,
  };

  // Tags more likely for COMPANY / SERVICE_PROVIDER contacts
  private static final ContactTag[] BUSINESS_WEIGHTED_TAGS = {
    ContactTag.VIP,
    ContactTag.PROSPECT,
    ContactTag.KEY_HOLDER,
    ContactTag.REFERRED,
  };

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    int totalTags = 0;

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);

      List<UUID> contactIds = ctx.getContactIdsByTeam().getOrDefault(teamId, List.of());

      for (UUID contactId : contactIds) {
        ContactType contactType = ctx.getContactType(contactId);

        // ~55% of contacts get at least one tag
        if (random.nextInt(100) >= 55) {
          continue;
        }

        ContactTag[] weightedPool =
            (contactType == ContactType.INDIVIDUAL)
                ? INDIVIDUAL_WEIGHTED_TAGS
                : BUSINESS_WEIGHTED_TAGS;

        // 1 tag for most, rarely 2
        int tagCount = random.nextInt(10) < 2 ? 2 : 1;

        List<ContactTag> tagPool = new ArrayList<>(List.of(weightedPool));
        Collections.shuffle(tagPool, random);

        int added = 0;
        for (ContactTag tag : tagPool) {
          if (added >= tagCount) {
            break;
          }
          insertTag(contactId, teamId, tag, now, createdBy);
          added++;
          totalTags++;
        }
      }

      log.info("Created contact tags for team {}", teamKey);
    }

    log.info("Created {} contact tags total", totalTags);
  }

  private void insertTag(
      UUID contactId, UUID teamId, ContactTag tag, LocalDateTime now, @Nullable UUID createdBy) {
    dsl.insertInto(CONTACT_TAGS)
        .set(CONTACT_TAGS.ID, UUID.randomUUID())
        .set(CONTACT_TAGS.TEAM_ID, teamId)
        .set(CONTACT_TAGS.CONTACT_ID, contactId)
        .set(CONTACT_TAGS.TAG, tag.name())
        .set(CONTACT_TAGS.CREATED_AT, now.minusDays(random.nextInt(0, 365)))
        .set(CONTACT_TAGS.CREATED_BY, createdBy)
        .execute();
  }
}
