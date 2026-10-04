package example.exceltoanki

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import example.genanki.AnkiPackage
import example.genanki.Deck
import example.genanki.Model
import example.genanki.ModelField
import example.genanki.ModelTemplate
import example.genanki.Note
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.util.zip.ZipFile

/**
 * Creates and updates .apkg files in-process through the Kotlin genanki rewrite.
 */
class ApkgCreator {
    private val mapper = jacksonObjectMapper()

    fun create(outFile: File, notes: List<AnkiNoteFields>, mediaDir: File, deckName: String, tmpWorkDir: File) {
        val deckId = (deckName.hashCode().toLong() and 0x7fffffffL) % 1_000_000_000L
        val modelId = 1_607_392_319L
        writePackage(
            outFile = outFile,
            deckId = deckId,
            deckName = deckName,
            modelId = modelId,
            modelName = "Simple Model",
            notes = notes,
            mediaFiles = collectMediaFiles(mediaDir),
            tmpWorkDir = tmpWorkDir
        )
    }

    fun update(apkgFile: File, notes: List<AnkiNoteFields>, mediaDir: File, tmpWorkDir: File) {
        require(apkgFile.exists()) { "APKG file not found: ${apkgFile.absolutePath}" }
        val existing = loadExistingApkg(apkgFile, tmpWorkDir)
        writePackage(
            outFile = apkgFile,
            deckId = existing.deckId,
            deckName = existing.deckName,
            modelId = existing.modelId,
            modelName = existing.modelName,
            notes = existing.notes + notes,
            mediaFiles = (existing.mediaFiles + collectMediaFiles(mediaDir)).distinctBy { it.absolutePath },
            tmpWorkDir = tmpWorkDir
        )
    }

    private fun writePackage(
        outFile: File,
        deckId: Long,
        deckName: String,
        modelId: Long,
        modelName: String,
        notes: List<AnkiNoteFields>,
        mediaFiles: List<File>,
        tmpWorkDir: File
    ) {
        try {
            val model = Model(
                modelId = modelId,
                name = modelName,
                fields = listOf(ModelField("Front"), ModelField("Back")),
                templates = listOf(
                    ModelTemplate(
                        name = "Card 1",
                        qfmt = "{{Front}}",
                        afmt = "{{FrontSide}}<hr id=\"answer\">{{Back}}"
                    )
                )
            )
            val deck = Deck(deckId = deckId, name = deckName)
            notes.forEach { deck.addNote(Note(model = model, fields = listOf(it.front, it.back))) }

            val pkg = AnkiPackage(deck, mediaFiles)
            pkg.writeToFile(outFile)
        } finally {
            try {
                tmpWorkDir.deleteRecursively()
            } catch (_: Exception) {
                // keep failure non-fatal because output may already have been produced
            }
        }
    }

    private fun collectMediaFiles(mediaDir: File): List<File> {
        if (!mediaDir.isDirectory) return emptyList()
        return mediaDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    private fun loadExistingApkg(apkgFile: File, tmpWorkDir: File): ExistingApkgData {
        val extractDir = Files.createTempDirectory("existing_apkg_").toFile()
        try {
            ZipFile(apkgFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outFile = File(extractDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }

            val collectionFile = File(extractDir, "collection.anki2")
            require(collectionFile.exists()) { "Invalid APKG: missing collection.anki2" }

            val jdbcUrl = "jdbc:sqlite:${collectionFile.absolutePath}"
            return DriverManager.getConnection(jdbcUrl).use { conn ->
                val notes = conn.createStatement().use { st ->
                    st.executeQuery("SELECT flds FROM notes ORDER BY id").use { rs ->
                        val loaded = mutableListOf<AnkiNoteFields>()
                        while (rs.next()) {
                            val fields = rs.getString(1).split('\u001f')
                            if (fields.size >= 2) loaded += AnkiNoteFields(fields[0], fields[1])
                        }
                        loaded
                    }
                }

                val meta = conn.createStatement().use { st ->
                    st.executeQuery("SELECT decks, models FROM col LIMIT 1").use { rs ->
                        check(rs.next()) { "No collection metadata found in APKG" }
                        val decksNode = mapper.readTree(rs.getString(1))
                        val modelsNode = mapper.readTree(rs.getString(2))

                        val deckEntry = decksNode.fields().asSequence().firstOrNull()
                            ?: error("No valid deck found in APKG")
                        val modelEntry = modelsNode.fields().asSequence().firstOrNull()
                            ?: error("No valid model found in APKG")

                        val did = deckEntry.key.toLong()
                        val dname = deckEntry.value.path("name").asText().ifBlank { "Exported Deck" }
                        val mid = modelEntry.key.toLong()
                        val mname = modelEntry.value.path("name").asText().ifBlank { "Simple Model" }
                        ExistingMeta(did, dname, mid, mname)
                    }
                }

                val mediaFiles = loadMediaFiles(extractDir, tmpWorkDir)
                ExistingApkgData(meta.deckId, meta.deckName, meta.modelId, meta.modelName, notes, mediaFiles)
            }
        } finally {
            extractDir.deleteRecursively()
        }
    }

    private fun loadMediaFiles(extractDir: File, tmpWorkDir: File): List<File> {
        val mediaManifest = File(extractDir, "media")
        if (!mediaManifest.exists()) return emptyList()
        val manifest = mapper.readTree(mediaManifest.readText())
        val mediaTempDir = File(tmpWorkDir, "existing_media").also { it.mkdirs() }
        val files = mutableListOf<File>()
        manifest.fields().forEach { (index, filenameNode) ->
            val filename = filenameNode.asText()
            val archivedFile = File(extractDir, index)
            if (archivedFile.exists()) {
                val copied = File(mediaTempDir, filename)
                archivedFile.copyTo(copied, overwrite = true)
                files += copied
            }
        }
        return files
    }
}

private data class ExistingMeta(
    val deckId: Long,
    val deckName: String,
    val modelId: Long,
    val modelName: String
)

private data class ExistingApkgData(
    val deckId: Long,
    val deckName: String,
    val modelId: Long,
    val modelName: String,
    val notes: List<AnkiNoteFields>,
    val mediaFiles: List<File>
)
