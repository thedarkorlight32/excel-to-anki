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
 * Read first sheet of the Excel file. Expects header row with columns named "English" and "Farsi".
 * For each row, synthesize speech using Google Translate's TTS endpoint, write notes as CSV,
 * and produce an Anki package (.apkg) using Python genanki (for full Anki compatibility).
 */
fun createAnkiPackageFromExcel(excelPath: Path, sourceColName: String = "English", targetColName: String = "Farsi", sourceTtsLang: String = "en", targetTtsLang: String = "en") {
	// Step 1: read excel into lightweight rows
	val reader = ExcelReader()
	val rows = reader.read(excelPath, sourceColName, targetColName)

	// Step 2: build Cards (synthesize audio where supported)
	val supportedTts = setOf("nl", "en")
	val tts: TextToSpeech = GoogleTranslateTtsFetcher()
	val builder = CardBuilder(tts, supportedTts)
	val cards = builder.build(rows, sourceTtsLang, targetTtsLang)

	// Step 3: write CSV, media and produce .apkg
	val baseName = excelPath.fileName.toString().substringBeforeLast('.')
	val outApkg = excelPath.parent.resolve("$baseName.apkg").toFile()
	val writer = AnkiWriter()
	val writeResult = writer.write(cards)
	ApkgCreator().create(outApkg, writeResult.notesCsvFile, writeResult.mediaDir, baseName, writeResult.tmpWorkDir)
	println("Created package: ${outApkg.absolutePath}")
}
