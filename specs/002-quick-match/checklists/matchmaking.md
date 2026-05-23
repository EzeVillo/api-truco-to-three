# Matchmaking Requirements Quality Checklist: Quick Match

**Purpose**: Pre-plan requirements quality check focused on concurrency/race conditions and player
availability integration (FR-010/FR-011 bypass). Standard depth.
**Created**: 2026-05-19
**Feature**: [spec.md](../spec.md)

## Concurrency & Race Conditions ⚠️ Mandatory Gating

- [ ] CHK001 - Are requirements defined for the outcome when a cancel request (FR-002) and the
  pairing event (FR-003) occur simultaneously — specifically, which takes precedence and what each
  player receives? [Clarity, Spec §Edge Cases]
- [ ] CHK002 - Is the expected outcome for the "3 simultaneous players" edge case fully specified:
  which two are paired, and what notification (if any) does the third player receive? [Completeness,
  Spec §Edge Cases]
- [ ] CHK003 - Is the FIFO ordering guarantee in FR-003 defined precisely enough to be unambiguous
  under concurrent queue arrivals (e.g., are ties broken by arrival timestamp, by request order, or
  undefined)? [Clarity, Spec §FR-003]
- [ ] CHK004 - Are requirements defined for what happens if match creation fails after two players
  are already paired (e.g., a system error during match initialisation) — are both players returned
  to the queue, notified, or left in an undefined state? [Coverage, Gap]
- [ ] CHK005 - Is the "race condition on cancel" edge case outcome ("cancel is ignored, match
  proceeds") consistent with FR-002, which guarantees that cancel removes the player from the queue?
  Is this apparent conflict explicitly resolved in the spec? [Consistency, Spec §FR-002, §Edge
  Cases]

## Player Availability Integration — FR-010/FR-011 Bypass ⚠️ Mandatory Gating

- [ ] CHK006 - Is the scope of "all match actions" blocked by FR-010 explicitly enumerated (e.g.,
  create private match, join by code, join public match, create bot match, enter tournament) rather
  than left as an open "etc."? [Clarity, Spec §FR-010]
- [ ] CHK007 - Is the distinction between "player-initiated" and "system-initiated" match creation
  (FR-011) defined with enough precision that an implementer can determine unambiguously whether a
  given code path must bypass the restriction? [Clarity, Spec §FR-011]
- [ ] CHK008 - Is there a measurable success criterion (analogous to SC-001–SC-005) covering FR-010
  — specifically, that players in the queue are rejected from other match actions 100% of the time?
  [Measurability, Gap]
- [ ] CHK009 - Are requirements specified for the sequence: player is in queue → system creates
  match (FR-011 bypass) → player attempts to cancel → what is the expected outcome? [Coverage, Gap]
- [ ] CHK010 - Is it explicitly specified whether FR-010 also blocks the player from attempting to
  enter the queue again with a different `gamesToPlay` value while already queued (vs. FR-008, which
  only addresses the same `gamesToPlay`)? [Completeness, Spec §FR-008, §FR-010]

## Requirement Completeness

- [ ] CHK011 - Are the contents of the join-queue confirmation response (FR-001) defined — does it
  include queue position, ticket ID, timestamp, or just a success acknowledgement? [Clarity, Spec
  §FR-001]
- [ ] CHK012 - Are the contents of the pairing notification (FR-005) fully specified beyond "match
  identifier" — e.g., opponent name, `gamesToPlay`, match start URL? [Completeness, Spec §FR-005]
- [ ] CHK013 - Are the contents of the cancel confirmation response (FR-002) specified — does the
  player receive a success body, an empty 204, or a status message? [Clarity, Spec §FR-002]
- [ ] CHK014 - Are authentication requirements for entering the Quick Match queue stated explicitly,
  or is it only implied by "authenticated player"? [Completeness, Gap]
- [ ] CHK015 - Are requirements defined for what happens to the match if one of the two newly-paired
  players disconnects immediately after pairing but before the match is fully
  started? [Coverage, Gap]

## Requirement Clarity

- [ ] CHK016 - Is "immediately" in FR-004 ("match starts immediately") quantified or aligned with
  the 2-second bound in SC-001, or is the relationship between these two requirements left
  ambiguous? [Clarity, Spec §FR-004, §SC-001]
- [ ] CHK017 - Is "real-time connection is closed" in FR-007 defined precisely — does it cover
  WebSocket disconnect only, session token expiry, or any loss of
  connection? [Clarity, Spec §FR-007]
- [ ] CHK018 - Is the term "occupied" (FR-010) formally defined in relation to existing player
  availability concepts (e.g., `hasUnfinishedMatch`, `hasOpenRematchSession`) to avoid ambiguity in
  implementation? [Clarity, Spec §FR-010]
- [ ] CHK019 - Is the "idempotent" behaviour for double-entry (FR-008) specified clearly — does the
  second request return an error code, a success with current queue status, or a silent no-op?
  [Clarity, Spec §FR-008]
- [ ] CHK020 - Are error/rejection responses for FR-006 and FR-010 described — is there a defined
  error format or message the player receives when rejected from the queue? [Completeness, Gap]

## Requirement Consistency

- [ ] CHK021 - Is SC-001 ("paired and notified within 2 seconds") consistent with FR-004 ("starts
  immediately") — do these refer to the same observable event, and are they aligned rather than
  duplicated with different implied thresholds? [Consistency, Spec §SC-001, §FR-004]
- [ ] CHK022 - Are the edge case outcomes consistent with the corresponding functional requirements?
  Specifically: "cancel is ignored when match is formed" (Edge Cases) vs. FR-002 ("cancel removes
  player from queue") — is the resolution of this apparent conflict explicit? [Consistency, Spec
  §FR-002, §Edge Cases]
- [ ] CHK023 - Is FR-009 ("matches not in public lobby") consistent with the assumption that "Quick
  Match reuses the existing match auto-start behaviour" — does the reused behaviour risk exposing
  the match in the public lobby? [Consistency, Spec §FR-009, §Assumptions]

## Acceptance Criteria Quality

- [ ] CHK024 - Can SC-003 ("0% of disconnected players remain as active entries") be objectively
  measured given the queue is in-memory and ephemeral — is a monitoring or inspection mechanism
  implied by this criterion? [Measurability, Spec §SC-003]
- [ ] CHK025 - Are the acceptance scenarios for User Story 3 (auto-cleanup on disconnect) sufficient
  to cover all connection-loss types (clean WebSocket close vs. abrupt TCP drop)? [Coverage, Spec
  §User Story 3]
- [ ] CHK026 - Does each acceptance scenario have an unambiguous "Then" clause that can be verified
  without implementation knowledge? [Measurability, Spec §User Scenarios]

## Edge Case & Scenario Coverage

- [ ] CHK027 - Are recovery flow requirements defined for when a player's connection drops while in
  the queue and they reconnect — is "re-enter the queue manually" a requirement or just an
  assumption?
  [Coverage, Spec §Assumptions]
- [ ] CHK028 - Are requirements defined for what a waiting player should do (or be told) when the
  server restarts and the queue is wiped — is silence/reconnect the defined behaviour? [Coverage,
  Spec §Assumptions, §Edge Cases]
- [ ] CHK029 - Are requirements defined for partial-state scenarios: e.g., the queue pairs two
  players but the notification delivery (FR-005) fails for one of them — is there a retry or
  fallback? [Coverage, Gap]

## Non-Functional Requirements

- [ ] CHK030 - Are concurrency/throughput requirements defined for the queue — is there an expected
  upper bound on simultaneous queue entries or pairing operations the system must
  handle? [Completeness,
  Gap]
- [ ] CHK031 - Are requirements defined for queue observability — how would an operator detect a
  stuck queue or a ghost entry that survived a disconnect? [Gap]

## Dependencies & Assumptions

- [ ] CHK032 - Is the assumption that "Quick Match reuses existing match auto-start behaviour"
  validated — is it confirmed that the existing behaviour produces a match in the `IN_PROGRESS`
  state without requiring an extra ready-up step? [Assumption, Spec §Assumptions]
- [ ] CHK033 - Is the dependency on WebSocket infrastructure for pairing notification (FR-005) and
  disconnect detection (FR-007) explicitly documented as a dependency rather than an implicit
  assumption? [Dependency, Gap]
- [ ] CHK034 - Is the "server restart = queue loss" risk explicitly accepted with a defined
  mitigation plan or rationale (e.g., "acceptable for v1, revisit in v2")? [Assumption, Spec
  §Assumptions]

## Notes

- Check items off as completed: `[x]`
- Items marked ⚠️ **Mandatory Gating** (CHK001–CHK010) should be resolved before `/speckit-plan`
- Items marked `[Gap]` indicate missing requirements — update spec.md before planning
- Items marked `[Consistency]` may require edge case wording to be tightened in spec.md
- Link to spec sections when adding findings inline
