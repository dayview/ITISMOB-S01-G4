# Care Logging backend and API integration

Scope: Quick Log, Pet Voice Log, Conversational Care Capture, Sound Event Journal, per-pet Care History, and Edit/Delete Care Log. The app remains Java 17 with XML/View Binding. Firebase Authentication and Firestore SDK APIs connect to the existing project. Per the chosen no-Blaze setup, audio bytes stay on the recording device; only observation metadata goes to Firestore. No Cloud Storage dependency, bucket, or billing upgrade is required.

## Configuration and identity

The supplied `google-services.json` is installed at `app/google-services.json` and ignored by Git. Other developers must obtain this file for Android package `com.purrfolio.mco1` from Firebase project `purrfolio-b1e6a`. This file configures the app; administrator access requires a separate Firebase CLI login.

Care History's **Care account** control signs into an existing Firebase Email/Password account. Existing Firebase sessions are reused. Account creation, password recovery, social login, pet management, and other feature backends are outside this change. Email/Password is enabled in the live project. There is no anonymous production fallback.

## Data and APIs

Records live at `users/{firebaseUid}/careLogs/{entryId}`. The authenticated UID scopes access; `petId` scopes each history query. Existing pet IDs remain stable on the device. When integrating a separate pet-management backend, use the same pet IDs on all devices; this care change does not synchronize pet profiles.

| Field | Meaning |
| --- | --- |
| `id`, `petId`, `petName` | Stable entry ID and selected pet identity |
| `kind`, `text` | Validated activity and user note |
| `source` | `text`, `voice`, or `audio` |
| `amount`, `transcript` | Structured quantity and original confirmed voice text |
| `occurredAt` | Confirmed activity time as epoch milliseconds |
| `createdAt`, `updatedAt` | Firestore server timestamps |
| `revision` | Increments on update; transactions reject stale edits |
| `audioDeviceId`, `durationMs` | Recording installation identifier and measured duration; neither file bytes nor a local file path |
| `status` | `ready` or `deleting` |

`FirebaseCareRepository` implements create, realtime per-pet history, edit, and delete. History starts with 50 records and can load older entries. Transactions wait for server acknowledgement, so offline requests do not display a false cloud-save success. Cached history is labelled. Account changes clear displayed records and stop playback.

Android's speech-recognition service supplies the transcript. `CareCapture` extracts pet, activity, quantity (including word/fraction forms), and time. The confirmation dialog prompts for missing/ambiguous pet/activity/amount/date/time and permits corrections before saving. Ambiguous hours need AM/PM. `occurredAt` uses the confirmed time rather than the time Save was tapped. Typed transcript entry is available when speech recognition is unavailable.

## Local audio behavior

Recordings use AAC in M4A, capped at 60 seconds and 5 MiB. Files are stored in the app's private no-backup directory. Their account-specific local attachment index is excluded from Android backup and device transfer. File names/absolute paths are not written to Firestore.

After a recording stops, its draft is retained per account on this device. The user adds a description, then saves observation metadata to Firestore. Interrupted/offline saves can be retried from Care History. A saved recording plays directly from the original device. Other devices can read its observation metadata and see that audio is available only on the original device. Clearing app data, uninstalling the app, or losing the device loses these audio files; they have no cloud backup.

Deleting an observation removes Firestore metadata and the local recording on the device performing the deletion. On a later connection, the original device reconciles its saved attachment index and removes recordings whose server records were deleted elsewhere. Offline reconciliation never deletes files merely because a cached query omits them.

Existing device care entries stay intact until explicitly imported. Imports preserve IDs, remove each legacy entry only after server confirmation, and offer correction/skip for incomplete legacy records. Imported local recordings are copied into the no-backup directory before their observation metadata is saved.

## Backend rules and deployment

`firebase/firestore.rules` preserves the existing live owner-only `users/{uid}` root rule and adds care-specific ownership, schema, immutable identity, server timestamps, revisions, and deletion-state validation. A pre-change backup is in `firebase/deployment-backup/firestore-before-care.rules`. The rules reject local file paths, public audio URLs, unsupported activities, oversized notes, invalid timestamps, incomplete voice data, and invalid recording duration/device metadata.

`firebase/firestore.indexes.json` adds the selected-pet chronological query index. No existing index was present when inspected. This feature does not require Cloud Functions or Cloud Storage.

From `firebase/`, after Firebase CLI sign-in:

```sh
./node_modules/.bin/firebase deploy --project purrfolio-b1e6a --only firestore:rules,firestore:indexes
```

Before later deployments, read the latest live policies and merge rules/indexes added by other modules. Do not replace newer policies with an older local file.

## Local validation

Node 24 and JDK 21 were used for the emulator tooling; Android source compatibility stays Java 17.

```sh
cd firebase
npm ci
npm test
npm run emulators
```

The scripts explicitly use **demo-purrfolio-care**. In a separate terminal at the repository root:

```sh
./gradlew -PcareUseEmulators=true :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.purrfolio.mco1.test/androidx.test.runner.AndroidJUnitRunner
```

The Android emulator connects Auth and Firestore through host address `10.0.2.2`. A separate named Firebase app with demo credentials keeps emulator sessions distinct from production. Release builds and builds without `-PcareUseEmulators=true` use the supplied live Firebase config.

```sh
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Rules tests cover ownership, original profile access, malformed payloads, stale updates, per-pet pagination, and local-audio metadata bounds/privacy. Java tests cover voice parsing, missing details, ambiguous names, and date/time resolution. Android tests exercise the actual Firebase repository: CRUD, persisted history, account isolation, stale edits, idempotent creation, and audio metadata without uploading bytes.

AGP 8.7.3's UTP launcher encountered a `kotlin.enums.EnumEntriesKt` tooling classpath error before executing tests. The direct ADB instrumentation command above runs the same test APK successfully without adding Kotlin app source or the Kotlin Android plugin.
