package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc
import javax.swing.JComponent

/**
 * Renders run_command output with a status header (success/fail),
 * exit code, and the command output in a monospace block.
 *
 * Input formats:
 * ```
 * ✅ Command succeeded
 *
 * <output>
 * ```
 * ```
 * ❌ Command failed (exit code 1)
 * (showing last 8000 chars — use offset=0 for beginning)
 *
 * <output>
 * ```
 */
internal object RunCommandRenderer : ToolResultRenderer {

    private val SUCCESS_HEADER = Regex("""^✅\s*Command succeeded""")
    private val FAIL_HEADER = Regex("""^❌\s*Command failed\s*\(exit code (\d+)\)""")
    private val TIMEOUT_HEADER = Regex("""^Command timed out after (\d+) seconds""")
    private val PAGINATION_NOTE = Regex("""\(showing last \d+ chars.*\)""")

    override fun render(output: String): JComponent? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val firstLine = lines.first().trim()

        val successMatch = SUCCESS_HEADER.find(firstLine)
        val failMatch = FAIL_HEADER.find(firstLine)
        val timeoutMatch = TIMEOUT_HEADER.find(firstLine)

        if (successMatch == null && failMatch == null && timeoutMatch == null) return null

        val sb = StringBuilder("<div class='cmd-result'>")

        when {
            successMatch != null -> {
                sb.append("<div class='cmd-header cmd-success'>")
                sb.append("<span class='build-icon'>✓</span>")
                sb.append("<span class='build-status'>Command succeeded</span>")
                sb.append("</div>")
            }
            failMatch != null -> {
                val exitCode = failMatch.groupValues[1]
                sb.append("<div class='cmd-header cmd-fail'>")
                sb.append("<span class='build-icon'>✗</span>")
                sb.append("<span class='build-status'>Command failed</span>")
                sb.append("<span class='build-meta'>exit code $exitCode</span>")
                sb.append("</div>")
            }
            timeoutMatch != null -> {
                val secs = timeoutMatch.groupValues[1]
                sb.append("<div class='cmd-header cmd-timeout'>")
                sb.append("<span class='build-icon'>⏱</span>")
                sb.append("<span class='build-status'>Timed out</span>")
                sb.append("<span class='build-meta'>after ${secs}s</span>")
                sb.append("</div>")
            }
        }

        val body = lines.drop(1)
            .filterNot { PAGINATION_NOTE.containsMatchIn(it) }
            .joinToString("\n")
            .trim()

        if (body.isNotEmpty()) {
            sb.append("<pre class='tool-output'><code>${esc(body)}</code></pre>")
        }

        sb.append("</div>")
        return ToolRenderers.htmlPanel(sb.toString())
    }
}
