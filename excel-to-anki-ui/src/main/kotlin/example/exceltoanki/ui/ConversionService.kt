package example.exceltoanki.ui

import example.exceltoanki.createAnkiPackageFromExcel
import java.io.File
import java.nio.file.Paths
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * State management for the UI
 */
class ConversionState {
    var selectedFilePath: String = ""
    var sourceLanguage: String = SupportedLanguage.NL.displayName
    var targetLanguage: String = SupportedLanguage.EN.displayName
    var isConverting: Boolean = false
    var progress: Float = 0f
    var progressMessage: String = ""
    var errorMessage: String? = null
    var successMessage: String? = null

    fun reset() {
        selectedFilePath = ""
        sourceLanguage = SupportedLanguage.NL.displayName
        targetLanguage = SupportedLanguage.EN.displayName
        isConverting = false
        progress = 0f
        progressMessage = ""
        errorMessage = null
        successMessage = null
    }
}

/**
 * Service class to handle conversion operations
 */
class ConversionService {
    fun selectExcelFile(): String? {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("Excel files (*.xlsx, *.xls)", "xlsx", "xls")
        val result = fileChooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            null
        }
    }

    fun convertFile(
        filePath: String,
        sourceLanguage: String,
        targetLanguage: String,
        onProgress: (Float, String) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        try {
            onProgress(0f, "Starting conversion...")
            val path = Paths.get(filePath)
            
            if (!path.toFile().exists()) {
                onComplete("File not found: $filePath")
                return
            }

            onProgress(0.1f, "Reading Excel file...")
            val sourceColName = languageToColumnName(sourceLanguage)
            val targetColName = languageToColumnName(targetLanguage)
            val sourceTtsLang = languageToTtsCode(sourceLanguage)
            val targetTtsLang = languageToTtsCode(targetLanguage)

            // Read Excel to get row count for progress tracking
            val excelReader = example.exceltoanki.ExcelReader()
            val rows = excelReader.read(path, sourceColName, targetColName)
            val totalRows = rows.size

            if (totalRows == 0) {
                onComplete("No data rows found in Excel file")
                return
            }

            onProgress(0.15f, "Building cards ($totalRows rows)...")
            
            // Create progress subscriber to track card creation
            val progressSubscriber = UIProgressSubscriber(totalRows) { progress, message ->
                // Scale the card building progress from 0.15 to 0.85
                val scaledProgress = 0.15f + (progress * 0.7f)
                onProgress(scaledProgress, message)
            }

            onProgress(0.2f, "Creating Anki package...")
            createAnkiPackageFromExcel(
                path,
                sourceColName,
                targetColName,
                sourceTtsLang,
                targetTtsLang,
                null,
                progressSubscriber
            )

            onProgress(1f, "Conversion complete!")
            val baseName = path.fileName.toString().substringBeforeLast('.')
            val apkgPath = path.parent.resolve("$baseName.apkg").toFile().absolutePath
            onComplete("Package created: $apkgPath")
        } catch (e: Exception) {
            onComplete("Error: ${e.message}")
        }
    }

    private fun languageToColumnName(displayName: String): String {
        val language = SupportedLanguage.fromDisplayName(displayName)
        return language?.columnName ?: displayName
    }

    private fun languageToTtsCode(displayName: String): String {
        val language = SupportedLanguage.fromDisplayName(displayName)
        return language?.ttsCode ?: displayName.lowercase()
    }
}
