package example.exceltoanki

import java.io.File

/**
 * Responsible for invoking the Python helper to create an Anki .apkg from CSV + media files.
 */
class ApkgCreator {
    fun create(outFile: File, notesCsvFile: File, mediaDir: File, deckName: String, tmpWorkDir: File) {
        // locate the Python script: check current directory and common project locations
        val candidates = listOf(
            File("create_apkg_from_csv.py"),
            File(System.getProperty("user.dir"), "create_apkg_from_csv.py"),
            File(System.getProperty("user.dir"), "../create_apkg_from_csv.py"),
            File("/Users/reza/workspace/excel-to-anki/create_apkg_from_csv.py")
        )
        val pythonScript = candidates.firstOrNull { it.exists() }
            ?: throw RuntimeException("Python script create_apkg_from_csv.py not found. Searched: ${candidates.map { it.absolutePath }}")

        // build the command
        val cmd = listOf(
            "python3",
            pythonScript.absolutePath,
            "--csv", notesCsvFile.absolutePath,
            "--media-dir", mediaDir.absolutePath,
            "--output", outFile.absolutePath,
            "--name", deckName
        )

        println("Invoking Python script: ${cmd.joinToString(" ")}")
        val process = ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Python script failed with exit code $exitCode")
        }

        // cleanup temp work directory
        try {
            tmpWorkDir.deleteRecursively()
        } catch (e: Exception) {
            println("Warning: failed to delete temp directory ${tmpWorkDir.absolutePath}: ${e.message}")
        }
    }
}
