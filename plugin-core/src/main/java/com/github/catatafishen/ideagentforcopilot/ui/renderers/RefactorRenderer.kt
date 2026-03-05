package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders refactor results (rename, safe_delete, extract_method, inline)
 * as a status card with the operation summary and affected files.
 *
 * Input formats:
 * ```
 * ✅ Renamed 'oldName' to 'newName'
 *   Updated 5 references across the project.
 *   File: path/to/File.java
 * ```
 * ```
 * ✅ Safely deleted 'symbolName' (no usages found).
 *   File: path/to/File.java
 * ```
 * ```
 * Cannot safely delete 'symbolName' — it has 3 usages:
 *   File.java:42
 *   Other.java:10
 * ```
 */
internal object RefactorRenderer : ToolResultRenderer {

    private val RENAME = Regex("""^✅\s*Renamed '(.+)' to '(.+)'""")
    private val DELETE_OK = Regex("""^✅\s*Safely deleted '(.+)'""")
    private val EXTRACT = Regex("""^✅\s*Extracted method '(.+)'""")
    private val INLINE = Regex("""^✅\s*Inlined '(.+)'""")
    private val DELETE_FAIL = Regex("""^Cannot safely delete '(.+)' — it has (\d+) usages?:""")
    private val REF_COUNT = Regex("""Updated (\d+) references?""")
    private val FILE_LINE = Regex("""^\s*File:\s+(.+)$""")
    private val USAGE_LINE = Regex("""^\s+(.+?):(\d+)""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val first = lines.first().trim()

        val renameMatch = RENAME.find(first)
        val deleteOkMatch = DELETE_OK.find(first)
        val extractMatch = EXTRACT.find(first)
        val inlineMatch = INLINE.find(first)
        val deleteFailMatch = DELETE_FAIL.find(first)

        if (renameMatch == null && deleteOkMatch == null && extractMatch == null
            && inlineMatch == null && deleteFailMatch == null
        ) return null

        val sb = StringBuilder("<div class='refactor-result'>")

        when {
            renameMatch != null -> renderRename(sb, renameMatch, lines)
            deleteOkMatch != null -> renderDeleteOk(sb, deleteOkMatch, lines)
            extractMatch != null -> renderSuccess(sb, "Extracted method", extractMatch.groupValues[1], lines)
            inlineMatch != null -> renderSuccess(sb, "Inlined", inlineMatch.groupValues[1], lines)
            deleteFailMatch != null -> renderDeleteFail(sb, deleteFailMatch, lines)
        }

        sb.append("</div>")
        return sb.toString()
    }

    private fun renderRename(sb: StringBuilder, match: MatchResult, lines: List<String>) {
        val oldName = match.groupValues[1]
        val newName = match.groupValues[2]
        val refCount = lines.firstNotNullOfOrNull { REF_COUNT.find(it) }?.groupValues?.get(1)
        val file = extractFile(lines)

        sb.append("<div class='refactor-header refactor-success'>")
        sb.append("<span class='build-icon'>✓</span>")
        sb.append("<span class='build-status'>Renamed</span>")
        sb.append("</div>")

        sb.append("<div class='refactor-detail'>")
        sb.append("<span class='refactor-old'>${esc(oldName)}</span>")
        sb.append("<span class='refactor-arrow'>→</span>")
        sb.append("<span class='refactor-new'>${esc(newName)}</span>")
        sb.append("</div>")

        appendMeta(sb, refCount, file)
    }

    private fun renderDeleteOk(sb: StringBuilder, match: MatchResult, lines: List<String>) {
        val name = match.groupValues[1]
        val file = extractFile(lines)

        sb.append("<div class='refactor-header refactor-success'>")
        sb.append("<span class='build-icon'>✓</span>")
        sb.append("<span class='build-status'>Deleted</span>")
        sb.append("<span class='refactor-symbol'>${esc(name)}</span>")
        sb.append("</div>")

        if (file != null) {
            sb.append("<div class='refactor-meta'>")
            sb.append("<span class='git-file-path'>${esc(file.substringAfterLast('/'))}</span>")
            sb.append("</div>")
        }
    }

    private fun renderSuccess(sb: StringBuilder, action: String, name: String, lines: List<String>) {
        val refCount = lines.firstNotNullOfOrNull { REF_COUNT.find(it) }?.groupValues?.get(1)
        val file = extractFile(lines)

        sb.append("<div class='refactor-header refactor-success'>")
        sb.append("<span class='build-icon'>✓</span>")
        sb.append("<span class='build-status'>${esc(action)}</span>")
        sb.append("<span class='refactor-symbol'>${esc(name)}</span>")
        sb.append("</div>")

        appendMeta(sb, refCount, file)
    }

    private fun renderDeleteFail(sb: StringBuilder, match: MatchResult, lines: List<String>) {
        val name = match.groupValues[1]
        val count = match.groupValues[2]

        sb.append("<div class='refactor-header refactor-fail'>")
        sb.append("<span class='build-icon'>✗</span>")
        sb.append("<span class='build-status'>Cannot delete</span>")
        sb.append("<span class='refactor-symbol'>${esc(name)}</span>")
        sb.append("</div>")

        sb.append("<div class='refactor-meta'>")
        sb.append("<span class='build-meta'>$count usages</span>")
        sb.append("</div>")

        val usages = lines.drop(1).mapNotNull { line ->
            USAGE_LINE.find(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }
        if (usages.isNotEmpty()) {
            sb.append("<div class='refactor-usages'>")
            for ((file, line) in usages) {
                sb.append("<div class='refactor-usage'>")
                sb.append("<span class='git-file-path'>${esc(file)}</span>")
                sb.append("<span class='search-line'>:${esc(line)}</span>")
                sb.append("</div>")
            }
            sb.append("</div>")
        }
    }

    private fun extractFile(lines: List<String>): String? =
        lines.firstNotNullOfOrNull { FILE_LINE.find(it) }?.groupValues?.get(1)?.trim()

    private fun appendMeta(sb: StringBuilder, refCount: String?, file: String?) {
        if (refCount == null && file == null) return
        sb.append("<div class='refactor-meta'>")
        if (refCount != null) {
            sb.append("<span class='build-meta'>$refCount references updated</span>")
        }
        if (file != null) {
            sb.append("<span class='git-file-path'>${esc(file.substringAfterLast('/'))}</span>")
        }
        sb.append("</div>")
    }
}
