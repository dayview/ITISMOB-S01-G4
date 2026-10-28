# XML layout migration QA

The migration is in progress. These are Pixel 6 emulator captures of the installed debug build at 1080 × 2400. The screenshots in this directory are review artifacts, not claims that the entire app has been migrated.

| Screen | Before | After | Result |
| --- | --- | --- | --- |
| Welcome, dark | [before](welcome-before-dark.png) | [after](welcome-after-dark.png) | Visually checked. Content area mean absolute RGB difference: 0.16/255. Status clock excluded. |
| App Theme, dark | [before](app-theme-before-dark.png) | [after](app-theme-after-dark.png) | Visually checked. Content area mean absolute RGB difference: 1.42/255. Status clock excluded. |
| App Theme, light | — | [after](app-theme-after-light.png) | Checked rendering and selection persistence. |
| Quick Log, dark | [before](quick-log-before-dark.png) | [after](quick-log-after-dark.png) | Visually checked. Content area mean absolute RGB difference: 1.84/255. Status clock excluded. |
| Care History, dark | [before](care-history-before-dark.png) | [after](care-history-after-dark.png) | Visually checked with one saved entry. Content area mean absolute RGB difference: 2.70/255. Status clock excluded. |

Build check: `ANDROID_HOME=/Users/zel/Library/Android/sdk ./gradlew :app:assembleDebug --offline` succeeded.

App Theme selection was exercised for Light, Dark, and System Default on the emulator. The original Dark Mode preference was restored after verification. Welcome and App Theme use View Binding and XML layouts. The floating bottom navigation has an XML structure shared by the existing screen builders. Other destinations still build their main content in Kotlin and require individual migration and before/after emulator checks.

Navigation smoke check: the shared XML Pets tab opened the Pets destination. That check exposed a pre-existing inset timing issue on the Kotlin screen; the render path now requests insets after attaching a screen view. A second emulator screenshot confirmed that Pets clears both system bars. Care History's XML New care entry button opened Quick Log, whose spinner, input, and save button appeared in the accessibility hierarchy. The legacy XML dock opened Home. Theme selection still persisted after the MainActivity cleanup.

See [cleanup audit](cleanup-audit.md) for retained Kotlin files and remaining migration work.
