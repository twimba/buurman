package com.buurman.dto.response;

/**
 * Who a communication actually went to.
 *
 * <p>One send fans out: {@code sendToTeam} emits a notification per admin/editor per channel
 * alongside the one addressed to the tenant, and an entity's timeline returns all of them. Without
 * this the client cannot tell "your tenant was told" from "you were told", which is the only
 * question the timeline exists to answer — and it would offer Resend on an internal copy, sending a
 * colleague a duplicate while the landlord believes they are chasing the tenant.
 */
public enum CommunicationAudience {

  /** Addressed to a contact: the tenant or another counterparty on the contract. */
  CONTACT,

  /** An internal copy addressed to someone on the team. */
  TEAM,

  /**
   * Neither — a system record with no recipient. Deliberately not folded into {@link #TEAM}: this
   * must never be presented as tenant contact, and the distinction between "we told a colleague"
   * and "we told nobody" is worth keeping.
   */
  UNKNOWN
}
