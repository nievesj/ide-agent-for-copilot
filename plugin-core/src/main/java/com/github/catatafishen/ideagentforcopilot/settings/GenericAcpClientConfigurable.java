package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Settings page for a user-added generic ACP client profile.
 * Shows an editable display name plus the full ACP profile form.
 * Includes a "Delete this client" button that marks the profile for removal.
 */
public final class GenericAcpClientConfigurable implements Configurable {

    private static final int SECTIONS =
        AcpProfileForm.SECTION_BINARY | AcpProfileForm.SECTION_ACP_ARGS
            | AcpProfileForm.SECTION_MCP | AcpProfileForm.SECTION_PRE_LAUNCH
            | AcpProfileForm.SECTION_AGENT_DIR | AcpProfileForm.SECTION_PERMISSIONS
            | AcpProfileForm.SECTION_FLAGS;

    private final String profileId;
    private volatile boolean deleted = false;

    // Project parameter required by IntelliJ's projectConfigurable extension point contract.
    public GenericAcpClientConfigurable(@SuppressWarnings("unused") @NotNull Project ignoredProject,
                                        @NotNull String profileId) {
        this.profileId = profileId;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        AgentProfile p = AgentProfileManager.getInstance().getProfile(profileId);
        return p != null ? p.getDisplayName() : profileId;
    }

    private JBTextField displayNameField;
    private AcpProfileForm form;
    private JPanel panel;

    @Override
    public @NotNull JComponent createComponent() {
        displayNameField = new JBTextField();
        form = new AcpProfileForm(SECTIONS);
        JPanel formPanel = form.buildPanel();

        JButton deleteBtn = new JButton("Delete this client");
        deleteBtn.setForeground(java.awt.Color.RED);
        deleteBtn.addActionListener(e -> deleteProfile());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Name:", displayNameField)
            .addSeparator(4)
            .addComponent(formPanel)
            .addSeparator(8)
            .addComponent(deleteBtn)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        reset();
        JBScrollPane scroll = new JBScrollPane(panel);
        scroll.setBorder(null);
        return scroll;
    }

    @Override
    public boolean isModified() {
        if (form == null || deleted) return false;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(profileId);
        if (p == null) return false;
        boolean nameChanged = !displayNameField.getText().trim().equals(p.getDisplayName());
        return nameChanged || form.isModified(p);
    }

    @Override
    public void apply() {
        if (form == null || deleted) return;
        AgentProfileManager mgr = AgentProfileManager.getInstance();
        AgentProfile p = mgr.getProfile(profileId);
        if (p == null) return;
        String name = displayNameField.getText().trim();
        if (!name.isEmpty()) p.setDisplayName(name);
        form.save(p);
        mgr.updateProfile(p);
    }

    @Override
    public void reset() {
        if (form == null) return;
        AgentProfile p = AgentProfileManager.getInstance().getProfile(profileId);
        if (p == null) return;
        displayNameField.setText(p.getDisplayName());
        form.load(p);
    }

    @Override
    public void disposeUIResources() {
        displayNameField = null;
        form = null;
        panel = null;
    }

    private void deleteProfile() {
        AgentProfileManager.getInstance().removeProfile(profileId);
        deleted = true;
        if (panel != null) {
            panel.setEnabled(false);
            javax.swing.JOptionPane.showMessageDialog(
                panel,
                "Profile deleted. Reopen Settings to update the sidebar.",
                "Client deleted",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}
