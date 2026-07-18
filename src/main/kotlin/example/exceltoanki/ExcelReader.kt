package example.exceltoanki

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import java.io.FileInputStream
import java.nio.file.Path

/**
 * Simple reader that converts the first sheet of the Excel file into a list of rows with
 * simple text (for TTS) and HTML for source/target (using existing helpers).
 */
class ExcelReader {
    data class Row(val simpleSourceText: String, val simpleTargetText: String, val sourceHtml: String, val targetHtml: String)

    fun read(excelPath: Path, sourceColName: String, targetColName: String): List<Row> {
        val rows = mutableListOf<Row>()
        FileInputStream(excelPath.toFile()).use { fis ->
            val wb = WorkbookFactory.create(fis)
            val sheet = wb.getSheetAt(0)
            val header = sheet.getRow(0) ?: throw IllegalArgumentException("First sheet must have a header row")

            val sourceCol = findColumnIndex(header, sourceColName)
            val targetCol = findColumnIndex(header, targetColName)
            if (sourceCol < 0 || targetCol < 0) {
                throw IllegalArgumentException("Missing required columns '$sourceColName' and/or '$targetColName' in first row.")
            }

            for (r in 1..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue
                val simpleSourceText = try { row.getCell(sourceCol).toString().trim() } catch (_: Exception) { "" }
                val simpleTargetText = try { row.getCell(targetCol).toString().trim() } catch (_: Exception) { "" }
                val sourceHtml = getCellTextWithFormatting(row.getCell(sourceCol)).trim()
                val targetHtml = getCellTextWithFormatting(row.getCell(targetCol)).trim()
                if (sourceHtml.isEmpty()) continue
                rows.add(Row(simpleSourceText, simpleTargetText, sourceHtml, targetHtml))
            }
        }
        return rows
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

    private fun findColumnIndex(headerRow: org.apache.poi.ss.usermodel.Row, colName: String): Int {
        for (c in 0 until headerRow.lastCellNum) {
            val v = headerRow.getCell(c)?.toString()?.trim()
            if (v.equals(colName, ignoreCase = true)) return c
        }
        return -1
    }


}


