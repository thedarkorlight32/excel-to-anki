package example.genanki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipFile

class PackageTest {
    @Test
    fun `generated package has collection and media entries`() {
        val mediaDir = Files.createTempDirectory("genanki-media").toFile()
        val mp3 = File(mediaDir, "present.mp3").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val jpg = File(mediaDir, "present.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }

        val deck = Deck(123_456L, "foodeck")
        val model = basicModel()
        deck.addNote(Note(model, listOf("question [sound:present.mp3]", "answer <img src=\"present.jpg\">")))

        val written = writePackageAndExtract(AnkiPackage(deck, mediaFiles = listOf(mp3, jpg)))
        try {
            ZipFile(written.apkg).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toSet()
                assertTrue("collection.anki2" in names)
                assertTrue("media" in names)
                assertTrue("0" in names)
                assertTrue("1" in names)
            }

            val manifest = parseMediaManifest(written.extractedDir)
            assertEquals(setOf("present.mp3", "present.jpg"), manifest.values.toSet())
        } finally {
            cleanupWrittenPackage(written)
            mediaDir.deleteRecursively()
        }
    }

    @Test
    fun `model metadata and due values are persisted`() {
        val customLatexPre = "\\documentclass{article}\\begin{document}\n"
        val customLatexPost = "% custom\n\\end{document}"
        val model = basicModel(
            modelId = 987_123L,
            sortFieldIndex = 1,
            latexPre = customLatexPre,
            latexPost = customLatexPost
        )

        val deck = Deck(414_527_3927L, "foodeck")
        deck.addNote(Note(model, listOf("Capital of Washington", "Olympia"), due = 2))
        deck.addNote(Note(model, listOf("Capital of Oregon", "Salem"), due = 1))

        val written = writePackageAndExtract(AnkiPackage(deck), timestamp = Instant.ofEpochSecond(1_700_000_000))
        try {
            withCollectionConnection(written.collectionDb) { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT sfld FROM notes ORDER BY id").use { rs ->
                        val sflds = mutableListOf<String>()
                        while (rs.next()) sflds += rs.getString(1)
                        assertEquals(listOf("Olympia", "Salem"), sflds)
                    }

                    st.executeQuery("SELECT due FROM cards ORDER BY id").use { rs ->
                        val dueValues = mutableListOf<Int>()
                        while (rs.next()) dueValues += rs.getInt(1)
                        assertEquals(listOf(2, 1), dueValues)
                    }

                    st.executeQuery("SELECT models FROM col LIMIT 1").use { rs ->
                        check(rs.next())
                        val modelsNode = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(rs.getString(1))
                        val modelNode = modelsNode.get(model.modelId.toString())
                        assertEquals(customLatexPre, modelNode["latexPre"].asText())
                        assertEquals(customLatexPost, modelNode["latexPost"].asText())
                        assertEquals(1, modelNode["sortf"].asInt())
                    }
                }
            }
        } finally {
            cleanupWrittenPackage(written)
        }
    }
}
