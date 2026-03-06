package com.github.catatafishen.ideagentforcopilot.ui

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Non-modal floating dialog that shows tool call details (name, parameters, result).
 * Opens when the user clicks a tool chip in the chat panel.
 * Uses native Swing components so it can be moved freely across the entire IDE.
 */
internal class ToolCallPopup private constructor(
    project: Project,
    private val toolTitle: String,
    private val arguments: String?,
    private val result: String?,
    private val status: String?
) : DialogWrapper(project, false) {

    companion object {
        private var current: ToolCallPopup? = null

        fun show(project: Project, title: String, arguments: String?, result: String?, status: String?) {
            current?.close(CLOSE_EXIT_CODE)
            current = ToolCallPopup(project, title, arguments, result, status).also { it.show() }
        }
    }

    init {
        this.title = toolTitle
        isModal = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(8)))
        panel.preferredSize = Dimension(JBUI.scale(520), JBUI.scale(400))

        val content = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)
        }

        if (!arguments.isNullOrBlank()) {
            content.add(createSection("Parameters", prettyJson(arguments)))
        }

        val resultText = result ?: if (status == "failed") "✖ Failed" else "Completed"
        content.add(createSection("Result", resultText))

        val scrollPane = JBScrollPane(
            content,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    private fun createSection(label: String, text: String): JPanel {
        val section = JPanel(BorderLayout(0, JBUI.scale(4)))
        section.alignmentX = JPanel.LEFT_ALIGNMENT
        section.border = JBUI.Borders.emptyBottom(8)

        val header = JBLabel(label).apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(font.size2D - 1f)
        }
        section.add(header, BorderLayout.NORTH)

        val textArea = JBTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = JBUI.Fonts.create("JetBrains Mono", JBUI.Fonts.smallFont().size)
            border = JBUI.Borders.empty(6)
            background = JBColor(
                java.awt.Color(0xF5, 0xF5, 0xF5),
                java.awt.Color(0x2B, 0x2D, 0x30)
            )
            rows = text.lines().size.coerceIn(2, 15)
        }

        val scroll = JBScrollPane(
            textArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        scroll.maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(300))
        section.add(scroll, BorderLayout.CENTER)
        return section
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)

    override fun getOKAction(): Action = super.getOKAction().apply { putValue(Action.NAME, "Close") }

    override fun doCancelAction() {
        current = null
        super.doCancelAction()
    }

    override fun doOKAction() {
        current = null
        super.doOKAction()
    }

    private fun prettyJson(json: String): String {
        return try {
            val el = JsonParser.parseString(json)
            GsonBuilder().setPrettyPrinting().create().toJson(el)
        } catch (_: Exception) {
            json
        }
    }
}
