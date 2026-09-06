package example.exceltoanki

import example.exceltoanki.ExcelReader.Row

/**
 * Maps ExcelReader.Row entries into Card instances, synthesizing audio when available.
 */
class CardBuilder(
    private val tts: TextToSpeech,
    private val supportedTts: Set<String> = setOf("en", "de", "nl", "tr", "ar"),
    private val progressSubscriber: CardBuildProgressSubscriber? = null
) {

    fun build(rows: List<Row>, sourceTtsLang: String, targetTtsLang: String): List<Card> {
        val cards = mutableListOf<Card>()
        for ((idx, r) in rows.withIndex()) {
            var frontAudio: ByteArray? = null
            var backAudio: ByteArray? = null
            try {
                // Use simpleSourceText (plain) for TTS; strip HTML tags just in case
                val plain = stripHtmlTags(r.simpleSourceText)
                frontAudio = if (plain.isNotBlank()) tts.synthesize(plain, sourceTtsLang) else null
            } catch (e: Exception) {
                System.err.println("Warning: failed to synthesize front for row ${idx + 1}: ${e.message}")
                progressSubscriber?.onCardError(idx, "Front audio synthesis failed: ${e.message}")
            }

            if (targetTtsLang.lowercase() in supportedTts) {
                try {
                    val plainTarget = stripHtmlTags(r.simpleTargetText)
                    backAudio = if (plainTarget.isNotBlank()) tts.synthesize(plainTarget, targetTtsLang) else null
                } catch (e: Exception) {
                    System.err.println("Warning: failed to synthesize back for row ${idx + 1}: ${e.message}")
                    progressSubscriber?.onCardError(idx, "Back audio synthesis failed: ${e.message}")
                }
            }

            cards.add(Card(r.sourceHtml, frontAudio, r.targetHtml, backAudio))
            
            // Notify subscriber that card was created
            progressSubscriber?.onCardCreated(cards.size, rows.size)
        }
        return cards
    }

    /**
     * Strip HTML tags from text, leaving only the plain text content.
     */
    private fun stripHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
    }

}
