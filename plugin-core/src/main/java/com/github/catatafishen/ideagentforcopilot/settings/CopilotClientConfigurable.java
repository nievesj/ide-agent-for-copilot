package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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
    private BillingConfigurable billingConfigurable;

    @Override
    public @NotNull JComponent createComponent() {
        form = new AcpProfileForm(SECTIONS);
        JBScrollPane configScroll = new JBScrollPane(form.buildPanel());
        configScroll.setBorder(null);

        billingConfigurable = new BillingConfigurable();
        JComponent billingPanel = billingConfigurable.createComponent();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Configuration", configScroll);
        tabs.addTab("Billing Data", billingPanel);

        reset();
        return tabs;
    }

    @Override
    public boolean isModified() {
        if (form == null) return false;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
        return (p != null && form.isModified(p))
            || (billingConfigurable != null && billingConfigurable.isModified());
    }

    @Override
    public void apply() {
        if (form != null) {
            AgentProfileManager mgr = AgentProfileManager.getInstance();
            AgentProfile p = mgr.getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
            if (p != null) {
                form.save(p);
                mgr.updateProfile(p);
            }
        }
        if (billingConfigurable != null) billingConfigurable.apply();
    }

    @Override
    public void reset() {
        if (form != null) {
            AgentProfile p = AgentProfileManager.getInstance().getProfile(AgentProfileManager.COPILOT_PROFILE_ID);
            if (p != null) form.load(p);
        }
        if (billingConfigurable != null) billingConfigurable.reset();
    }

    @Override
    public void disposeUIResources() {
        form = null;
        if (billingConfigurable != null) {
            billingConfigurable.disposeUIResources();
            billingConfigurable = null;
        }
    }
}
