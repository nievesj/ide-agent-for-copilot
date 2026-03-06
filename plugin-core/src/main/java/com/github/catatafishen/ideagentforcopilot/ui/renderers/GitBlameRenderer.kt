package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc
import javax.swing.JComponent

/**
 * Renders git blame output as a compact table with author coloring,
 * line numbers, and code content.
 *
 * Input format (raw git blame):
 * ```
 * abc123d (John Doe 2024-01-15 10:30:45 -0500  1) public class MyClass {
 * def456a (Jane Smith 2024-01-14 14:22:10 -0500  2)   private String name;
 * ```
 */
internal object GitBlameRenderer : ToolResultRenderer {

    private val BLAME_LINE = Regex(
        """^([a-f0-9]+)\s+\((.+?)\s+(\d{4}-\d{2}-\d{2})\s+[\d:]+\s+[+-]\d{4}\s+(\d+)\)\s?(.*)$"""
    )

    override fun render(output: String): JComponent? {
        val lines = output.trimEnd().lines()
        val entries = lines.mapNotNull { parseBlameLine(it) }
        if (entries.size < 2) return null

        val authors = entries.map { it.author }.distinct()
        val authorColors = assignAuthorColors(authors)

        val sb = StringBuilder("<div class='blame-result'>")
        sb.append("<div class='blame-header'>")
        sb.append("<span class='blame-count'>${entries.size} lines</span>")
        sb.append("<span class='blame-authors'>${authors.size} authors</span>")
        sb.append("</div>")

        sb.append("<div class='blame-lines'>")
        var prevAuthor = ""
        for (entry in entries) {
            val showAuthor = entry.author != prevAuthor
            val colorVar = authorColors[entry.author] ?: "var(--fg)"
            sb.append("<div class='blame-line${if (showAuthor) " blame-group-start" else ""}'>")
            if (showAuthor) {
                sb.append("<span class='blame-author' style='color:$colorVar'>${esc(abbreviateAuthor(entry.author))}</span>")
            } else {
                sb.append("<span class='blame-author'></span>")
            }
            sb.append("<span class='blame-date'>${esc(entry.date)}</span>")
            sb.append("<span class='inspection-line'>${entry.lineNum}</span>")
            sb.append("<span class='blame-code'>${esc(entry.content)}</span>")
            sb.append("</div>")
            prevAuthor = entry.author
        }
        sb.append("</div></div>")
        return ToolRenderers.htmlPanel(sb.toString())
    }

    private data class BlameEntry(
        val hash: String,
        val author: String,
        val date: String,
        val lineNum: String,
        val content: String,
    )

    private fun parseBlameLine(line: String): BlameEntry? {
        val match = BLAME_LINE.matchEntire(line) ?: return null
        return BlameEntry(
            hash = match.groupValues[1],
            author = match.groupValues[2].trim(),
            date = match.groupValues[3],
            lineNum = match.groupValues[4],
            content = match.groupValues[5],
        )
    }

    private val AUTHOR_PALETTE = listOf(
        "var(--link)", "#5cb85c", "#f0ad4e", "#d9534f",
        "#9b59b6", "#1abc9c", "#e67e22", "#3498db",
    )

    private fun assignAuthorColors(authors: List<String>): Map<String, String> =
        authors.withIndex().associate { (i, author) -> author to AUTHOR_PALETTE[i % AUTHOR_PALETTE.size] }

    private fun abbreviateAuthor(name: String): String =
        if (name.length <= 14) name else name.take(12) + "…"
}
