# Planner form refresh

Brief: improve the two existing native planner forms without changing their offline rules or save behaviour.
References: existing Planner History screen (dominant: Nur typography, charcoal/cream canvas, gold highlights); Outside Reading's fixed bottom save action (secondary: always reachable confirmation); Refero bundled icons and craft-details guides (labelled 24dp outline icons, 48dp minimum interaction targets, grouped choices and visible selected states). Live Refero tools are unavailable.

Lock: 20dp screen gutters, 16dp surface corners, restrained thin borders, gold only for selection and emphasis; existing app theme tokens throughout. No bitmap assets needed.

Decision ledger:
- Give each form a compact icon-led introduction, using the user's request and existing History hierarchy.
- Group inputs into numbered sections so page selection, date, duration and review remain easy to locate; preserve current order and validation.
- Use full-width selectable tiles, with both radio/check indicators and borders so selection never relies on colour alone.
- Show reminder permissions as a readiness section and keep clear labels on all actions.
- Keep reminder save reachable in a fixed footer, matching Outside Reading; allow large text and narrow screens to scroll naturally.

Validation: 562 unit tests, zero failures (one existing skip). Five existing form UI checks passed at standard size, then all five passed at 320dp with font scale 1.3. Inspected light Outside Reading and dark Reminder screenshots. Removed duplicated summary content and made the fixed gold save action use dark foreground for contrast in both themes. No progress or scheduling logic changed.
