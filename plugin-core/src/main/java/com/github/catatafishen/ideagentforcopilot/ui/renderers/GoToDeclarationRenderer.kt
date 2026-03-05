package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders go_to_declaration output as a compact card with the file path,
 * line number, and a code context snippet.
 *
 * Input format:
 * ```
 * Declaration of 'symbolName':
 *
 *   File: path/to/File.java
 *   Line: 42
 *   Context:
 *     → 42: public void methodName() {
 *         43:   implementation
 *         44: }
 * ```
 */
internal object GoToDeclarationRenderer : ToolResultRenderer {

    private val HEADER = Regex("""^Declaration of '(.+)':""")
    private val FILE_LINE = Regex("""^\s*File:\s+(.+)$""")
    private val LINE_NUM = Regex("""^\s*Line:\s+(\d+)""")
    private val CONTEXT_LINE = Regex("""^\s*(→\s*)?(\d+):\s*(.*)$""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val headerMatch = HEADER.find(lines.first().trim()) ?: return null
        val symbolName = headerMatch.groupValues[1]

        var filePath = ""
        var lineNumber = ""
        val contextLines = mutableListOf<Triple<Boolean, String, String>>() // (isTarget, lineNum, code)

        for (line in lines.drop(1)) {
            val fileMatch = FILE_LINE.find(line)
            val lineMatch = LINE_NUM.find(line)
            val ctxMatch = CONTEXT_LINE.find(line)
            when {
                fileMatch != null -> filePath = fileMatch.groupValues[1].trim()
                lineMatch != null -> lineNumber = lineMatch.groupValues[1]
                ctxMatch != null -> contextLines.add(
                    Triple(
                        ctxMatch.groupValues[1].isNotEmpty(),
                        ctxMatch.groupValues[2],
                        ctxMatch.groupValues[3]
                    )
                )
            }
        }

        if (filePath.isEmpty()) return null

        val fileName = filePath.substringAfterLast('/')

        val sb = StringBuilder("<div class='decl-result'>")

        sb.append("<div class='decl-header'>")
        sb.append("<span class='decl-symbol'>${esc(symbolName)}</span>")
        sb.append("</div>")

        sb.append("<div class='decl-location'>")
        sb.append("<span class='git-file-path' title='${esc(filePath)}'>${esc(fileName)}</span>")
        if (lineNumber.isNotEmpty()) {
            sb.append("<span class='search-line'>:$lineNumber</span>")
        }
        sb.append("</div>")

        if (contextLines.isNotEmpty()) {
            sb.append("<div class='decl-context'>")
            for ((isTarget, num, code) in contextLines) {
                val cls = if (isTarget) "decl-line decl-target" else "decl-line"
                sb.append("<div class='$cls'>")
                sb.append("<span class='decl-line-num'>${esc(num)}</span>")
                sb.append("<span class='decl-line-code'>${esc(code)}</span>")
                sb.append("</div>")
            }
            sb.append("</div>")
        }

        sb.append("</div>")
        return sb.toString()
    }
}
