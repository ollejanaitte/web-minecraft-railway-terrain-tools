# Phase 1-R10F Contract Change Policy

Date: 2026-08-13 JST
Status: effective for all phases after R10F.

Any change to a **FROZEN** Foundation Contract item (see
`phase1_r10f_foundation_classification.md` section A) is a **Contract Change**,
NOT a refactor. It requires a Contract Change Proposal (CCP) approved with an
explicit GO before implementation.

## When a CCP is required

- The change alters anchor / support-surface datum semantics (A1, A6);
- direction semantics (A2);
- preview/confirm identity or introduces renderer fake geometry (A3);
- asset/path isolation (A4);
- cancel/clear/delete semantics (A5);
- editing anchor invariance or cant/centerline rules (A7);
- inventory/command authority (A8);
- command-fallback identity (A9).

Adding an EXTENSIBLE item (section B) or swapping a REPLACEABLE implementation
(section C) does NOT require a CCP, but must keep the frozen invariants proven.

## Required CCP fields

1. Contract ID (e.g. A2) and title.
2. Current behaviour (quote the frozen contract).
3. Proposed behaviour.
4. Reason (RTM reference alone is insufficient).
5. Evidence (docs, source, tests, runtime).
6. User-visible impact.
7. Geometry impact.
8. Renderer impact.
9. Editing impact.
10. Asset impact.
11. Persistence/network impact.
12. Backwards compatibility.
13. Affected tests (Foundation Contract Suite + existing harness).
14. Affected Golden Data (fixtures that change and why).
15. Migration need (persisted world data).
16. GO / NOGO decision and approver.

## Rules

- Golden values are NOT updated as a "test fix". A golden change is a contract
  change: first approve the CCP, then regenerate and re-verify against the
  contract and evidence.
- The Foundation Contract Suite gate: any FAILED test = NOGO for R11 entry and
  for any phase that touches the Foundation.
- After a CCP-GO change, ALL of the following must pass: Foundation Contract
  Suite, existing harness regression, Golden Data verification, Production
  Build, and (where relevant) Normal World Acceptance.
