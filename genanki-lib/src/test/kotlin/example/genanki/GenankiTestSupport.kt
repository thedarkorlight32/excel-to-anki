package example.genanki

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.zip.ZipFile

internal data class WrittenPackage(val apkg: File, val extractedDir: File, val collectionDb: File)

internal fun writePackageAndExtract(pkg: AnkiPackage, timestamp: Instant = Instant.ofEpochSecond(1_700_000_000)): WrittenPackage {
    val apkg = Files.createTempFile("genanki-test", ".apkg").toFile()
    val extracted = Files.createTempDirectory("genanki-extract").toFile()
    pkg.writeToFile(apkg, timestamp)

    ZipFile(apkg).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val out = File(extracted, entry.name)
            if (entry.isDirectory) {
                out.mkdirs()
            } else {
                out.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    val db = File(extracted, "collection.anki2")
    check(db.exists()) { "collection.anki2 not found in generated package." }
    return WrittenPackage(apkg, extracted, db)
}

internal fun withCollectionConnection(dbFile: File, block: (Connection) -> Unit) {
    DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use(block)
}

internal fun cleanupWrittenPackage(written: WrittenPackage) {
    written.apkg.delete()
    written.extractedDir.deleteRecursively()
}

internal fun parseMediaManifest(extractedDir: File): Map<String, String> {
    val mapper = jacksonObjectMapper()
    val mediaFile = File(extractedDir, "media")
    if (!mediaFile.exists()) return emptyMap()
    val node = mapper.readTree(mediaFile.readText())
    val out = linkedMapOf<String, String>()
    node.fields().forEach { (k, v) -> out[k] = v.asText() }
    return out
}

internal fun basicModel(
    modelId: Long = 234_567L,
    sortFieldIndex: Int = 0,
    latexPre: String = Model.DEFAULT_LATEX_PRE,
    latexPost: String = Model.DEFAULT_LATEX_POST
): Model = Model(
    modelId = modelId,
    name = "foomodel",
    fields = listOf(ModelField("AField"), ModelField("BField")),
    templates = listOf(
        ModelTemplate(
            name = "card1",
            qfmt = "{{AField}}",
            afmt = "{{FrontSide}}<hr id=\"answer\">{{BField}}"
        )
    ),
    latexPre = latexPre,
    latexPost = latexPost,
    sortFieldIndex = sortFieldIndex
)
