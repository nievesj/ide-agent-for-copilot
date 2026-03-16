package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.bridge.AnthropicKeyStore;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Settings page for the Claude Code (direct API) client.
 * Shows only the Anthropic API key — no binary or ACP settings needed.
 */
public final class AnthropicDirectClientConfigurable implements Configurable {

    @SuppressWarnings("unused")
    public AnthropicDirectClientConfigurable(@NotNull Project ignoredProject) {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Claude Code";
    }

    private JBLabel statusLabel;
    private JPasswordField apiKeyField;
    private JPanel panel;

    @Override
    public @NotNull JComponent createComponent() {
        statusLabel = new JBLabel();

        apiKeyField = new JPasswordField();
        apiKeyField.setEchoChar('•');

        HyperlinkLabel keyLink = new HyperlinkLabel("Get your API key at console.anthropic.com/settings/keys");
        keyLink.setHyperlinkTarget("https://console.anthropic.com/settings/keys");

        JBLabel keyNote = new JBLabel(
            "<html>Enter your Anthropic API key (starts with <code>sk-ant-...</code>). "
                + "Stored securely in the IDE credential store.</html>");
        keyNote.setForeground(UIUtil.getContextHelpForeground());
        keyNote.setFont(JBUI.Fonts.smallFont());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Status:", statusLabel)
            .addComponent(keyNote, 2)
            .addComponent(keyLink, 2)
            .addSeparator(8)
            .addLabeledComponent("Anthropic API key:", apiKeyField)
            .addTooltip("Your Anthropic API key (sk-ant-...). Stored securely in the IDE credential store.")
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        if (apiKeyField == null) return false;
        String current = new String(apiKeyField.getPassword()).trim();
        String stored = storedKey();
        return !current.equals(stored);
    }

    @Override
    public void apply() {
        if (apiKeyField == null) return;
        String key = new String(apiKeyField.getPassword()).trim();
        AnthropicKeyStore.setApiKey(AgentProfileManager.CLAUDE_CODE_PROFILE_ID, key.isEmpty() ? null : key);
        refreshStatus();
    }

    @Override
    public void reset() {
        if (apiKeyField == null) return;
        apiKeyField.setText(storedKey());
        refreshStatus();
    }

    @Override
    public void disposeUIResources() {
        statusLabel = null;
        apiKeyField = null;
        panel = null;
    }

    private void refreshStatus() {
        if (statusLabel == null) return;
        String key = storedKey();
        if (!key.isEmpty()) {
            statusLabel.setText("✓ API key configured");
            statusLabel.setForeground(new Color(0, 128, 0));
        } else {
            statusLabel.setText("No API key configured");
            statusLabel.setForeground(Color.RED);
        }
    }

    @NotNull
    private String storedKey() {
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.CLAUDE_CODE_PROFILE_ID);
        if (p == null) return "";
        String key = AnthropicKeyStore.getApiKey(p.getId());
        return key != null ? key : "";
    }
}
