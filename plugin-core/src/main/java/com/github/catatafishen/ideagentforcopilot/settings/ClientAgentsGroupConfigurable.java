package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.ActiveAgentManager;
import com.github.catatafishen.ideagentforcopilot.services.AgentUiSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public final class ClientAgentsGroupConfigurable implements Configurable, Configurable.Composite {

    private final Project project;

    private JSpinner turnTimeoutSpinner;
    private JSpinner inactivityTimeoutSpinner;
    private JSpinner maxToolCallsSpinner;
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
        turnTimeoutSpinner = new JSpinner(new SpinnerNumberModel(300, 30, 3600, 10));
        inactivityTimeoutSpinner = new JSpinner(new SpinnerNumberModel(300, 30, 3600, 10));
        maxToolCallsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 500, 1));

        Dimension spinnerSize = JBUI.size(100, turnTimeoutSpinner.getPreferredSize().height);
        turnTimeoutSpinner.setMaximumSize(spinnerSize);
        inactivityTimeoutSpinner.setMaximumSize(spinnerSize);
        maxToolCallsSpinner.setMaximumSize(spinnerSize);

        JBLabel introLabel = new JBLabel(
            "<html>Global behavior settings for all agent sessions. "
                + "Configure individual agent clients in the sub-pages below.</html>");
        introLabel.setForeground(UIUtil.getContextHelpForeground());

        panel = FormBuilder.createFormBuilder()
            .addComponent(introLabel)
            .addSeparator(8)
            .addLabeledComponent("Turn timeout (seconds):", turnTimeoutSpinner)
            .addTooltip("Maximum wall-clock time allowed for a turn (30–3600).")
            .addLabeledComponent("Inactivity timeout (seconds):", inactivityTimeoutSpinner)
            .addTooltip("Maximum silence before a turn is considered stalled (30–3600).")
            .addLabeledComponent("Max tool calls per turn:", maxToolCallsSpinner)
            .addTooltip("Limit how many tools the agent can call in a single turn. 0 = unlimited.")
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        if ((int) turnTimeoutSpinner.getValue() != settings.getTurnTimeout()) return true;
        if ((int) inactivityTimeoutSpinner.getValue() != settings.getInactivityTimeout()) return true;
        return (int) maxToolCallsSpinner.getValue() != settings.getMaxToolCallsPerTurn();
    }

    @Override
    public void apply() {
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        settings.setTurnTimeout((int) turnTimeoutSpinner.getValue());
        settings.setInactivityTimeout((int) inactivityTimeoutSpinner.getValue());
        settings.setMaxToolCallsPerTurn((int) maxToolCallsSpinner.getValue());
    }

    @Override
    public void reset() {
        if (turnTimeoutSpinner == null) return;
        AgentUiSettings settings = ActiveAgentManager.getInstance(project).getSettings();
        turnTimeoutSpinner.setValue(settings.getTurnTimeout());
        inactivityTimeoutSpinner.setValue(settings.getInactivityTimeout());
        maxToolCallsSpinner.setValue(settings.getMaxToolCallsPerTurn());
    }

    @Override
    public void disposeUIResources() {
        turnTimeoutSpinner = null;
        inactivityTimeoutSpinner = null;
        maxToolCallsSpinner = null;
        panel = null;
    }

    /**
     * Built-in agent pages are registered statically in plugin.xml — no dynamic children needed.
     */
    @Override
    public Configurable @NotNull [] getConfigurables() {
        return new Configurable[0];
    }
}
