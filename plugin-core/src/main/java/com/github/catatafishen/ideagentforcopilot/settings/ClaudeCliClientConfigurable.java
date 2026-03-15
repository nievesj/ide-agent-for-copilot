package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Settings page for the Claude CLI client.
 * Shows authentication status and the optional custom-models list
 * ({@code model-id=Display Name} one per line).
 */
public final class ClaudeCliClientConfigurable implements Configurable {

    @SuppressWarnings("unused")
    public ClaudeCliClientConfigurable(@NotNull Project ignoredProject) {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Claude CLI";
    }

    private JBLabel authStatusLabel;
    private JBTextArea customModelsArea;
    private JPanel panel;

    @Override
    public @NotNull JComponent createComponent() {
        authStatusLabel = new JBLabel();
        customModelsArea = new JBTextArea(6, 40);
        customModelsArea.setLineWrap(false);
        customModelsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JBScrollPane modelsScroll = new JBScrollPane(customModelsArea);

        panel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("<html><b>Authentication status</b></html>"))
            .addComponent(authStatusLabel)
            .addSeparator(8)
            .addComponent(new JBLabel("<html><b>Custom models</b></html>"))
            .addTooltip("One entry per line: <model-id>=<Display Name>")
            .addTooltip("Leave empty to use the built-in model list.")
            .addComponentFillVertically(modelsScroll, 200)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        if (customModelsArea == null) return false;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.CLAUDE_CLI_PROFILE_ID);
        if (p == null) return false;
        List<String> current = parseModels();
        return !current.equals(p.getCustomCliModels());
    }

    @Override
    public void apply() {
        if (customModelsArea == null) return;
        AgentProfileManager mgr = AgentProfileManager.getInstance();
        AgentProfile p = mgr.getProfile(AgentProfileManager.CLAUDE_CLI_PROFILE_ID);
        if (p == null) return;
        p.setCustomCliModels(parseModels());
        mgr.updateProfile(p);
    }

    @Override
    public void reset() {
        if (customModelsArea == null) return;
        refreshAuthStatus();
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.CLAUDE_CLI_PROFILE_ID);
        if (p == null) return;
        customModelsArea.setText(String.join("\n", p.getCustomCliModels()));
        customModelsArea.setCaretPosition(0);
    }

    @Override
    public void disposeUIResources() {
        authStatusLabel = null;
        customModelsArea = null;
        panel = null;
    }

    private void refreshAuthStatus() {
        if (authStatusLabel == null) return;
        String status = AgentProfileManager.getClaudeCliAuthStatus();
        if (status != null) {
            authStatusLabel.setText(status);
            authStatusLabel.setForeground(new Color(0, 128, 0));
        } else {
            authStatusLabel.setText("✗ Not logged in — run 'claude auth login' in a terminal.");
            authStatusLabel.setForeground(Color.RED);
        }
    }

    @NotNull
    private List<String> parseModels() {
        if (customModelsArea == null) return List.of();
        return Arrays.stream(customModelsArea.getText().split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
