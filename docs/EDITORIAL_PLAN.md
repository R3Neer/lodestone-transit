# Discovery-first documentation plan

## Plan before editing — 2026-09-09

- Audit the public branch and existing documentation; use an isolated checkout so
  concurrent local implementation is not accidentally committed or documented as released.
- Keep a short README: purpose, one first step, truthful experiments, installation,
  essential caveats and an explicit link to the complete guide.
- Preserve existing detailed information in docs/GUIDE.md, with contents/navigation
  and repaired relative links. Do not silently remove technical guidance.
- Retain failed-attempt fuel/damage warning, physical anchor loss and essential charging/readiness controls. Move full recipes, capacities, algorithms, lighting and integration details to a spoiler-rich guide. Reuse one existing overview image.
- Review against these criteria, correct omissions or justify changes, then review
  again. Check relative links and Markdown diff. Documentation-only commit/push;
  no version bump, gameplay mutation, release or binary installation.

## Review

### First review and adjustment

TeleportResolver, EquipReadiness and TeleporterItem confirm physical destination validation, charging and readiness. Kept failed-attempt cost/damage and broken-anchor warning in README rather than hiding them as surprises. Retained provenance, one existing real screenshot and all original detailed recipes/compatibility in the guide.

### Repeat review

- The short README offers a concrete first step and optional experiments without
  exposing every rule. Essential requirements and operational caveats remain.
- GUIDE.md preserves the original detailed reference, with contents and a return
  link. All local links and heading anchors in both pages resolve.
- Reviewed attribution, claims and loss of information against the public source.
  No gameplay files, versions, dependencies or local concurrent edits changed.
- Documentation-only validation: link/anchor checks and git diff whitespace checks;
  no new runtime/gameplay validation is claimed by this editorial commit.
