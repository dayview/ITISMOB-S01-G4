---
name: purrfolio-design
description: Apply PURRFOLIO's distinctive light-and-dark pet-care visual identity to Android UI design and implementation. Use whenever planning, styling, reviewing, or refining PURRFOLIO screens, components, themes, typography, colors, icons, or UI polish; preserve existing architecture and functionality.
---

# PURRFOLIO visual identity skill

## Objective
Build a polished, native Android pet-care interface combining the editorial restraint, dense-but-readable information design, confident typographic hierarchy, and careful alignment of the supplied Zapurr references with PURRFOLIO's original affectionate lavender identity. Take inspiration from quality, never duplicate layouts, copy, logos, or distinctive artwork.

## Scope and workflow
1. Inspect the project to identify Kotlin/Compose versus XML Views, current theme tokens, fonts, assets, and reusable components. Preserve the framework and architecture.
2. Read any supplied reference images and compare their visual traits against the current implementation. Extract principles, not pixel-for-pixel screen copies.
3. Create or update centralized semantic theme tokens, typography, elevations, shapes and component styles for both light and dark modes before changing individual UI elements.
4. Propose concrete changes, then implement only the specifically requested scope. Do not add screens, flows, features, backend behavior, or dependencies without authorization.
5. Build and validate what can be validated locally, and inspect screenshots/previews when available. State what was checked and any remaining gaps.

## Art direction
- Mood: premium, calm, modern, editorial, compassionate, subtle kawaii.
- Zapurr-inspired strengths: bold rounded heading hierarchy, generous negative space, refined dark surfaces, small muted metadata labels, deliberate color emphasis, crisp pet photography, compact legible cards, restrained accent use.
- PURRFOLIO signatures: lavender primary accents, softly pink secondary accents, warm pet photography, rounded controls, occasional paw/heart/sparkle motifs. Cute without looking childish.
- Do not import Zapurr's teal-centric branding, brand name, copy, proprietary illustrations, or specific screen arrangements.
- Avoid generic Android blue, random gradients, excessive decorations, loud shadows, hard borders, and emoji as mascot replacements.

## Semantic colors
Treat hex values as initial direction subject to accessibility/visual review, not exact screenshot sampling. Prefer role tokens (background, surface, onSurface, primary, onPrimary, outline, error) over direct color literals.

| Role | Light | Dark |
| --- | --- | --- |
| background | #FCF9FF | #101014 |
| surface | #FFFFFF | #1C1D26 |
| surfaceVariant | #F5EEFA | #242530 |
| primary | #8756B0 | #D8B4F8 |
| primarySoft | #E5C9FA | #E6C8FF |
| accentPink | #F3B6D7 | #F3B6D7 |
| accentCream | #FFF1DF | #F8DFCA |
| onBackground | #27212F | #F8F2FF |
| onSurfaceVariant | #71667B | #BDB3CD |
| outline | #E9DEEE | #343442 |
| error | #B32646 | #F29CA9 |

Use appropriate on-primary colors for readable buttons, verifying contrast. State colors (success/warning/critical) must be distinguishable from purely decorative accents and must not rely only on color.

## Typography
- Display / hero: confident, rounded sans (Fredoka or a legitimately licensed suitable equivalent), weight 600–700, sizes around 28–36sp; apply sparingly.
- Functional text: highly legible rounded sans (Nunito or licensed equivalent), 14–16sp body; headings 18–24sp; labels 11–13sp.
- Use compact uppercase micro-labels with careful tracking only for metadata/eyebrows, not body copy.
- Balance expressive headers with clear numeric data, readable sentence case, and sensible line length.
- Do not invent or download font files without user approval; use available fonts and platform fallbacks.

## Geometry and spacing
- 4dp base units; main rhythm 8, 12, 16, 24, 32dp.
- Cards generally 20–24dp corners, fields 16–20dp, buttons pill or 24–30dp corners, chips 14–18dp.
- Prefer contained, deliberate components over giant padded cards. Make compact, readable data groups.
- Keep pet images real and focal when a photo is appropriate; crop carefully.
- Minimum interactive touch targets 48dp, scalable text, correct system bars/insets, keyboard-aware layout.

## Light/dark parity
- Light = warm cream/lavender-paper atmosphere, subtle borders and very soft elevation.
- Dark = sophisticated charcoal/ink with high contrast and restrained pastel luminous accents, never a flat black void everywhere.
- Both modes have the same visual hierarchy, component semantics, and brand identity.
- Follow system theme by default; allow an explicit user override only if the app already supports it or it is requested.

## Components
- Buttons: high-contrast primary fill, rounded form, controlled pressed/loading/disabled states.
- Cards: hierarchical text, lightweight dividers or edges, minimal shadow.
- Inputs: consistent outlines, icon treatment, error/focus states and helper messages.
- Chips and statuses: understated tinted fills; emergencies can depart from brand lavender for clear semantic urgency.
- Icons: one consistent rounded-stroke family; meaningful icons never replaced by arbitrary doodles.
- Imagery: warm, authentic pet portraits + occasional consistent soft illustrations; decoration should never obscure content.

## Visual QA checklist
Check against the supplied references for: density, alignment, baseline rhythm, title/body hierarchy, text wrapping, photo crop, component size, states, and overall polish. Audit both modes, small-screen behavior, large font scale, contrast, screen reader labels, and accessible touch sizes. Validate using available Android build/previews/tests, and never claim screenshot validation if screenshots were not actually inspected.

## Guardrails
Preserve existing project package names, navigation, state, data models, API calls, business logic, and working features. Do not assume Compose if the project is XML-based. Do not design individual screens unless the request explicitly asks for screens; a branding request should produce tokens, reusable components, and visual guidelines only.
