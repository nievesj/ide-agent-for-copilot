package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.bridge.AnthropicKeyStore;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

    private JPasswordField apiKeyField;
    private JPanel panel;

    @Override
    public @NotNull JComponent createComponent() {
        apiKeyField = new JPasswordField();
        apiKeyField.setEchoChar('•');
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Anthropic API key:", apiKeyField)
            .addTooltip("Your Anthropic API key (sk-ant-...). Get one at console.anthropic.com/settings/keys.")
            .addTooltip("Stored securely in the IDE credential store.")
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
    }

    @Override
    public void reset() {
        if (apiKeyField == null) return;
        apiKeyField.setText(storedKey());
    }

    @Override
    public void disposeUIResources() {
        apiKeyField = null;
        panel = null;
    }

    @NotNull
    private String storedKey() {
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.CLAUDE_CODE_PROFILE_ID);
        if (p == null) return "";
        String key = AnthropicKeyStore.getApiKey(p.getId());
        return key != null ? key : "";
    }
}
