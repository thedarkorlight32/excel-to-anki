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
		println("Usage: App <path-to-excel> [sourceColumnName] [targetColumnName] [sourceTtsLang] [targetTtsLang] [--update <path-to-existing-apkg>]")
		println("Defaults: source=\"English\", target=\"Farsi\", sourceTtsLang=\"en\", targetTtsLang=sourceTtsLang")
		println("Supported TTS languages: en (English), de (German), nl (Dutch), tr (Turkish), ar (Arabic)")
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

	val updateIdx = args.indexOf("--update")
	val existingApkg = if (updateIdx >= 0 && updateIdx + 1 < args.size) {
		val path = Paths.get(args[updateIdx + 1]).toFile()
		println("Update APKG file: ${path.absolutePath}")
		if (!path.exists()) {
			System.err.println("APKG file not found: ${path.absolutePath}")
			return
		}
		path
	} else {
		null
	}

	try {
		createAnkiPackageFromExcel(excelPath, sourceColName, targetColName, sourceTtsLang, targetTtsLang, existingApkg)
		println("Done.")
	} catch (e: Exception) {
		System.err.println("Error: ${e.message}")
		e.printStackTrace()
	}
}

/**
 * Read first sheet of the Excel file. Expects header row with columns named "English" and "Farsi".
 * For each row, synthesize speech using Google Translate's TTS endpoint, write notes as CSV,
 * and produce an Anki package (.apkg) using Python genanki (for full Anki compatibility).
 * If existingApkg is provided, add cards to the existing package instead of creating a new one.
 * If progressSubscriber is provided, it will receive progress updates as cards are created.
 */
fun createAnkiPackageFromExcel(
    excelPath: Path,
    sourceColName: String = "English",
    targetColName: String = "Farsi",
    sourceTtsLang: String = "en",
    targetTtsLang: String = "en",
    existingApkg: File? = null,
    progressSubscriber: CardBuildProgressSubscriber? = null
) {
	// Step 1: read excel into lightweight rows
	val reader = ExcelReader()
	val rows = reader.read(excelPath, sourceColName, targetColName)

	// Step 2: build Cards (synthesize audio where supported)
	val tts: TextToSpeech = GoogleTranslateTtsFetcher()
	val builder = CardBuilder(tts, setOf("en", "de", "nl", "tr", "ar"), progressSubscriber)
	val cards = builder.build(rows, sourceTtsLang, targetTtsLang)

	// Step 3: write CSV, media and produce/update .apkg
	val baseName = excelPath.fileName.toString().substringBeforeLast('.')
	val outApkg = existingApkg ?: excelPath.parent.resolve("$baseName.apkg").toFile()
	val writer = AnkiWriter()
	val writeResult = writer.write(cards)
	
	val creator = ApkgCreator()
	if (existingApkg != null) {
		creator.update(outApkg, writeResult.notesCsvFile, writeResult.mediaDir, writeResult.tmpWorkDir)
		println("Updated package: ${outApkg.absolutePath}")
	} else {
		creator.create(outApkg, writeResult.notesCsvFile, writeResult.mediaDir, baseName, writeResult.tmpWorkDir)
		println("Created package: ${outApkg.absolutePath}")
	}
}
