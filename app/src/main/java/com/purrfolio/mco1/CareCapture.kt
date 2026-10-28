package com.purrfolio.mco1

import java.util.Locale

/** Small, deterministic parser for the speech recognizer's text result. */
object CareCapture {
    data class Draft(val pet: String?, val activity: String?, val amount: String?, val time: String?, val transcript: String)
    private val activities = linkedMapOf(
        "fed" to "Feeding", "feed" to "Feeding", "feeding" to "Feeding",
        "medication" to "Medication", "medicine" to "Medication", "gave medicine" to "Medication",
        "vitamin" to "Vitamins", "water" to "Water", "groom" to "Grooming",
        "walk" to "Playtime", "play" to "Playtime", "vet" to "Vet Visit"
    )
    private val amountPattern = Regex("\\b\\d+(?:[.,]\\d+)?\\s*(?:cups?|tablespoons?|teaspoons?|tbsp|tsp|ml|milliliters?|grams?|g|kg|pills?|tablets?|doses?|scoops?)\\b", RegexOption.IGNORE_CASE)
    private val timePattern = Regex("\\b(?:at\\s+)?(?:1[0-2]|0?[1-9]):[0-5]\\d\\s*(?:am|pm)?\\b|\\b(?:at\\s+)?(?:1[0-2]|0?[1-9])\\s*(?:am|pm)\\b", RegexOption.IGNORE_CASE)

    fun parse(transcript: String, petNames: List<String>): Draft {
        val lower = transcript.lowercase(Locale.ROOT)
        val pet = petNames.sortedByDescending { it.length }.firstOrNull { name ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(name.lowercase(Locale.ROOT))}(?![\\p{L}\\p{N}])").containsMatchIn(lower)
        }
        val activity = activities.entries.firstOrNull { (word, _) ->
            Regex("\\b${Regex.escape(word)}\\w*\\b").containsMatchIn(lower)
        }?.value
        return Draft(pet, activity, amountPattern.find(transcript)?.value?.trim(),
            timePattern.find(transcript)?.value?.replace(Regex("^at\\s+", RegexOption.IGNORE_CASE), "")?.trim(), transcript.trim())
    }
}
