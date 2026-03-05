package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders git stage results as a card with staged file list and clickable paths.
 *
 * Expected formats:
 * - `✓ Staged N file(s):\npath1\npath2`      (specific files)
 * - `✓ Staged all changes:\nM\tpath1\nA\tpath2` (--all with name-status)
 * - `✓ Nothing to stage`
 * - Error output (fallback)
 */
internal object GitStageRenderer : ToolResultRenderer {

    private val SUCCESS_PATTERN = Regex("""^✓\s+(.+)""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val firstMatch = SUCCESS_PATTERN.find(lines.first()) ?: return null

        val sb = StringBuilder("<div class='git-stage-result'>")
        sb.append("<div class='git-stage-header'>")
        sb.append("<span class='inspection-icon'>✓</span>")
        sb.append("<span class='git-stage-label'>${esc(firstMatch.groupValues[1])}</span>")
        sb.append("</div>")

        val fileLines = lines.drop(1).filter { it.isNotBlank() }
        if (fileLines.isNotEmpty()) {
            sb.append("<div class='git-commit-files'>")
            for (fileLine in fileLines) sb.append(renderFileEntry(fileLine))
            sb.append("</div>")
        }

        sb.append("</div>")
        return sb.toString()
    }

    private fun renderFileEntry(line: String): String {
        // name-status format: "M\tpath" or "A\tpath"
        val tabIdx = line.indexOf('\t')
        if (tabIdx >= 0) {
            val status = line.substring(0, tabIdx).trim()
            val path = esc(line.substring(tabIdx + 1).trim())
            val (badge, cls) = statusBadge(status)
            return "<div class='git-file-entry'><span class='git-file-badge $cls'>$badge</span> " +
                    "<a href='openfile://$path' class='git-file-path'>$path</a></div>"
        }
        // Plain path (specific file staging)
        val path = esc(line.trim())
        return "<div class='git-file-entry'><span class='git-file-badge git-file-add'>+</span> " +
                "<a href='openfile://$path' class='git-file-path'>$path</a></div>"
    }

    private fun statusBadge(status: String): Pair<String, String> = when (status) {
        "M" -> "M" to "git-file-mod"
        "A" -> "A" to "git-file-add"
        "D" -> "D" to "git-file-del"
        "R" -> "R" to "git-file-rename"
        "C" -> "C" to "git-file-rename"
        "T" -> "T" to "git-file-mod"
        else -> status to "git-file-mod"
    }
}
