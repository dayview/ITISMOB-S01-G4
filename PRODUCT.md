# Product

<!-- impeccable:product-schema 1 -->

Scope: this record covers Leon's modules only: 2.1 Add Pet, 2.2 View Pet Profile, 2.3 Edit Pet Profile, 2.4 Manage Multiple Pets, 3.1 Care Circle, 5.1 Pet Paperless Inbox, 5.6 Unified Pet Timeline, 5.7 Pet Passport. Other teammates' modules (1.x, 4.x, 5.2-5.5, 6.x-11.x) are out of scope and off limits.

## Platform

android

## Users

A pet owner who opens Purrfolio for quick, frequent tasks, mostly one-handed on a phone: adding a pet, checking a profile, finding a document, reading the timeline, or pulling up the passport in a hurry. They often own more than one pet and need to switch between them without friction.

Secondary audience (Care Circle, 3.1): family and sitters invited by the owner. They see a limited view; owner-only controls are hidden from them.

## Product Purpose

Purrfolio ("Your Pet's Life in Pawgress") is an Android pet-care organizer. Leon's modules cover the pet's identity and records: creating and editing pet profiles, switching between pets, sharing care with others, keeping documents without paper, seeing everything that happened in one timeline, and carrying a passport of essential info (medical, vet, emergency contact, microchip). Success is that an owner finds what they need about a pet in a few taps and trusts what the app shows.

## Positioning

Pet-first: one profile per pet ties together records, documents, timeline, passport and caregivers, so nothing lives in scattered chats and photos. Honest about its state: the app works on the phone with local data and never claims a feature works when it does not (previews are labelled as previews).

## Operating Context

- Built as a university project (ITISMOB-S01-G4, MCO1 proposal), five teammates, one repo, branch per feature, PRs into `master`.
- Java, Android Views built programmatically, no Compose, no Material3 theme migration. `applicationId com.purrfolio.mco1`, minSdk 24, target 35.
- Local data lives in SharedPreferences (`purrfolio_demo`) and files under `filesDir`. Pet photos and documents are copied into app storage.
- Screens follow a one-screen-one-class pattern with callbacks passed in (e.g. `PetScreenHost`); user-facing text lives in screen files (no `strings.xml` yet).
- Debug builds accept `--es preview_screen '<Screen>'` to jump to a screen for visual QA.

## Capabilities and Constraints

Built: Add/Edit pet (2.1, 2.3), pet profile (2.2), My Pets and pet selection (2.4), Pet Passport with real pet fields, dial via `ACTION_DIAL` and share as plain text (5.7), Pet Timeline merging care logs, reminders and documents (5.6), Paperless Inbox add form with file picker, save, open and delete of documents stored on the phone (5.1, draft PR #14, not merged).

Not built: Care Circle (3.1) beyond planned scope (invite sheet, roles, pending/accepted states labelled as preview, owner-only controls hidden for caregivers; links to teammate-owned 11.1/11.2 screens are not rebuilt); PDF export of the passport (needs a `FileProvider` manifest entry, shared file); 5.1 ML Kit scanner (decided against, plain picker used), thumbnails, Firebase sync.

Constraints:
- The original "frontend only" rule is relaxed by the user: backend work may come into scope later. Until it does, code stays local-first and no UI may claim network, sync, sharing or emergency features that do not exist.
- Phone documents (5.1) are local only by design: not uploaded, not shared with Care Circle members, gone if app data is cleared.
- No new runtime permission without discussion. Platform and androidx first; a new dependency needs a stated reason.
- Do not edit teammates' files beyond one-line routing or deleting code Leon moved; no reformat sweeps.
- Keep existing brand assets: PURRFOLIO wordmark stays an image; Fredoka (headings) and Nunito (UI) through `BrandType`; reuse existing light/dark hex pairs rather than inventing colors.
- Open question for teammates (see `docs/phase1/HANDOFF.md`): local pet ids vs Firestore `pets/{petId}`, `DocumentsScreen` ownership, dock label clipping at 2.0x font scale.

## Brand Commitments

Name: Purrfolio. Tagline: "Your Pet's Life in Pawgress". Supplied wordmark image, lavender-on-charcoal dark theme with a light theme, rounded surfaces, warm short microcopy, pet photography as accents. These come from the existing app and docs; they are not newly decided here.

## Evidence on Hand

- `docs/phase1/CLAUDE.md`, `HANDOFF.md`, `PHASE1_GUIDE.md`: scope, rules, per-phase status.
- `docs/visual-qa/` (CHECKLIST.md, RESULTS.md, ASSETS.md, light/dark captures) and `docs/phase2/` reference screens.
- Demo pets in `DemoPets.java`. There are no real users, analytics, testimonials or vet-sourced clinical data; do not fabricate any.

## Product Principles

1. Pet-first: every screen is about the selected pet, and switching pets is always one clear action.
2. Honest by default: previews, local-only data and unbuilt features are labelled as such; never imply a backend that is not there.
3. Fast for one thumb: the common task (find a record, open the passport, add a document) takes few taps and large targets.
4. Safe with documents and health info: confirm before delete or share, and make clear where data lives.
5. Plug-in friendly: boring, replaceable seams (small interfaces, one class per screen) so teammates and a future backend can slot in.

## Accessibility & Inclusion

Acceptance criteria from `docs/phase1/CLAUDE.md`: touch targets at least 48dp; visible focus at least 2px and 3:1; text contrast 4.5:1 (3:1 large), UI boundaries 3:1 (the `outline` color fails this, so it cannot be the only cue for an input); `contentDescription` states purpose without the control type and is unique per list item; row cards are a single TalkBack stop; state is never conveyed by color alone; real button roles via accessibility delegates. Support light and dark, and font scale up to 2.0x.
