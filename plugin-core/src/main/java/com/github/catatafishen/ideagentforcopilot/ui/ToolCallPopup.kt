package com.github.catatafishen.ideagentforcopilot.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal object ToolCallPopup {

    private var currentPopup: com.intellij.openapi.ui.popup.JBPopup? = null

    private val KIND_COLORS = mapOf(
        "read" to JBColor(Color(0x3A, 0x95, 0x95), Color(100, 185, 185)),
        "edit" to JBColor(Color(0xA0, 0x7A, 0x3A), Color(205, 155, 95)),
        "execute" to JBColor(Color(0x4A, 0x90, 0x4A), Color(130, 190, 130)),
        "search" to JBColor(Color(0x3A, 0x95, 0x95), Color(100, 185, 185)),
        "think" to JBColor(Color(0x7A, 0x70, 0xA8), Color(170, 155, 210)),
        "other" to JBColor(Color(0x78, 0x7C, 0x80), Color(160, 165, 170)),
    )

    /**
     * Shows a popup with tool result and optional parameters.
     *
     * @param kind         tool kind ("read", "edit", "execute", etc.) — drives the background tint
     * @param paramsPanel  Swing component for the parameters section, or null to omit
     * @param resultPanel  Swing component for the result section
     */
    fun show(project: Project, title: String, kind: String, paramsPanel: JComponent?, resultPanel: JComponent) {
        currentPopup?.cancel()

        val kindColor = KIND_COLORS[kind] ?: KIND_COLORS["other"]!!
        val panelBg = UIUtil.getPanelBackground()
        val tintedBg = blendColor(kindColor, panelBg, 0.07)

        val contentPanel = buildContentPanel(tintedBg, resultPanel, paramsPanel)

        val scrollPane = JBScrollPane(contentPanel).apply {
            preferredSize = Dimension(JBUI.scale(520), JBUI.scale(400))
            border = JBUI.Borders.empty()
        }

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, contentPanel)
            .setTitle(title)
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(false)
            .setCancelKeyEnabled(true)
            .setMinSize(Dimension(JBUI.scale(300), JBUI.scale(150)))
            .createPopup()
        currentPopup = popup

        val frame = WindowManager.getInstance().getFrame(project)
        if (frame != null) {
            val center = Point(
                frame.x + (frame.width - JBUI.scale(520)) / 2,
                frame.y + (frame.height - JBUI.scale(400)) / 2,
            )
            popup.showInScreenCoordinates(frame.rootPane, center)
        } else {
            popup.showInFocusCenter()
        }
    }

    private fun buildContentPanel(bg: Color, resultPanel: JComponent, paramsPanel: JComponent?): JPanel {
        val muted = blendColor(UIUtil.getLabelForeground(), bg, 0.55)
        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = bg
            border = JBUI.Borders.empty(6, 10)
        }

        panel.add(sectionLabel("Result", muted))
        resultPanel.alignmentX = JComponent.LEFT_ALIGNMENT
        panel.add(resultPanel)

        if (paramsPanel != null) {
            panel.add(sectionLabel("Parameters", muted))
            paramsPanel.alignmentX = JComponent.LEFT_ALIGNMENT
            panel.add(paramsPanel)
        }

        return panel
    }

    private fun sectionLabel(text: String, color: Color): JBLabel {
        val font = UIUtil.getLabelFont()
        return JBLabel(text).apply {
            foreground = color
            this.font = font.deriveFont(font.size2D - 2f).deriveFont(java.awt.Font.BOLD)
            border = BorderFactory.createEmptyBorder(6, 0, 4, 0)
            alignmentX = JComponent.LEFT_ALIGNMENT
        }
    }

    private fun blendColor(fg: Color, bg: Color, alpha: Double): Color = Color(
        (fg.red * alpha + bg.red * (1 - alpha)).toInt().coerceIn(0, 255),
        (fg.green * alpha + bg.green * (1 - alpha)).toInt().coerceIn(0, 255),
        (fg.blue * alpha + bg.blue * (1 - alpha)).toInt().coerceIn(0, 255),
    )
}
