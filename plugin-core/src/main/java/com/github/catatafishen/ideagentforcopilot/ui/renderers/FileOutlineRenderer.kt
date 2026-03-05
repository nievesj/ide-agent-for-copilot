package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders file outline results as a structured list with type badges
 * (C=class, I=interface, E=enum, M=method, F=field, ƒ=function)
 * and line numbers.
 *
 * Output format:
 * ```
 * Outline of path/to/File.java:
 *   5: class MyClass
 *   8: method main
 *   12: field count
 * ```
 */
internal object FileOutlineRenderer : ToolResultRenderer {

    private val ENTRY_PATTERN = Regex("""^\s*(\d+):\s+(\w+)\s+(.+)$""")
    private val HEADER_PATTERN = Regex("""^Outline of (.+):$""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val headerMatch = HEADER_PATTERN.find(lines.first()) ?: return null
        val filePath = headerMatch.groupValues[1]
        val fileName = filePath.substringAfterLast('/')

        val entries = lines.drop(1).mapNotNull { ENTRY_PATTERN.find(it.trim()) }
        if (entries.isEmpty()) return null

        val sb = StringBuilder("<div class='outline-result'>")

        sb.append("<div class='outline-header'>")
        sb.append("<span class='git-file-path' title='${esc(filePath)}'>${esc(fileName)}</span>")
        sb.append("<span class='inspection-file-count'>${entries.size}</span>")
        sb.append("</div>")

        sb.append("<div class='outline-entries'>")
        for (entry in entries) {
            val line = entry.groupValues[1]
            val type = entry.groupValues[2]
            val name = entry.groupValues[3]
            val badgeInfo = typeBadge(type)
            sb.append("<div class='outline-entry'>")
            sb.append("<span class='git-file-badge ${badgeInfo.second}'>${badgeInfo.first}</span>")
            sb.append("<span class='outline-name'>${esc(name)}</span>")
            sb.append("<span class='inspection-line'>:$line</span>")
            sb.append("</div>")
        }
        sb.append("</div></div>")
        return sb.toString()
    }

    private fun typeBadge(type: String): Pair<String, String> = when (type.lowercase()) {
        "class" -> "C" to "outline-badge-class"
        "interface" -> "I" to "outline-badge-interface"
        "enum" -> "E" to "outline-badge-enum"
        "method" -> "M" to "outline-badge-method"
        "function" -> "ƒ" to "outline-badge-method"
        "field" -> "F" to "outline-badge-field"
        "property" -> "P" to "outline-badge-field"
        else -> type.take(1).uppercase() to "outline-badge-class"
    }
}
