package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.bridge.TransportType;
import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.AgentProfileManager;
import com.github.catatafishen.ideagentforcopilot.services.AgentUiSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientAgentsGroupConfigurable implements Configurable, Configurable.Composite {

    private final Project project;

    // Agent behaviour settings
    private JSpinner timeoutSpinner;
    private JSpinner maxToolCallsSpinner;

    // Custom client creation
    private JBTextField newClientNameField;
    private JPanel panel;

    public ClientAgentsGroupConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Agents";
    }

    @Override
    public @Nullable JComponent createComponent() {
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(300, 30, 3600, 10));
        maxToolCallsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 500, 1));

        Dimension spinnerSize = JBUI.size(100, timeoutSpinner.getPreferredSize().height);
        timeoutSpinner.setMaximumSize(spinnerSize);
        maxToolCallsSpinner.setMaximumSize(spinnerSize);

        newClientNameField = new JBTextField();
        newClientNameField.setColumns(24);
        newClientNameField.getEmptyText().setText("Name for your custom agent");

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> addGenericClient());

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.add(newClientNameField, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Prompt timeout (seconds):", timeoutSpinner)
            .addTooltip("Time before an inactive agent session is considered timed out (30–3600).")
            .addLabeledComponent("Max tool calls per turn:", maxToolCallsSpinner)
            .addTooltip("Limit how many tools the agent can call in a single turn. 0 = unlimited.")
            .addSeparator(12)
            .addLabeledComponent("Add custom ACP client:", addRow)
            .addTooltip("Creates a custom ACP-compatible agent. Reopen Settings to configure it.")
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        if ((int) timeoutSpinner.getValue() != settings.getPromptTimeout()) return true;
        return (int) maxToolCallsSpinner.getValue() != settings.getMaxToolCallsPerTurn();
    }

    @Override
    public void apply() {
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        settings.setPromptTimeout((int) timeoutSpinner.getValue());
        settings.setMaxToolCallsPerTurn((int) maxToolCallsSpinner.getValue());
    }

    @Override
    public void reset() {
        if (timeoutSpinner == null) return;
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        timeoutSpinner.setValue(settings.getPromptTimeout());
        maxToolCallsSpinner.setValue(settings.getMaxToolCallsPerTurn());
    }

    @Override
    public void disposeUIResources() {
        timeoutSpinner = null;
        maxToolCallsSpinner = null;
        newClientNameField = null;
        panel = null;
    }

    /**
     * Returns one {@link GenericAcpClientConfigurable} per user-added generic ACP profile.
     * Built-in client pages are registered statically in plugin.xml.
     */
    @Override
    public Configurable @NotNull [] getConfigurables() {
        List<Configurable> children = new ArrayList<>();
        for (AgentProfile p : AgentProfileManager.getInstance().getAllProfiles()) {
            if (!p.isBuiltIn()) {
                children.add(new GenericAcpClientConfigurable(project, p.getId()));
            }
        }
        return children.toArray(new Configurable[0]);
    }

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
