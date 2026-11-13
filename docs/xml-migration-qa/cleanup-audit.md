# Java source migration audit

All eight original app Kotlin source files have Java replacements. No `.kt` source remains in the repository. `MainActivity`, `ReferenceScreens`, `PhaseTwoScreens`, the theme and care helpers, and all custom artwork now compile as Java 17. The Android manifest still launches `com.purrfolio.mco1.MainActivity`.

`*.gradle.kts` files remain because they are Gradle Kotlin DSL build configuration, not app source. The Kotlin Android plugin and `kotlinOptions` were removed. AndroidX still supplies a transitive Kotlin standard library; the app's Java source does not use Kotlin APIs. Legacy `kotlin-stdlib-jdk7` and `kotlin-stdlib-jdk8` variants are excluded to avoid duplicate classes with the transitive standard library.

Welcome, App Theme, Quick Log, Care History, the shared dock, reminder and care dialogs, the reusable secondary-screen info row, and the root structures of reference and secondary screens use XML with View Binding. Dynamic lists, forms, Canvas artwork, and screen-specific content are still assembled from Java Views. The historical screenshots in this directory predate the final Java migration and should not be treated as a visual diff of the final build.

The final Java-only debug build passed `./gradlew :app:assembleDebug :app:lintDebug`. The Pixel 6 emulator started all 33 debug screen entry points with no process-start failures or AndroidRuntime fatal exceptions in the smoke-test log.
