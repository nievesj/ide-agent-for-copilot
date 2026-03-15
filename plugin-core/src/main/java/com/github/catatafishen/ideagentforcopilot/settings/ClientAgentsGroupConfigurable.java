package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.bridge.TransportType;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root settings group for all AI agent clients.
 * Registered in plugin.xml under the main plugin settings node.
 * <p>
 * Built-in clients ({@link CopilotClientConfigurable}, {@link OpenCodeClientConfigurable},
 * {@link AnthropicDirectClientConfigurable}, {@link ClaudeCliClientConfigurable}) are always
 * present. User-added generic ACP profiles are listed after them as
 * {@link GenericAcpClientConfigurable} children.
 * <p>
 * The group page itself contains an "Add Generic ACP Client" form so users can create
 * new custom profiles without navigating elsewhere.
 */
public final class ClientAgentsGroupConfigurable implements Configurable, Configurable.Composite {

    private final Project project;

    public ClientAgentsGroupConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Client Agents";
    }

    // ── Group overview page ───────────────────────────────────────────────────

    private JBTextField newClientNameField;
    private JPanel panel;

    @Override
    public @Nullable JComponent createComponent() {
        newClientNameField = new JBTextField();
        newClientNameField.getEmptyText().setText("My Custom Agent");

        JButton addBtn = new JButton("Add Generic ACP Client");
        addBtn.addActionListener(e -> addGenericClient());

        panel = FormBuilder.createFormBuilder()
            .addComponent(new JLabel(
                "<html>Select a client below to configure it, or add a custom ACP client here.</html>"))
            .addSeparator(8)
            .addLabeledComponent("New client name:", newClientNameField)
            .addComponent(addBtn)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        return panel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
        // Nothing to apply on the group page — the name field is consumed immediately on Add.
    }

    @Override
    public void disposeUIResources() {
        newClientNameField = null;
        panel = null;
    }

    // ── Dynamic children ─────────────────────────────────────────────────────

    /**
     * Called each time the Settings dialog opens. Returns:
     * <ol>
     *   <li>{@link AgentSettingsConfigurable} — timeout/tool call limits</li>
     *   <li>Built-in clients: Copilot, OpenCode, Claude Code, Claude CLI</li>
     *   <li>{@link ToolPermissionsConfigurable} — tool allow/ask/deny</li>
     *   <li>One {@link GenericAcpClientConfigurable} per user-added profile</li>
     * </ol>
     */
    @Override
    public Configurable @NotNull [] getConfigurables() {
        List<Configurable> children = new ArrayList<>();

        children.add(new AgentSettingsConfigurable(project));
        children.add(new CopilotClientConfigurable(project));
        children.add(new OpenCodeClientConfigurable(project));
        children.add(new AnthropicDirectClientConfigurable(project));
        children.add(new ClaudeCliClientConfigurable(project));
        children.add(new ToolPermissionsConfigurable(project));

        for (AgentProfile p : AgentProfileManager.getInstance().getAllProfiles()) {
            if (!p.isBuiltIn()) {
                children.add(new GenericAcpClientConfigurable(project, p.getId()));
            }
        }

        return children.toArray(new Configurable[0]);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addGenericClient() {
        String name = newClientNameField != null ? newClientNameField.getText().trim() : "";
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel,
                "Please enter a name for the new client.",
                "Name required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AgentProfile p = new AgentProfile();
        p.setId(UUID.randomUUID().toString());
        p.setDisplayName(name);
        p.setBuiltIn(false);
        p.setTransportType(TransportType.ACP);
        AgentProfileManager.getInstance().addProfile(p);

        if (newClientNameField != null) newClientNameField.setText("");
        JOptionPane.showMessageDialog(panel,
            "\"" + name + "\" added. Reopen Settings to configure it.",
            "Client added", JOptionPane.INFORMATION_MESSAGE);
    }
}
