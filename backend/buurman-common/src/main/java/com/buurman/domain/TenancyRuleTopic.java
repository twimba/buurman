package com.buurman.domain;

/**
 * Topic of a display-only tenancy-law reference entry on a catalogue country.
 *
 * <p>Adding a constant here means updating FIVE places in lockstep. Two of them fail silently — no
 * failed build, no failed test, nothing — so an 8th constant added only to this enum would compile,
 * pass every backend test, bundle cleanly and generate valid TypeScript, and then quietly drop rows
 * from the UI in production:
 *
 * <ol>
 *   <li>{@code openapi/src/app.yaml}'s {@code TenancyRuleTopic} schema (the source of the generated
 *       TS union type).
 *   <li>{@code openapi/app.yaml}, the bundled output — regenerate it with {@code make
 *       bundle-openapi} after step 1, or the frontend keeps generating TS against the stale schema.
 *   <li>The {@code rentRegulations.tenancyRules.topic} key in all 13 locale files at {@code
 *       frontend/app/public/locales/*}/contracts.json — <b>fails silently</b>: a missing key
 *       renders the raw key path (e.g. {@code rentRegulations.tenancyRules.topic.NEW_TOPIC}) to the
 *       user instead of failing any build or test.
 *   <li>{@code TOPIC_ORDER} in {@code
 *       frontend/app/src/components/rentRegulations/RegulationSummary.tsx} — <b>fails silently</b>:
 *       a topic absent from this list is dropped from the UI with no error, no warning, no failed
 *       test.
 *   <li>The {@code tenancyRules} section of {@code
 *       .claude/skills/update-rent-regulations/SKILL.md}, which documents the closed list of valid
 *       topics for whoever curates {@code rent-regulations.json} — stale guidance there means the
 *       next dataset update never uses the new topic at all.
 * </ol>
 */
public enum TenancyRuleTopic {
  NOTICE_PERIOD,
  TENANCY_DURATION,
  DEPOSIT,
  LEASE_FORM,
  REGISTRATION,
  FEES_AND_PENALTIES,
  OTHER
}
