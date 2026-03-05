package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc
import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.parseDiffStats

/**
 * Renders git show output as a commit card with metadata and optional diff.
 * Reuses git-log and git-diff CSS classes to avoid duplication.
 *
 * Input format (raw git show output):
 * ```
 * commit abc123...
 * Author: John Doe <john@example.com>
 * Date:   Mon Jan 15 10:30:45 2024 -0500
 *
 *     Fix bug in login handler
 *
 *  file.txt | 2 +-
 *  1 file changed, 1 insertion(+), 1 deletion(-)
 *
 * diff --git a/file.txt b/file.txt
 * ...
 * ```
 */
internal object GitShowRenderer : ToolResultRenderer {

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null
        if (!lines.first().startsWith("commit ")) return null

        val hash = lines.first().removePrefix("commit ").trim().take(8)
        var author = ""
        var date = ""
        val messageLines = mutableListOf<String>()
        val statLines = mutableListOf<String>()
        val diffLines = mutableListOf<String>()
        var inMessage = false
        var inDiff = false
        var summaryLine = ""

        for (line in lines.drop(1)) {
            when {
                inDiff -> diffLines.add(line)
                line.startsWith("diff --git") -> { inDiff = true; diffLines.add(line) }
                line.startsWith("Author:") -> author = line.removePrefix("Author:").trim()
                line.startsWith("Date:") -> date = line.removePrefix("Date:").trim()
                line.isBlank() && !inMessage && author.isNotEmpty() -> inMessage = true
                inMessage && line.isBlank() -> inMessage = false
                inMessage -> messageLines.add(line.trim())
                line.matches(Regex(""".*\d+ files? changed.*""")) -> summaryLine = line.trim()
                line.contains(" | ") -> statLines.add(line)
            }
        }

        val message = messageLines.joinToString(" ").trim()
        if (message.isEmpty() && statLines.isEmpty() && diffLines.isEmpty()) return null

        val sb = StringBuilder("<div class='git-commit-result'>")

        // Header — reuses git-commit classes
        sb.append("<div class='git-commit-header'>")
        sb.append("<span class='git-commit-hash'>")
        sb.append("<a href='gitshow://$hash' class='git-commit-link' title='Show in IDE'>$hash</a>")
        sb.append("</span>")
        if (author.isNotEmpty()) sb.append(" <span class='git-show-author'>${esc(author)}</span>")
        sb.append("</div>")

        if (date.isNotEmpty()) {
            sb.append("<div class='git-show-date'>${esc(date)}</div>")
        }
        if (message.isNotEmpty()) {
            sb.append("<div class='git-commit-message'>${esc(message)}</div>")
        }

        // Stat summary — reuses git-commit-stats
        if (summaryLine.isNotEmpty()) {
            val stats = parseDiffStats(summaryLine)
            sb.append("<div class='git-commit-stats'>")
            sb.append("<span class='git-stat-files'>${esc(stats.files)}</span>")
            if (stats.insertions.isNotEmpty()) sb.append(" <span class='git-stat-ins'>+${esc(stats.insertions)}</span>")
            if (stats.deletions.isNotEmpty()) sb.append(" <span class='git-stat-del'>-${esc(stats.deletions)}</span>")
            sb.append("</div>")
        }

        // Stat-only file lines — reuse git-diff rendering
        if (statLines.isNotEmpty() && diffLines.isEmpty()) {
            sb.append("<div class='git-commit-files'>")
            for (stat in statLines) {
                val parts = stat.trim().split("|", limit = 2)
                if (parts.size == 2) {
                    val path = esc(parts[0].trim())
                    val change = esc(parts[1].trim())
                    sb.append("<div class='git-file-entry'>")
                    sb.append("<span class='git-file-badge git-file-mod'>M</span>")
                    sb.append("<span class='git-file-path'>$path</span>")
                    sb.append("<span class='git-stat-files'>$change</span>")
                    sb.append("</div>")
                }
            }
            sb.append("</div>")
        }

        sb.append("</div>")
        return sb.toString()
    }
}
