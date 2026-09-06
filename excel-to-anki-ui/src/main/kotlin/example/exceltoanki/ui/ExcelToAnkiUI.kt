package example.exceltoanki.ui

import javax.swing.*
import javax.swing.border.EmptyBorder
import java.awt.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.concurrent.thread

class ExcelToAnkiUI : JFrame("Excel to Anki Converter") {
    private val state = ConversionState()
    private val service = ConversionService()
    
    private lateinit var filePathLabel: JLabel
    private lateinit var sourceLanguageDropdown: JComboBox<String>
    private lateinit var targetLanguageDropdown: JComboBox<String>
    private lateinit var uploadButton: JButton
    private lateinit var convertButton: JButton
    private lateinit var progressBar: JProgressBar
    private lateinit var progressLabel: JLabel
    private lateinit var statusTextArea: JTextArea

    init {
        setupUI()
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        setSize(600, 500)
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun setupUI() {
        contentPane.layout = BorderLayout(10, 10)
        (contentPane as JPanel).border = EmptyBorder(10, 10, 10, 10)

        // Top panel: File selection and language dropdowns
        val topPanel = JPanel(GridLayout(3, 2, 10, 10))
        topPanel.border = BorderFactory.createTitledBorder("Configuration")

        // File selection
        topPanel.add(JLabel("Excel File:"))
        val filePanel = JPanel(BorderLayout(5, 0))
        filePathLabel = JLabel("No file selected")
        uploadButton = JButton("Upload")
        uploadButton.addActionListener { selectFile() }
        filePanel.add(filePathLabel, BorderLayout.CENTER)
        filePanel.add(uploadButton, BorderLayout.EAST)
        topPanel.add(filePanel)

        // Source Language
        topPanel.add(JLabel("Source Language:"))
        sourceLanguageDropdown = JComboBox(SupportedLanguage.getDisplayNames())
        sourceLanguageDropdown.selectedItem = SupportedLanguage.EN.displayName
        sourceLanguageDropdown.addActionListener {
            state.sourceLanguage = sourceLanguageDropdown.selectedItem as String
        }
        topPanel.add(sourceLanguageDropdown)

        // Target Language
        topPanel.add(JLabel("Target Language:"))
        targetLanguageDropdown = JComboBox(SupportedLanguage.getDisplayNames())
        targetLanguageDropdown.selectedItem = SupportedLanguage.NL.displayName
        targetLanguageDropdown.addActionListener {
            state.targetLanguage = targetLanguageDropdown.selectedItem as String
        }
        topPanel.add(targetLanguageDropdown)

        contentPane.add(topPanel, BorderLayout.NORTH)

        // Middle panel: Progress
        val middlePanel = JPanel()
        middlePanel.layout = BoxLayout(middlePanel, BoxLayout.Y_AXIS)
        middlePanel.border = BorderFactory.createTitledBorder("Progress")

        progressBar = JProgressBar(0, 100)
        progressBar.isStringPainted = true
        progressBar.string = "Ready"
        middlePanel.add(progressBar)

        progressLabel = JLabel("No conversion in progress")
        progressLabel.font = Font("Arial", Font.PLAIN, 12)
        middlePanel.add(Box.createVerticalStrut(5))
        middlePanel.add(progressLabel)

        contentPane.add(middlePanel, BorderLayout.CENTER)

        // Bottom panel: Convert button and status
        val bottomPanel = JPanel()
        bottomPanel.layout = BoxLayout(bottomPanel, BoxLayout.Y_AXIS)

        convertButton = JButton("Convert to APKG")
        convertButton.font = Font("Arial", Font.BOLD, 14)
        convertButton.preferredSize = Dimension(200, 50)
        convertButton.addActionListener { startConversion() }
        
        val buttonPanel = JPanel()
        buttonPanel.add(convertButton)
        bottomPanel.add(buttonPanel)

        bottomPanel.add(Box.createVerticalStrut(5))

        statusTextArea = JTextArea(4, 50)
        statusTextArea.isEditable = false
        statusTextArea.lineWrap = true
        statusTextArea.wrapStyleWord = true
        statusTextArea.font = Font("Monospaced", Font.PLAIN, 11)
        val scrollPane = JScrollPane(statusTextArea)
        scrollPane.border = BorderFactory.createTitledBorder("Status")
        bottomPanel.add(scrollPane)

        contentPane.add(bottomPanel, BorderLayout.SOUTH)
    }

    private fun selectFile() {
        val selectedPath = service.selectExcelFile()
        if (selectedPath != null) {
            state.selectedFilePath = selectedPath
            filePathLabel.text = selectedPath.substringAfterLast('/')
            filePathLabel.toolTipText = selectedPath
            statusTextArea.text = "Selected file: $selectedPath"
        }
    }

    private fun startConversion() {
        if (state.selectedFilePath.isEmpty()) {
            showError("Please select an Excel file first")
            return
        }

        if (state.isConverting) {
            showError("Conversion is already in progress")
            return
        }

        state.isConverting = true
        convertButton.isEnabled = false
        uploadButton.isEnabled = false
        sourceLanguageDropdown.isEnabled = false
        targetLanguageDropdown.isEnabled = false
        statusTextArea.text = ""

        thread {
            service.convertFile(
                state.selectedFilePath,
                state.sourceLanguage,
                state.targetLanguage,
                { progress, message ->
                    SwingUtilities.invokeLater {
                        progressBar.value = (progress * 100).toInt()
                        progressBar.string = "${(progress * 100).toInt()}%"
                        progressLabel.text = message
                    }
                },
                { result ->
                    SwingUtilities.invokeLater {
                        state.isConverting = false
                        convertButton.isEnabled = true
                        uploadButton.isEnabled = true
                        sourceLanguageDropdown.isEnabled = true
                        targetLanguageDropdown.isEnabled = true

                        if (result?.startsWith("Error") == true) {
                            statusTextArea.text = result
                            progressBar.value = 0
                            progressBar.string = "Failed"
                            progressLabel.text = "Conversion failed"
                        } else {
                            statusTextArea.text = result ?: "Conversion complete"
                            progressBar.value = 100
                            progressBar.string = "Complete"
                            progressLabel.text = "Conversion successful"
                        }
                    }
                }
            )
        }
    }

    private fun showError(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        )
        statusTextArea.text = message
    }
}

fun main() {
    SwingUtilities.invokeLater {
        ExcelToAnkiUI()
    }
}
