package example.genanki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModelAndNoteTest {
    @Test
    fun `model req matches upstream-style behavior`() {
        val testCnModel = Model(
            modelId = 345_678L,
            name = "Chinese",
            fields = listOf(ModelField("Traditional"), ModelField("Simplified"), ModelField("English")),
            templates = listOf(
                ModelTemplate("Traditional", "{{Traditional}}", "{{FrontSide}}<hr id=\"answer\">{{English}}"),
                ModelTemplate("Simplified", "{{Simplified}}", "{{FrontSide}}<hr id=\"answer\">{{English}}")
            )
        )
        val testModelWithHint = Model(
            modelId = 456_789L,
            name = "with hint",
            fields = listOf(ModelField("Question"), ModelField("Hint"), ModelField("Answer")),
            templates = listOf(
                ModelTemplate("card1", "{{Question}}{{#Hint}}<br>Hint: {{Hint}}{{/Hint}}", "{{Answer}}")
            )
        )

        assertEquals(listOf(listOf(0, "all", listOf(0)), listOf(1, "all", listOf(1))), testCnModel.req)
        assertEquals(listOf(listOf(0, "any", listOf(0, 1))), testModelWithHint.req)
    }

    @Test
    fun `notes generate cards based on req`() {
        val model = Model(
            modelId = 345_678L,
            name = "Chinese",
            fields = listOf(ModelField("Traditional"), ModelField("Simplified"), ModelField("English")),
            templates = listOf(
                ModelTemplate("Traditional", "{{Traditional}}", "{{FrontSide}}<hr id=\"answer\">{{English}}"),
                ModelTemplate("Simplified", "{{Simplified}}", "{{FrontSide}}<hr id=\"answer\">{{English}}")
            )
        )
        val deck = Deck(123_456L, "foodeck")
        deck.addNote(Note(model, listOf("中國", "中国", "China")))
        deck.addNote(Note(model, listOf("你好", "", "hello")))

        val written = writePackageAndExtract(AnkiPackage(deck))
        try {
            withCollectionConnection(written.collectionDb) { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT ord FROM cards ORDER BY id").use { rs ->
                        val ords = mutableListOf<Int>()
                        while (rs.next()) ords += rs.getInt(1)
                        assertEquals(listOf(0, 1, 0), ords)
                    }
                }
            }
        } finally {
            cleanupWrittenPackage(written)
        }
    }

    @Test
    fun `tags with spaces are rejected`() {
        val model = basicModel()
        assertThrows(IllegalArgumentException::class.java) {
            Note(model = model, fields = listOf("a", "b"), tags = listOf("bad tag"))
        }
    }

    @Test
    fun `field count mismatch raises`() {
        val model = basicModel()
        val deck = Deck(123L, "bad")
        deck.addNote(Note(model = model, fields = listOf("only-one")))

        assertThrows(IllegalArgumentException::class.java) {
            writePackageAndExtract(AnkiPackage(deck))
        }
    }

    @Test
    fun `cloze model type is not supported yet`() {
        val clozeModel = Model(
            modelId = 998_877_661L,
            name = "My Cloze Model",
            fields = listOf(ModelField("Text"), ModelField("Extra")),
            templates = listOf(ModelTemplate("My Cloze Card", "{{cloze:Text}}", "{{cloze:Text}}<br>{{Extra}}")),
            modelType = Model.CLOZE
        )
        val deck = Deck(1L, "cloze")
        deck.addNote(Note(clozeModel, listOf("{{c1::single deletion}}", "")))

        assertThrows(IllegalStateException::class.java) {
            writePackageAndExtract(AnkiPackage(deck))
        }
    }
}
