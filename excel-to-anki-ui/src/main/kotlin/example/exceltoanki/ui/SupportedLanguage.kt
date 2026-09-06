package example.exceltoanki.ui

/**
 * Enum representing all supported languages for the Excel to Anki converter.
 * Each language includes mappings for column names, TTS codes, and display names.
 */
enum class SupportedLanguage(
    val displayName: String,
    val columnName: String,
    val ttsCode: String
) {
    EN("English", "English", "en"),
    DE("German", "German", "de"),
    NL("Dutch", "Dutch", "nl"),
    TR("Turkish", "Turkish", "tr"),
    AR("Arabic", "Arabic", "ar"),
    FA("Farsi (No Audio)", "Farsi", "none"),
    NONE("None (No Audio)", "NONE", "none");

    companion object {
        /**
         * Get all language names for display in dropdowns
         */
        fun getDisplayNames(): Array<String> {
            return entries.map { it.displayName }.toTypedArray()
        }

        /**
         * Get language by display name
         */
        fun fromDisplayName(displayName: String): SupportedLanguage? {
            return entries.firstOrNull { it.displayName == displayName }
        }

        /**
         * Get language by enum name
         */
        fun fromName(name: String): SupportedLanguage? {
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
