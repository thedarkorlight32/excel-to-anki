package example.exceltoanki

import java.io.File

/**
 * Result of writing CSV and media files for Anki packaging.
 */
data class AnkiWriteResult(val notesCsvFile: File, val mediaDir: File, val tmpWorkDir: File)

/**
 * Writes a list of Card instances to notes.csv and media files. Returns paths needed to
 * create the .apkg (the caller is responsible for invoking the Python helper).
 */
class AnkiWriter {
    fun write(cards: List<Card>): AnkiWriteResult {
        val tmpWorkDir = kotlin.io.path.createTempDirectory("anki_work_").toFile()
        val notesCsvFile = File(tmpWorkDir, "notes.csv")
        val mediaDir = File(tmpWorkDir, "media")
        mediaDir.mkdirs()

        val sb = StringBuilder()
        for ((i, c) in cards.withIndex()) {
            val srcName = "audio_${i + 1}_src.mp3"
            val tgtName = "audio_${i + 1}_tgt.mp3"
            if (c.frontAudio != null) File(mediaDir, srcName).writeBytes(c.frontAudio)
            if (c.backAudio != null) File(mediaDir, tgtName).writeBytes(c.backAudio)

            val frontField = if (c.frontAudio != null) "${c.frontHtml} <br>[sound:$srcName]" else c.frontHtml
            val backField = if (c.backAudio != null) "${c.backHtml} <br>[sound:$tgtName]" else c.backHtml
            sb.append(escapeCsv(frontField)).append(',').append(escapeCsv(backField)).append('\n')
        }

        notesCsvFile.writeText(sb.toString())
        println("Wrote notes.csv: ${notesCsvFile.absolutePath}")
        println("Wrote ${mediaDir.listFiles()?.size ?: 0} media file(s) to: ${mediaDir.absolutePath}")

        return AnkiWriteResult(notesCsvFile, mediaDir, tmpWorkDir)
    }
    private fun escapeCsv(field: String): String {
        val needsQuotes = field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')
        var f = field.replace("\"", "\"\"")
        if (needsQuotes) f = "\"$f\""
        return f
    }

}
