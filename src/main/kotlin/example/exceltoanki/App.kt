package example.exceltoanki

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
	if (args.isEmpty()) {
		println("Usage: App <path-to-excel> [sourceColumnName] [targetColumnName] [sourceTtsLang] [targetTtsLang]")
		println("Defaults: source=\"English\", target=\"Farsi\", sourceTtsLang=\"en\", targetTtsLang=sourceTtsLang")
		return
	}

	val excelPath = Paths.get(args[0])
	if (!excelPath.toFile().exists()) {
		System.err.println("File not found: $excelPath")
		return
	}

	val sourceColName = if (args.size >= 2) args[1] else "English"
	val targetColName = if (args.size >= 3) args[2] else "Farsi"
	val sourceTtsLang = if (args.size >= 4) args[3] else "en"
	val targetTtsLang = if (args.size >= 5) args[4] else sourceTtsLang

	try {
		createAnkiPackageFromExcel(excelPath, sourceColName, targetColName, sourceTtsLang, targetTtsLang)
		println("Done.")
	} catch (e: Exception) {
		System.err.println("Error: ${e.message}")
		e.printStackTrace()
	}
}

/**
 * Strip HTML tags from text, leaving only the plain text content.
 */
fun stripHtmlTags(html: String): String {
	return html.replace(Regex("<[^>]*>"), "").trim()
}

/**
 * Extract text from an Excel cell while preserving formatting (bold, italic, underline, etc.).
 * Returns HTML-formatted text suitable for Anki flashcards.
 */
fun getCellTextWithFormatting(cell: Cell?): String {
	if (cell == null) return ""

	return try {
		val cellValue = cell.richStringCellValue
		if (cellValue is XSSFRichTextString) {
			buildHtmlFromRichText(cellValue)
		} else {
			// Fallback to plain text
			try {
				cell.stringCellValue.trim()
			} catch (_: Exception) {
				cell.toString().trim()
			}
		}
	} catch (_: Exception) {
		// If rich text extraction fails, use plain text
		try {
			cell.stringCellValue.trim()
		} catch (_: Exception) {
			cell.toString().trim()
		}
	}
}

/**
 * Build HTML from XSSFRichTextString, applying formatting tags for bold, italic, underline.
 */
fun buildHtmlFromRichText(richText: XSSFRichTextString): String {
	val text = richText.string
	if (text.isEmpty()) return text

	val html = StringBuilder()
	var currentTags = setOf<String>()
	var i = 0

	while (i < text.length) {
		// Get font for character at position i
		val font = try {
			richText.getFontAtIndex(i)
		} catch (_: Exception) {
			null
		}

		// Determine what formatting this character should have
		val newTags = mutableSetOf<String>()
		if (font != null) {
			if (font.bold) newTags.add("b")
			if (font.italic) newTags.add("i")
			if (font.underline != org.apache.poi.ss.usermodel.Font.U_NONE) newTags.add("u")
		}

		// Check if formatting changed
		if (newTags != currentTags) {
			// Close old tags
			for (tag in currentTags) {
				html.append("</$tag>")
			}
			// Open new tags
			for (tag in newTags.sorted()) {
				html.append("<$tag>")
			}
			currentTags = newTags
		}

		// Add the character (HTML-escaped)
		val char = text[i]
		html.append(when (char) {
			'<' -> "&lt;"
			'>' -> "&gt;"
			'&' -> "&amp;"
			'"' -> "&quot;"
			'\'' -> "&#39;"
			else -> char
		})

		i++
	}

	// Close any remaining open tags
	for (tag in currentTags) {
		html.append("</$tag>")
	}

	return html.toString().trim()
}


/**
 * Read first sheet of the Excel file. Expects header row with columns named "English" and "Farsi".
 * For each row, synthesize speech using Google Translate's TTS endpoint, write notes as CSV,
 * and produce an Anki package (.apkg) using Python genanki (for full Anki compatibility).
 */
fun createAnkiPackageFromExcel(excelPath: Path, sourceColName: String = "English", targetColName: String = "Farsi", sourceTtsLang: String = "en", targetTtsLang: String = "en") {
	FileInputStream(excelPath.toFile()).use { fis ->
		val wb = WorkbookFactory.create(fis)
		val sheet = wb.getSheetAt(0)
		val header = sheet.getRow(0) ?: throw IllegalArgumentException("First sheet must have a header row")

		val sourceCol = findColumnIndex(header, sourceColName)
		val targetCol = findColumnIndex(header, targetColName)
		if (sourceCol < 0 || targetCol < 0) {
			throw IllegalArgumentException("Missing required columns '$sourceColName' and/or '$targetColName' in first row.")
		}

		val notesCsv = StringBuilder()
		val mediaFiles = mutableListOf<Pair<String, ByteArray>>()
		// Supported TTS languages for target/back field. Extend as needed.
		val supportedTts = setOf("nl", "en")

		// iterate rows
		for (r in 1..sheet.lastRowNum) {
			val row = sheet.getRow(r) ?: continue
			val simpleEng = row.getCell(sourceCol).toString().trim()
			val source = getCellTextWithFormatting(row.getCell(sourceCol)).trim()
			val target = getCellTextWithFormatting(row.getCell(targetCol)).trim()
			if (source.isEmpty()) continue

			val srcName = "audio_${r}_src.mp3"
			
			// Synthesize using Google Translate TTS endpoint (no Cloud client library required).
			// Strip HTML tags for TTS (we want to synthesize plain text, not tags)
			try {
				val plainTextForTts = stripHtmlTags(simpleEng)
				val audioBytes = synthesizeWithGoogleTranslateTts(plainTextForTts, sourceTtsLang)
				mediaFiles.add(srcName to audioBytes)
				var front = "$source <br>[sound:$srcName]"
				var back = target
				// Attempt to synthesize back/target audio if target language is supported
				if (targetTtsLang.lowercase() in supportedTts) {
					try {
						val plainTarget = stripHtmlTags(target)
						val tgtName = "audio_${r}_tgt.mp3"
						val tgtBytes = synthesizeWithGoogleTranslateTts(plainTarget, targetTtsLang)
						mediaFiles.add(tgtName to tgtBytes)
						back = "$target <br>[sound:$tgtName]"
					} catch (e: Exception) {
						System.err.println("Warning: failed to synthesize target row $r: ${e.message}")
					}
				}
				notesCsv.append(escapeCsv(front)).append(',').append(escapeCsv(back)).append('\n')
				println("Processed row $r: '$source' -> $srcName")
			} catch (e: Exception) {
				System.err.println("Warning: failed to synthesize row $r: ${e.message}")
			}
		}

		// Write CSV and media files to a temporary directory, then use Python genanki to create .apkg
		val baseName = excelPath.fileName.toString().substringBeforeLast('.')
		val tmpWorkDir = kotlin.io.path.createTempDirectory("anki_work_").toFile()
		val notesCsvFile = File(tmpWorkDir, "notes.csv")
		val mediaDir = File(tmpWorkDir, "media")
		mediaDir.mkdirs()

		// Write notes.csv
		notesCsvFile.writeText(notesCsv.toString())
		println("Wrote notes.csv: ${notesCsvFile.absolutePath}")

		// Write media files
		for ((name, bytes) in mediaFiles) {
			File(mediaDir, name).writeBytes(bytes)
		}
		println("Wrote ${mediaFiles.size} media file(s) to: ${mediaDir.absolutePath}")

		val outApkg = excelPath.parent.resolve("$baseName.apkg").toFile()
		createApkgWithPython(outApkg, notesCsvFile, mediaDir, baseName, tmpWorkDir)
		println("Created package: ${outApkg.absolutePath}")
	}
}

fun findColumnIndex(headerRow: org.apache.poi.ss.usermodel.Row, colName: String): Int {
	for (c in 0 until headerRow.lastCellNum) {
		val v = headerRow.getCell(c)?.toString()?.trim()
		if (v.equals(colName, ignoreCase = true)) return c
	}
	return -1
}

fun escapeCsv(field: String): String {
	val needsQuotes = field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')
	var f = field.replace("\"", "\"\"")
	if (needsQuotes) f = "\"$f\""
	return f
}

/**
 * Invoke the Python genanki script to create a proper Anki .apkg from CSV + media files.
 * This ensures media files are properly included and the package is fully compatible with Anki.
 */
fun createApkgWithPython(outFile: File, notesCsvFile: File, mediaDir: File, deckName: String, tmpWorkDir: File) {
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

/**
 * Use Google Translate's public TTS endpoint. This is an undocumented endpoint and may change or be rate-limited.
 * It does not require Google Cloud credentials. For long texts, split into chunks (approx 200 chars) and append
 * the MP3 responses.
 */
fun synthesizeWithGoogleTranslateTts(text: String, lang: String = "en"): ByteArray {
	val chunks = splitToChunks(text, 200)
	val out = ByteArrayOutputStream()
	for ((idx, chunk) in chunks.withIndex()) {
		val bytes = fetchTranslateTtsChunk(chunk, lang)
		// Append bytes. Note: concatenating MP3 streams may produce multiple headers, but most players (and Anki)
		// handle concatenated MP3 data.
		out.write(bytes)
		println("  fetched chunk ${idx + 1}/${chunks.size} (${chunk.length} chars)")
	}
	return out.toByteArray()
}

private fun splitToChunks(text: String, maxLen: Int): List<String> {
	if (text.length <= maxLen) return listOf(text)
	val words = text.split(Regex("\\s+"))
	val chunks = mutableListOf<String>()
	var current = StringBuilder()
	for (w in words) {
		if (current.isEmpty()) {
			current.append(w)
		} else if (current.length + 1 + w.length <= maxLen) {
			current.append(' ').append(w)
		} else {
			chunks.add(current.toString())
			current = StringBuilder(w)
		}
	}
	if (current.isNotEmpty()) chunks.add(current.toString())
	return chunks
}

private fun fetchTranslateTtsChunk(textChunk: String, lang: String): ByteArray {
	val base = "https://translate.google.com/translate_tts"
	val q = URLEncoder.encode(textChunk, "UTF-8")
	val url = "$base?ie=UTF-8&q=$q&tl=${URLEncoder.encode(lang, "UTF-8")}&client=tw-ob"
	val conn = java.net.URL(url).openConnection() as HttpURLConnection
	conn.requestMethod = "GET"
	conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36")
	conn.setRequestProperty("Referer", "https://translate.google.com/")
	conn.connectTimeout = 20000
	conn.readTimeout = 20000

	val responseCode = conn.responseCode
	if (responseCode != 200) {
		val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "(no body)"
		throw RuntimeException("TTS request failed (HTTP $responseCode): $err")
	}

	conn.inputStream.use { ins ->
		val baos = ByteArrayOutputStream()
		val buf = ByteArray(8 * 1024)
		var read: Int
		while (ins.read(buf).also { read = it } != -1) {
			baos.write(buf, 0, read)
		}
		return baos.toByteArray()
	}
}

