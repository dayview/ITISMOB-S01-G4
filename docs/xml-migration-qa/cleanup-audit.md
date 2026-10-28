# Kotlin cleanup audit

All Kotlin source files in this repository are under `app/src/main/java/com/purrfolio/mco1/`. No Kotlin source exists in test or other app source directories.

| File | Decision | Verified reason |
| --- | --- | --- |
| `MainActivity.kt` | Keep, consolidate | Manifest launcher Activity; owns screen routing, local pet/log/reminder state, View Binding controllers, and the theme selector. Its unreachable fallback screen builder was removed. |
| `PhaseTwoScreens.kt` | Keep, migrate later | Still constructs most secondary destinations, forms, and interactions. `MainActivity` routes to it. The old App Theme implementation and unused screen list were removed. |
| `ReferenceScreens.kt` | Keep, migrate later | Still constructs Home, Pet Detail, Care, Pet Timeline, Sitter Mode, and Pet Passport; also binds the shared XML dock. The old Welcome implementation was already removed. |
| `CareArtwork.kt` | Keep | `CareArtwork` is declared by XML and used by both remaining screen builders; `PetRings` draws the concentric wellness view used by Pet Detail. |
| `DetailArtwork.kt` | Keep | `DetailIcon`, `Waveform`, and `DemoQr` are used by `PhaseTwoScreens`. |
| `BrandType.kt` | Keep | Applies the existing variable Fredoka and Nunito weights used by controllers and remaining screen builders. |
| `AppTheme.kt` | Keep | Persists and applies Light, Dark, and System selection; referenced by `MainActivity` and account settings. |

No Kotlin source file was deleted: every file still has a verified runtime or XML custom-view reference. The obsolete code removed from `MainActivity` included duplicate implementations of screens now routed through `ReferenceScreens` and `PhaseTwoScreens`, a second programmatic dock, and an unused `Account` prototype. Quick Log and Care History were moved to XML before removing their Kotlin layout builders. The reminder dialog form was also moved to XML.

The remaining programmatic UI is in `PhaseTwoScreens.kt` and `ReferenceScreens.kt`. Until those destinations are migrated individually and visually checked, XML is **not yet the primary implementation for every screen**. Keep both files and their custom drawing dependencies during that work.
