package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders list_project_files output as a compact file listing grouped by directory,
 * with type badges and a count header.
 *
 * Input format:
 * ```
 * 42 files:
 * src/main/java/Main.java [java]
 * build/generated/Stub.java [generated java]
 * src/test/java/MyTest.java [test java]
 * ```
 */
internal object ListProjectFilesRenderer : ToolResultRenderer {

    private val COUNT_HEADER = Regex("""^(\d+)\s+files?:?\s*$""")
    private val FILE_ENTRY = Regex("""^(.+?)\s+\[([^\]]+)]$""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.size < 2) return null

        val countMatch = COUNT_HEADER.find(lines.first().trim()) ?: return null
        val fileCount = countMatch.groupValues[1].toIntOrNull() ?: return null

        val entries = lines.drop(1).mapNotNull { parseEntry(it.trim()) }
        if (entries.isEmpty()) return null

        val grouped = entries.groupBy { it.dir }

        val sb = StringBuilder("<div class='file-list-result'>")
        sb.append("<div class='file-list-header'>")
        sb.append("<span class='file-list-count'>$fileCount</span>")
        sb.append("<span class='file-list-label'>files</span>")
        sb.append("</div>")

        sb.append("<div class='file-list-entries'>")
        for ((dir, files) in grouped) {
            if (dir.isNotEmpty()) {
                sb.append("<div class='file-list-dir'>")
                sb.append("<span class='file-list-dir-name'>${esc(dir)}/</span>")
                sb.append("<span class='inspection-file-count'>${files.size}</span>")
                sb.append("</div>")
            }
            for (f in files) {
                sb.append("<div class='file-list-entry'>")
                sb.append("<span class='git-file-path'>${esc(f.name)}</span>")
                for (tag in f.tags) {
                    val cls = tagClass(tag)
                    sb.append(" <span class='file-list-tag $cls'>${esc(tag)}</span>")
                }
                sb.append("</div>")
            }
        }
        sb.append("</div></div>")
        return sb.toString()
    }

    private data class FileEntry(val dir: String, val name: String, val tags: List<String>)

    private fun parseEntry(line: String): FileEntry? {
        if (line.isBlank()) return null
        val match = FILE_ENTRY.find(line)
        val path: String
        val tags: List<String>
        if (match != null) {
            path = match.groupValues[1].trim()
            tags = match.groupValues[2].split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            path = line
            tags = emptyList()
        }
        val lastSlash = path.lastIndexOf('/')
        val dir = if (lastSlash >= 0) path.substring(0, lastSlash) else ""
        val name = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
        return FileEntry(dir, name, tags)
    }

    private fun tagClass(tag: String): String = when (tag.lowercase()) {
        "test" -> "file-tag-test"
        "generated" -> "file-tag-generated"
        "excluded" -> "file-tag-excluded"
        else -> "file-tag-type"
    }
}
