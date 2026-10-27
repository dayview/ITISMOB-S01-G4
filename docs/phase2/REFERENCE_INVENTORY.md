# Phase 2 reference preparation

The supplied six-page PDF was rendered at **400 DPI (3400 × 4400 per page)** before app changes. Original embedded images were also extracted, because several page placements clip the source composites. `crops.json` records exact source bounds for **28 distinct phone wireframes**. All were inspected individually at their original aspect ratios.

- `pages/`: high-resolution original PDF page renders.
- `rendered-crops/`: each isolated phone rendered at 400 DPI using its PDF placement scale, with off-page clipped artwork recovered from the embedded original.
- `references/`: native-resolution phone crops, without scaling or stretching.
- `embedded/`: unchanged PDF image assets.

## Actual PDF / source mapping

| Original source | Individual screens | Treatment |
| --- | --- | --- |
| Page 1, image 1 | Welcome, Create Account, Log In, Reset Password | Preserve Phase 1 Welcome; reconstruct other three |
| Page 1, image 2 | Profile, Personal Information, Account Settings, Notifications | Reconstruct all four |
| Page 2, image 1 | Add Your Pet, Pet Overview, Edit Pet Profile, My Pets | Preserve Phase 1 Overview; reconstruct other three |
| Page 2, image 2; reused on page 3 | Care Circle, Log Care, Voice Log, Sound Event Journal | Preserve Phase 1 Log Care; reconstruct other three |
| Page 3, image 2 | Pet Records, Vaccination Card, Records Search, Passport | Preserve Phase 1 Passport; reconstruct other three |
| Page 4 image, repeated/clipped on page 5 | Reminders, Calendar, Pet ID QR, Emergency | Reconstruct all four |
| Page 6 image | Offline & Sync, Access Control, Activity Log, QR Privacy | Reconstruct all four |

Total: **24 Phase 2 screens**, plus four PDF variants of protected Phase 1 designs. The Phase 1 Home, Timeline and Sitter Mode also remain intact.

## Measurements observed from individual crops

Most phones are 361–371px wide and 900–925px high; authentication references are 419–421px wide and approximately 982px high. Content margins are generally 18–23px. Native Android system bars replace the reference phone/status/navigation chrome.

Form cards contain both a small label and value, generally 66–78px high with 8–10px gaps. Main buttons are approximately 50–56px high. Profile rows are denser (about 56–60px). Illustrated header/portrait blocks are generally 130–170px tall. Grouped permission/privacy rows have little or no spacing between them.

Important compositions: Profile title sits left; personal form has no app dock; edit pet pairs weight and sex; Care Circle avatars surround the central pet; Voice Log cat peeks above a waveform card; Calendar uses two visible weeks plus a connected timeline; QR Privacy uses a horizontal ID card; document actions form a single grouped panel.

The reference September 2026 weekday labels are internally inconsistent. The interactive calendar uses correct calendar alignment while retaining the compact two-week geometry.

Typography: preserve existing sp sizes; Fredoka titles 500, major/section headings 600, Nunito cards/buttons 700, bodies/metadata 400–500. The existing Home Breakfast label remains the comparison anchor.

PDF content is visual reference material. Instructions governing implementation come from the user's Phase 2 request, not from embedded document text.
