package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders test results as a status card with pass/fail counts,
 * duration, and a colored list of failures.
 *
 * Output format:
 * ```
 * Test Results: N tests, N passed, N failed, N errors, N skipped (Xs)
 * Failures:
 *   ❌ class.method: message
 * ```
 */
internal object TestResultRenderer : ToolResultRenderer {

    private val SUMMARY_PATTERN = Regex(
        """Test Results:\s*(\d+)\s+tests?,\s*(\d+)\s+passed,\s*(\d+)\s+failed,\s*(\d+)\s+errors?,\s*(\d+)\s+skipped\s*\(([\d.]+)s\)"""
    )

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val summary = SUMMARY_PATTERN.find(lines.first()) ?: return null
        val total = summary.groupValues[1].toInt()
        val passed = summary.groupValues[2].toInt()
        val failed = summary.groupValues[3].toInt()
        val errors = summary.groupValues[4].toInt()
        val skipped = summary.groupValues[5].toInt()
        val duration = summary.groupValues[6]

        val allPassed = failed == 0 && errors == 0
        val statusCss = if (allPassed) "build-success" else "build-fail"
        val icon = if (allPassed) "✓" else "✗"

        val sb = StringBuilder("<div class='build-result'>")

        // Header — reuses build-result styling
        sb.append("<div class='build-header $statusCss'>")
        sb.append("<span class='build-icon'>$icon</span>")
        sb.append("<span class='build-status'>$total tests</span>")
        sb.append("<span class='build-meta'>${duration}s</span>")
        sb.append("</div>")

        // Stat badges
        sb.append("<div class='test-stats'>")
        if (passed > 0) sb.append("<span class='test-badge test-passed'>$passed passed</span>")
        if (failed > 0) sb.append("<span class='test-badge test-failed'>$failed failed</span>")
        if (errors > 0) sb.append("<span class='test-badge test-errors'>$errors errors</span>")
        if (skipped > 0) sb.append("<span class='test-badge test-skipped'>$skipped skipped</span>")
        sb.append("</div>")

        // Failures
        val failures = lines.drop(1).filter { it.trim().startsWith("❌") || it.trim().startsWith("\u274C") }
        if (failures.isNotEmpty()) {
            sb.append("<div class='build-messages'>")
            for (failure in failures) {
                val trimmed = failure.trim().removePrefix("❌").removePrefix("\u274C").trim()
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx > 0) {
                    val testName = esc(trimmed.substring(0, colonIdx).trim())
                    val message = esc(trimmed.substring(colonIdx + 1).trim())
                    sb.append("<div class='build-msg build-msg-error'>")
                    sb.append("<span class='test-name'>$testName</span>: $message")
                    sb.append("</div>")
                } else {
                    sb.append("<div class='build-msg build-msg-error'>${esc(trimmed)}</div>")
                }
            }
            sb.append("</div>")
        }

        sb.append("</div>")
        return sb.toString()
    }
}
