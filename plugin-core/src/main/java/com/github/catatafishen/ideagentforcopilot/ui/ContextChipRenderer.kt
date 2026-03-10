package com.github.catatafishen.ideagentforcopilot.ui

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.UIManager

class ContextChipRenderer(val contextData: ContextItemData) : EditorCustomElementRenderer {

    private companion object {
        private const val H_PAD = 3
    }

    private val label: String = contextData.name

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val metrics =
            inlay.editor.contentComponent.getFontMetrics(inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN))
        return H_PAD + metrics.stringWidth(label) + H_PAD
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return inlay.editor.lineHeight
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val linkColor = UIManager.getColor("Link.activeForeground")
                ?: UIManager.getColor("link.foreground")
                ?: JBColor(Color(0x58, 0x9D, 0xF6), Color(0x58, 0x9D, 0xF6))

            val font = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN)
            g2.font = font
            val metrics = g2.fontMetrics
            val textY = targetRegion.y + (targetRegion.height + metrics.ascent - metrics.descent) / 2

            g2.color = linkColor
            g2.drawString(label, targetRegion.x + H_PAD, textY)
        } finally {
            g2.dispose()
        }
    }
}

/**
 * Data associated with an inline context chip.
 * Mirrors the fields of the old `ContextItem` but is a standalone data class
 * so the renderer can carry it without depending on the tool window's private types.
 */
data class ContextItemData(
    val path: String,
    val name: String,
    val startLine: Int,
    val endLine: Int,
    val fileTypeName: String?,
    val isSelection: Boolean
)
