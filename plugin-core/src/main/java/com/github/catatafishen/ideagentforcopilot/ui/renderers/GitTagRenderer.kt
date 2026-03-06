package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Renderer for git_tag output.
 * Input: one tag per line from `git tag -l`.
 */
internal object GitTagRenderer : ToolResultRenderer {

    override fun render(output: String): JComponent? {
        val tags = output.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (tags.isEmpty()) return null

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        val headerLabel = JBLabel("🏷 ${tags.size} ${if (tags.size == 1) "tag" else "tags"}").apply {
            font = UIUtil.getLabelFont().deriveFont(java.awt.Font.BOLD)
            border = JBUI.Borders.emptyBottom(4)
        }
        headerLabel.alignmentX = JComponent.LEFT_ALIGNMENT
        panel.add(headerLabel)

        for (tag in tags) {
            val row = JPanel(FlowLayout(FlowLayout.LEFT, 4, 1)).apply {
                isOpaque = false
                alignmentX = JComponent.LEFT_ALIGNMENT
            }
            val badge = JBLabel("🏷").apply {
                font = JBUI.Fonts.create("JetBrains Mono", UIUtil.getLabelFont().size - 1)
            }
            val name = JBLabel(tag).apply {
                font = JBUI.Fonts.create("JetBrains Mono", UIUtil.getLabelFont().size)
                foreground = JBColor.namedColor("Link.activeForeground", UIUtil.getLabelForeground())
            }
            row.add(badge)
            row.add(name)
            panel.add(row)
        }

        return panel
    }
}
