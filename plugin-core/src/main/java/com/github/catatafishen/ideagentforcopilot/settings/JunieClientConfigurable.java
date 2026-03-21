package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.acp.client.AcpClient;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class JunieClientConfigurable implements Configurable {

    private static final String AGENT_ID = "junie";

    @SuppressWarnings("unused")
    public JunieClientConfigurable(@NotNull Project ignoredProject) {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Junie";
    }

    private JBLabel statusLabel;
    private JBTextField binaryPathField;
    private JPanel panel;

    @Override
    public @NotNull JComponent createComponent() {
        statusLabel = new JBLabel();

        binaryPathField = new JBTextField();
        binaryPathField.getEmptyText().setText("Auto-detect (leave empty)");
        binaryPathField.setToolTipText("Absolute path to the junie binary. Leave empty to find it on PATH.");

        HyperlinkLabel authLink = new HyperlinkLabel("Manage your Junie account at junie.jetbrains.com/cli");
        authLink.setHyperlinkTarget("https://junie.jetbrains.com/cli");

        JBLabel authNote = new JBLabel(
            "<html>Run <code>junie</code> in a terminal and use the "
                + "<code>/account</code> command to log in with your JetBrains Account "
                + "or a <code>JUNIE_API_KEY</code> token.</html>");
        authNote.setForeground(UIUtil.getContextHelpForeground());
        authNote.setFont(JBUI.Fonts.smallFont());

        JBLabel toolWarning = new JBLabel(
            "<html><b>⚠ Tool Selection Limitation:</b> Junie ignores <code>excludedTools</code> and does not send "
                + "<code>request_permission</code> for any tools. Built-in tools (Edit, View, Bash) may bypass "
                + "IntelliJ's editor buffer. The plugin uses prompt engineering to encourage MCP tool usage, but "
                + "compliance depends on the LLM. See <code>docs/JUNIE-TOOL-WORKAROUND.md</code> for details.</html>");
        toolWarning.setForeground(new Color(184, 134, 11));
        toolWarning.setFont(JBUI.Fonts.smallFont());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Status:", statusLabel)
            .addComponent(authNote, 2)
            .addComponent(authLink, 2)
            .addSeparator(8)
            .addComponent(toolWarning, 2)
            .addSeparator(8)
            .addLabeledComponent("Junie binary:", binaryPathField)
            .addTooltip("Leave empty to auto-detect on PATH.")
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        return panel;
    }

    @Override
    public boolean isModified() {
        if (binaryPathField == null) return false;
        String stored = nullToEmpty(AcpClient.loadCustomBinaryPath(AGENT_ID));
        return !binaryPathField.getText().trim().equals(stored);
    }

    @Override
    public void apply() {
        if (binaryPathField == null) return;
        AcpClient.saveCustomBinaryPath(AGENT_ID, binaryPathField.getText().trim());
    }

    @Override
    public void reset() {
        if (binaryPathField == null) return;
        refreshStatusAsync();
        binaryPathField.setText(nullToEmpty(AcpClient.loadCustomBinaryPath(AGENT_ID)));
    }

    @Override
    public void disposeUIResources() {
        statusLabel = null;
        binaryPathField = null;
        panel = null;
    }

    private void refreshStatusAsync() {
        if (statusLabel == null) return;
        statusLabel.setText("Checking...");
        statusLabel.setForeground(UIUtil.getLabelForeground());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String customPath = AcpClient.loadCustomBinaryPath(AGENT_ID);
            String binary = customPath != null ? customPath : "junie";
            String version = BinaryDetector.detectBinaryVersion(binary, new String[0]);
            SwingUtilities.invokeLater(() -> {
                if (statusLabel == null) return;
                if (version != null) {
                    statusLabel.setText("✓ Junie CLI found — " + version);
                    statusLabel.setForeground(new Color(0, 128, 0));
                } else {
                    statusLabel.setText("Junie CLI not found on PATH — install from junie.jetbrains.com");
                    statusLabel.setForeground(Color.RED);
                }
            });
        });
    }

    @NotNull
    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
