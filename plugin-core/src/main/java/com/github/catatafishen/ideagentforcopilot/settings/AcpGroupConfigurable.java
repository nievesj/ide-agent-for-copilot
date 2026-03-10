package com.github.catatafishen.ideagentforcopilot.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Group node: Settings → Tools → IDE Agent for Copilot → ACP.
 * Contains child pages for Agent Settings, Agent Profiles, and Tool Permissions.
 */
public final class AcpGroupConfigurable implements Configurable {

    public static final String ID = "com.github.catatafishen.ideagentforcopilot.acp";

    @SuppressWarnings("unused") // injected by the platform via projectConfigurable
    private final Project project;

    public AcpGroupConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "ACP";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel(
                "<html>"
                    + "<b>Agent Communication Protocol (ACP)</b><br><br>"
                    + "Configure how the IDE communicates with Copilot-compatible agents.<br><br>"
                    + "Use the sub-pages to manage:<br>"
                    + "&#8226; <b>Agent Settings</b> \u2014 timeout and tool-call limits<br>"
                    + "&#8226; <b>Agent Profiles</b> \u2014 agent binaries and MCP injection<br>"
                    + "&#8226; <b>Tool Permissions</b> \u2014 allow, ask, or deny per tool"
                    + "</html>"))
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
        // group page — no settings
    }
}
