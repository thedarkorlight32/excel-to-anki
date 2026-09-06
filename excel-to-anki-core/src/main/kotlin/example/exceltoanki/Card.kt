package example.exceltoanki

/**
 * Card model representing front/back HTML and optional audio bytes.
 */
data class Card(
    val frontHtml: String,
    val frontAudio: ByteArray?,
    val backHtml: String,
    val backAudio: ByteArray?
)
