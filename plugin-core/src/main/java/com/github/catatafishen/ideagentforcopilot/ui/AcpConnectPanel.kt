package com.github.catatafishen.ideagentforcopilot.ui

import com.github.catatafishen.ideagentforcopilot.psi.PsiBridgeService
import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager
import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager.AgentType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

/**
 * Pre-connection landing panel shown before ACP is established.
 * Displays PSI bridge (MCP tool server) status, agent selector dropdown,
 * optional custom command input, a Connect button, and an auto-connect checkbox.
 */
class AcpConnectPanel(
    private val project: Project,
    private val onConnect: (AgentType, String?) -> Unit
) : JBPanel<AcpConnectPanel>(BorderLayout()) {

    private val agentManager = ActiveAgentManager.getInstance(project)
    private val agentCombo: JComboBox<AgentType>
    private val customCommandField = JBTextField()
    private val customCommandLabel = JBLabel("Start command:")
    private val connectButton = JButton("Connect")
    private val autoConnectCheckbox = JCheckBox("Auto-connect on startup")
    private val mcpStatusLabel = JBLabel()
    private val statusBanner = StatusBanner(project)

    init {
        border = JBUI.Borders.empty(20, 40)

        val centerBox = JBPanel<JBPanel<*>>()
        centerBox.layout = BoxLayout(centerBox, BoxLayout.Y_AXIS)
        centerBox.isOpaque = false

        // Title
        val titleLabel = JBLabel("Connect to an AI Agent")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        centerBox.add(titleLabel)
        centerBox.add(Box.createVerticalStrut(12))

        // MCP tool server status
        mcpStatusLabel.alignmentX = Component.LEFT_ALIGNMENT
        mcpStatusLabel.font = mcpStatusLabel.font.deriveFont(11f)
        centerBox.add(mcpStatusLabel)
        centerBox.add(Box.createVerticalStrut(16))

        // Agent selector
        val agentLabel = JBLabel("Agent:")
        agentLabel.alignmentX = Component.LEFT_ALIGNMENT
        centerBox.add(agentLabel)
        centerBox.add(Box.createVerticalStrut(4))

        agentCombo = JComboBox(AgentType.entries.toTypedArray())
        agentCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = (value as? AgentType)?.displayName() ?: ""
                return this
            }
        }
        agentCombo.selectedItem = agentManager.activeType
        agentCombo.maximumSize = Dimension(300, agentCombo.preferredSize.height)
        agentCombo.alignmentX = Component.LEFT_ALIGNMENT
        agentCombo.addActionListener { updateCustomCommandVisibility() }
        centerBox.add(agentCombo)
        centerBox.add(Box.createVerticalStrut(8))

        // Custom command field (only visible for GENERIC)
        customCommandLabel.alignmentX = Component.LEFT_ALIGNMENT
        centerBox.add(customCommandLabel)
        centerBox.add(Box.createVerticalStrut(4))

        customCommandField.text = agentManager.getCustomAcpCommandFor(agentManager.activeType)
        customCommandField.maximumSize = Dimension(400, customCommandField.preferredSize.height)
        customCommandField.alignmentX = Component.LEFT_ALIGNMENT
        centerBox.add(customCommandField)
        centerBox.add(Box.createVerticalStrut(16))

        // Connect button
        connectButton.alignmentX = Component.LEFT_ALIGNMENT
        connectButton.addActionListener { doConnect() }
        centerBox.add(connectButton)
        centerBox.add(Box.createVerticalStrut(12))

        // Auto-connect checkbox
        autoConnectCheckbox.isSelected = agentManager.isAutoConnect
        autoConnectCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        autoConnectCheckbox.addActionListener {
            agentManager.setAutoConnect(autoConnectCheckbox.isSelected)
        }
        centerBox.add(autoConnectCheckbox)
        centerBox.add(Box.createVerticalStrut(8))

        // Status banner for connect errors
        statusBanner.alignmentX = Component.LEFT_ALIGNMENT
        centerBox.add(statusBanner)

        // Wrap in a vertically centered layout
        val wrapper = JBPanel<JBPanel<*>>(GridBagLayout())
        wrapper.isOpaque = false
        wrapper.add(centerBox)

        add(wrapper, BorderLayout.CENTER)

        updateCustomCommandVisibility()
        refreshMcpStatus()

        // Subscribe to PSI bridge status changes so the label updates when the server starts
        project.messageBus.connect().subscribe(
            PsiBridgeService.STATUS_TOPIC,
            PsiBridgeService.StatusListener { port ->
                SwingUtilities.invokeLater { updateMcpLabel(port) }
            })
    }

    private fun updateCustomCommandVisibility() {
        val selectedType = agentCombo.selectedItem as? AgentType ?: return
        customCommandLabel.isVisible = true
        customCommandField.isVisible = true

        if (!customCommandField.hasFocus()) {
            val stored = agentManager.getCustomAcpCommandFor(selectedType)
            customCommandField.text = stored
        }

        customCommandField.emptyText.text = if (selectedType == AgentType.GENERIC) {
            "e.g., my-agent --acp --stdio"
        } else {
            selectedType.defaultStartCommand()
        }
    }

    private fun doConnect() {
        val selectedType = agentCombo.selectedItem as AgentType
        val cmd = customCommandField.text.trim()

        if (cmd.isEmpty()) {
            statusBanner.showError("Enter a start command for the agent.")
            return
        }

        agentManager.setCustomAcpCommandFor(selectedType, cmd)

        val customCommand = if (selectedType == AgentType.GENERIC || cmd != selectedType.defaultStartCommand()) {
            cmd
        } else {
            null
        }

        statusBanner.dismissCurrent()
        connectButton.isEnabled = false
        connectButton.text = "Connecting..."
        onConnect(selectedType, customCommand)
    }

    fun showError(message: String) {
        SwingUtilities.invokeLater {
            connectButton.isEnabled = true
            connectButton.text = "Connect"
            statusBanner.showError(message)
        }
    }

    /** Resets the connect button to its default state (enabled, "Connect" label). */
    fun resetConnectButton() {
        SwingUtilities.invokeLater {
            connectButton.isEnabled = true
            connectButton.text = "Connect"
        }
    }

    fun refreshMcpStatus() {
        val bridge = PsiBridgeService.getInstance(project)
        val port = bridge.port
        updateMcpLabel(port)
    }

    private fun updateMcpLabel(port: Int) {
        if (port > 0) {
            mcpStatusLabel.text = "MCP Tool Server: running on port $port"
            mcpStatusLabel.foreground = UIUtil.getLabelInfoForeground()
        } else {
            mcpStatusLabel.text = "MCP Tool Server: starting..."
            mcpStatusLabel.foreground = JBColor.GRAY
        }
    }
}
