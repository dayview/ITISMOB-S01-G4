# Visual verification — 2026-10-28

## Build and functional checks

- `./gradlew :app:assembleDebug :app:lintDebug` with JDK 21: **passed**.
- Final lint: **0 errors, 24 warnings**. Remaining warnings concern static Canvas allocations, programmatic custom-view constructors, existing unused resources/RTL alignment, and available dependency updates.
- Install on Pixel 6 emulator: passed.
- Actual visible-control smoke checks: Home center button → Log Care → Water opens the existing form with Water selected; pet options → Passport → Share Passport opens the existing integration placeholder. No test care records or public links were created.
- Backend, package name, persistence schema, local profile editing and care-entry saving remain in the existing Activity.

## Screenshot review

All seven dark and all seven light screens were captured on the same emulator: 1080 × 2400, density 420. Real Android clock, cutout/status area, and gesture region are retained. Contact sheets are in `dark-contact-sheet.png` and `light-contact-sheet.png`; full-size captures are in `dark/` and `light/`.

The supplied reference composites were used for visual review in the conversation. They were not available as local image files, so no numeric pixel-difference score or direct automated color sampling is claimed.

| Screen | Inspected and refined |
| --- | --- |
| Onboarding | Wordmark image proportions, transparent cat composition, heart/sparkles, subtitle, action pills, social marks and dots. Removed excess gap between cat and primary button. |
| Home | Two-line greeting, avatar ring, seven weekday rings, selected capsule contrast, care summary, four icon rows and dock. |
| Overview | Actual Canvas tracks/arcs with round caps; revised inner-ring spacing, lavender-to-blue arc, photo crop, face badges, four metrics and Insights. |
| Log Care | Top sheet geometry, handle/close, two-column circular grid, illustrated icons and expanded voice panel. Smaller-width circle sizing adapts to available width. |
| Timeline | Seven capsules, continuous colored rail and markers, four today activities and tomorrow vet card; navigation remains above system gestures. |
| Sitter | Mint header dot, reconstructed cat/dog art, descriptive copy, visible lavender switch, inset URL and four information rows. Sharing explicitly remains a preview. |
| Passport | Selected pet identity, seven separated information rows, edit/share controls and dock. Missing clinical/contact data are identified instead of inventing records. |

Light-mode review checked the same geometry and replaced low-contrast pastel metric text with deeper green/blue values.

## Fidelity limits

This is a reference-based reconstruction, **not a claim of pixel identity**. The original supplied local cat was an incomplete square crop and the wordmark had an opaque background. Transparent sibling assets were reconstructed using the built-in image tool; the pet face, fur, lettering edges and sitter illustration differ from the composite references. Original assets remain bundled unchanged. See `ASSETS.md` for paths and prompts.

Native Android aspect ratio and system insets produce different vertical breathing room than the reference phones. Icons are custom approximations, not extracted originals. The reference-only care schedule and wellness insights remain illustrative; unavailable clinical records and unsupported sitter/public-sharing operations are deliberately not presented as real data or successful integrations.

These limitations matter for strict visual acceptance: the original full-resolution photo/illustration/transparent logo artwork would be needed to eliminate asset differences. The captured UI is directly reviewable alongside the supplied references.

## Compact viewport / system navigation

Supplemental captures in `compact/` use 1080 × 2400 at density 480 (360dp wide). Home was inspected with both gesture and three-button system navigation; the dock stays above the system controls. The Log Care check exposed a compressed voice panel; the scroll child now measures its content height and the voice panel retains its full height, permitting vertical scrolling on short screens. Emulator density and navigation mode were restored after the checks.
