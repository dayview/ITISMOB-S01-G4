# Sitter links

The Android app writes a seven-day snapshot to `sitterLinks/{token}`. The token is 32 cryptographically random bytes encoded as a 43-character URL-safe string. The hosted page reads that one document through Firestore REST. It does not query the collection or render shared text as HTML.

The snapshot contains the pet's name, species, breed, care notes, medical information, vet clinic and phone, and emergency contact name and phone. It excludes the local photo, documents, account profile, and care logs. Editing a pet does not change an existing link. Turn off the old link and create a new one to share updated details. Turning off a link sets `active=false`; an expired or inactive link is denied by Firestore rules.

The app requires Firebase sign-in and an online Firestore response to create, load, or turn off a link. The browser reader does not need an account. Anyone with an active link can read its snapshot, so the create dialog lists the shared fields before publishing.

`firestore.rules` starts from the active ruleset read from project `purrfolio-b1e6a` on 2026-11-21, then adds only the `sitterLinks` match. The rules and Hosting page were deployed to the default Firebase site on 2026-11-21. Review the diff against the live rules before each future deployment. The standalone rule addition is in `docs/sitter-link-firestore.rules`.

Future deployment:

```sh
npx firebase-tools deploy --only firestore:rules,hosting --project purrfolio-b1e6a
```

The live Hosting route and owner query were verified after deployment. A real link was not created during verification, so the owner should create one in Sitter Mode, open it in a private browser window, turn it off in the app, and confirm the browser link no longer opens.
