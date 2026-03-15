package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Settings page for the GitHub Copilot client (ACP transport).
 * Shows binary discovery, ACP args, MCP injection, pre-launch hooks,
 * agent selector, permissions, and feature flags.
 */
public final class CopilotClientConfigurable implements Configurable {

    private static final int SECTIONS =
        AcpProfileForm.SECTION_BINARY | AcpProfileForm.SECTION_ACP_ARGS
            | AcpProfileForm.SECTION_MCP | AcpProfileForm.SECTION_PRE_LAUNCH
            | AcpProfileForm.SECTION_AGENT_DIR | AcpProfileForm.SECTION_PERMISSIONS
            | AcpProfileForm.SECTION_FLAGS;

    @SuppressWarnings("unused")
    public CopilotClientConfigurable(@NotNull Project ignoredProject) {
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "GitHub Copilot";
    }

    private AcpProfileForm form;

    @Override
    public @NotNull JComponent createComponent() {
        form = new AcpProfileForm(SECTIONS);
        JPanel panel = form.buildPanel();
        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(null);
        reset();
        return scroll;
    }

    @Override
    public boolean isModified() {
        if (form == null) return false;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
        return p != null && form.isModified(p);
    }

    @Override
    public void apply() {
        if (form == null) return;
        AgentProfileManager mgr = AgentProfileManager.getInstance();
        AgentProfile p = mgr.getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
        if (p == null) return;
        form.save(p);
        mgr.updateProfile(p);
    }

    @Override
    public void reset() {
        if (form == null) return;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
        if (p != null) form.load(p);
    }

    @Override
    public void disposeUIResources() {
        form = null;
    }
}
