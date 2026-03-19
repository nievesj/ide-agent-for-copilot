package com.github.catatafishen.ideagentforcopilot.ui

import com.github.catatafishen.ideagentforcopilot.bridge.*
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object AgentIconProvider {
    private val defaultIcon: Icon = loadIcon("agentbridge.svg")
    private val claudeIcon: Icon = loadIcon("claude.svg")
    private val copilotIcon: Icon = loadIcon("copilot.svg")
    private val opencodeIcon: Icon = loadIcon("opencode.svg")
    private val junieIcon: Icon = loadIcon("junie.svg")
    private val kiroIcon: Icon = loadIcon("kiro.svg")

    fun getDefaultIcon(): Icon = defaultIcon

    fun getIconForProfile(profileId: String?): Icon {
        val icon = when (profileId) {
            AnthropicDirectClient.PROFILE_ID, ClaudeCliClient.PROFILE_ID -> claudeIcon
            CopilotAcpClient.PROFILE_ID -> copilotIcon
            OpenCodeAcpClient.PROFILE_ID -> opencodeIcon
            JunieAcpClient.PROFILE_ID -> junieIcon
            KiroAcpClient.PROFILE_ID -> kiroIcon
            else -> null
        }
        return icon ?: defaultIcon
    }

    private fun loadIcon(filename: String): Icon {
        return IconLoader.getIcon("/icons/$filename", AgentIconProvider::class.java)
    }
}
