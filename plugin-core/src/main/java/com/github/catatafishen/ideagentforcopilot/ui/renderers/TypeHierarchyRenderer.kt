package com.github.catatafishen.ideagentforcopilot.ui.renderers

import com.github.catatafishen.ideagentforcopilot.ui.renderers.ToolRenderers.esc

/**
 * Renders get_type_hierarchy output as a visual class hierarchy tree
 * with supertypes above and subtypes below.
 *
 * Input format:
 * ```
 * Type hierarchy for: com.example.MyClass (class)
 *
 * Supertypes:
 *   class Object
 *   interface Serializable
 *
 * Subtypes:
 *   class ChildA [path/ChildA.java]
 *   class ChildB [path/ChildB.java]
 * ```
 */
internal object TypeHierarchyRenderer : ToolResultRenderer {

    private val HEADER = Regex("""^Type hierarchy for:\s+(.+?)\s+\((class|interface)\)""")
    private val SECTION_HEADER = Regex("""^(Supertypes|Subtypes|Implementations):""")
    private val TYPE_ENTRY = Regex("""^\s+(class|interface|enum|annotation)\s+(\S+)(?:\s+\[(.+)])?""")
    private val NONE_FOUND = Regex("""^\s+\(none found""")

    override fun render(output: String): String? {
        val lines = output.trimEnd().lines()
        if (lines.isEmpty()) return null

        val headerMatch = HEADER.find(lines.first().trim()) ?: return null
        val typeName = headerMatch.groupValues[1]
        val typeKind = headerMatch.groupValues[2]

        val shortName = typeName.substringAfterLast('.')

        var currentSection = ""
        val supertypes = mutableListOf<TypeEntry>()
        val subtypes = mutableListOf<TypeEntry>()

        for (line in lines.drop(1)) {
            val sectionMatch = SECTION_HEADER.find(line.trim())
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1]
                continue
            }
            if (NONE_FOUND.containsMatchIn(line)) continue

            val typeMatch = TYPE_ENTRY.find(line) ?: continue
            val entry = TypeEntry(
                kind = typeMatch.groupValues[1],
                name = typeMatch.groupValues[2],
                location = typeMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }
            )

            when (currentSection) {
                "Supertypes" -> supertypes.add(entry)
                "Subtypes", "Implementations" -> subtypes.add(entry)
            }
        }

        if (supertypes.isEmpty() && subtypes.isEmpty()) return null

        val sb = StringBuilder("<div class='hierarchy-result'>")

        if (supertypes.isNotEmpty()) {
            sb.append("<div class='hierarchy-section'>")
            sb.append("<div class='outline-header'><span style='font-weight:600'>Supertypes</span>")
            sb.append("<span class='inspection-file-count'>${supertypes.size}</span></div>")
            sb.append("<div class='hierarchy-entries'>")
            for (t in supertypes) appendEntry(sb, t)
            sb.append("</div></div>")
        }

        // Center node (the queried type)
        sb.append("<div class='hierarchy-center'>")
        val kindBadge = kindClass(typeKind)
        sb.append("<span class='hierarchy-kind $kindBadge'>${typeKind[0].uppercaseChar()}</span>")
        sb.append("<span class='hierarchy-name'>${esc(shortName)}</span>")
        sb.append("</div>")

        if (subtypes.isNotEmpty()) {
            sb.append("<div class='hierarchy-section'>")
            sb.append("<div class='outline-header'><span style='font-weight:600'>Subtypes</span>")
            sb.append("<span class='inspection-file-count'>${subtypes.size}</span></div>")
            sb.append("<div class='hierarchy-entries'>")
            for (t in subtypes) appendEntry(sb, t)
            sb.append("</div></div>")
        }

        sb.append("</div>")
        return sb.toString()
    }

    private data class TypeEntry(val kind: String, val name: String, val location: String?)

    private fun appendEntry(sb: StringBuilder, entry: TypeEntry) {
        val shortName = entry.name.substringAfterLast('.')
        val kindBadge = kindClass(entry.kind)
        sb.append("<div class='hierarchy-entry'>")
        sb.append("<span class='hierarchy-kind $kindBadge'>${entry.kind[0].uppercaseChar()}</span>")
        sb.append("<span class='hierarchy-type-name'>${esc(shortName)}</span>")
        if (entry.location != null) {
            val fileName = entry.location.substringAfterLast('/')
            sb.append("<span class='hierarchy-location'>${esc(fileName)}</span>")
        }
        sb.append("</div>")
    }

    private fun kindClass(kind: String): String = when (kind.lowercase()) {
        "class" -> "kind-class"
        "interface" -> "kind-interface"
        "enum" -> "kind-enum"
        "annotation" -> "kind-annotation"
        else -> "kind-class"
    }
}
