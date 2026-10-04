package example.genanki

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AnkiPackage(deck: Deck, mediaFiles: List<File> = emptyList()) {
    private val decks = listOf(deck)
    var mediaFiles: List<File> = mediaFiles.distinctBy { it.absolutePath }

    fun writeToFile(file: File, timestamp: Instant = Instant.now()) {
        val dbFile = Files.createTempFile("genanki", ".anki2").toFile()
        try {
            val jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            DriverManager.getConnection(jdbcUrl).use { conn ->
                conn.autoCommit = false
                writeToDb(conn, timestamp)
                conn.commit()
            }
            writeZip(file, dbFile)
        } finally {
            dbFile.delete()
        }
    }

    private fun writeToDb(connection: Connection, timestamp: Instant) {
        connection.executeScript(APKG_SCHEMA)
        connection.executeScript(APKG_COL)
        val idGen = IdGenerator(timestamp.toEpochMilli())
        val timestampSeconds = timestamp.epochSecond
        decks.forEach { it.writeToDb(connection, timestampSeconds, idGen) }
    }

    private fun writeZip(file: File, dbFile: File) {
        ZipOutputStream(FileOutputStream(file)).use { out ->
            putFileEntry(out, "collection.anki2", dbFile)

            val mediaByIndex = mediaFiles.withIndex().associate { it.index.toString() to it.value }
            val mediaJson = mediaByIndex.mapValues { (_, f) -> f.name }
            out.putNextEntry(ZipEntry("media"))
            out.write(jsonMapper.writeValueAsBytes(mediaJson))
            out.closeEntry()

            mediaByIndex.forEach { (index, mediaFile) ->
                putFileEntry(out, index, mediaFile)
            }
        }
    }

    private fun putFileEntry(out: ZipOutputStream, entryName: String, file: File) {
        out.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(out) }
        out.closeEntry()
    }

    private fun Connection.executeScript(script: String) {
        createStatement().use { st ->
            script.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { st.execute(it) }
        }
    }
}
