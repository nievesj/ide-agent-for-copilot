package com.github.catatafishen.ideagentforcopilot.settings;

import com.github.catatafishen.ideagentforcopilot.services.AgentProfile;
import com.github.catatafishen.ideagentforcopilot.services.McpInjectionMethod;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Reusable form panel encapsulating binary discovery, ACP args, MCP injection,
 * agent-selector, pre-launch, permissions, and feature-flag fields for ACP profiles.
 *
 * <p>Shared by the built-in ACP client configurables and generic user-created profiles
 * to avoid code duplication.</p>
 */
final class AcpProfileForm {

    // ── Section flags ─────────────────────────────────────────────────────────

    static final int SECTION_BINARY = 1;
    static final int SECTION_ACP_ARGS = 1 << 1;
    static final int SECTION_MCP = 1 << 2;
    static final int SECTION_PRE_LAUNCH = 1 << 3;
    static final int SECTION_PERMISSIONS = 1 << 4;
    static final int SECTION_FLAGS = 1 << 5;
    static final int SECTION_AGENT_DIR = 1 << 6;

    // ── Form fields ───────────────────────────────────────────────────────────

    JBTextField binaryNameField;
    JBTextField alternateNamesField;
    JBTextField customBinaryPathField;
    JBTextField acpArgsField;
    ComboBox<McpInjectionMethod> mcpMethodCombo;
    JBTextArea mcpConfigTemplateArea;
    JBTextField mcpEnvVarNameField;
    JBTextField prependInstructionsToField;
    JBTextField bundledAgentFilesField;
    JBTextField agentsDirectoryField;
    JBCheckBox usePluginPermissionsCb;
    JBCheckBox excludeAgentBuiltInToolsCb;
    JBCheckBox supportsModelFlagCb;
    JBCheckBox supportsConfigDirCb;
    JBCheckBox supportsMcpConfigFlagCb;
    JBCheckBox requiresResourceDuplicationCb;
    JBTextField modelUsageFieldField;
    JBTextField toolNameRegexField;
    JBTextField toolNameReplacementField;

    private final int sections;

    AcpProfileForm(int sections) {
        this.sections = sections;
        binaryNameField = new JBTextField();
        binaryNameField.getEmptyText().setText("e.g. copilot");
        alternateNamesField = new JBTextField();
        alternateNamesField.getEmptyText().setText("Comma-separated, e.g. gh, github-copilot");
        customBinaryPathField = new JBTextField();
        customBinaryPathField.getEmptyText().setText("Absolute path (overrides auto-discovery)");
        acpArgsField = new JBTextField();
        acpArgsField.getEmptyText().setText("e.g. --acp --stdio");
        mcpMethodCombo = new ComboBox<>(McpInjectionMethod.values());
        mcpConfigTemplateArea = new JBTextArea(6, 40);
        mcpConfigTemplateArea.setFont(JBUI.Fonts.create(Font.MONOSPACED, mcpConfigTemplateArea.getFont().getSize()));
        mcpConfigTemplateArea.setLineWrap(true);
        mcpConfigTemplateArea.setWrapStyleWord(false);
        mcpEnvVarNameField = new JBTextField();
        mcpEnvVarNameField.getEmptyText().setText("e.g. COPILOT_MCP_CONFIG");
        prependInstructionsToField = new JBTextField();
        prependInstructionsToField.getEmptyText().setText("e.g. .copilot/copilot-instructions.md");
        bundledAgentFilesField = new JBTextField();
        bundledAgentFilesField.getEmptyText().setText("e.g. ide-explore.md,ide-task.md");
        agentsDirectoryField = new JBTextField();
        agentsDirectoryField.getEmptyText().setText("Relative path, e.g. .agents");
        usePluginPermissionsCb = new JBCheckBox("Use plugin-level tool permissions");
        excludeAgentBuiltInToolsCb = new JBCheckBox("Exclude agent's built-in tools at session start");
        supportsModelFlagCb = new JBCheckBox("Supports --model flag");
        supportsConfigDirCb = new JBCheckBox("Supports --config-dir flag");
        supportsMcpConfigFlagCb = new JBCheckBox("Supports --additional-mcp-config flag");
        requiresResourceDuplicationCb = new JBCheckBox("Requires resource content duplication");
        modelUsageFieldField = new JBTextField();
        modelUsageFieldField.getEmptyText().setText("e.g. copilotUsage");
        toolNameRegexField = new JBTextField();
        toolNameRegexField.getEmptyText().setText("Regex to match tool name (e.g. ^agentbridge-(.*)$)");
        toolNameReplacementField = new JBTextField();
        toolNameReplacementField.getEmptyText().setText("Replacement string (e.g. $1)");
    }

    /**
     * Builds and returns the form panel according to the enabled sections.
     */
    @NotNull
    JPanel buildPanel() {
        FormBuilder b = FormBuilder.createFormBuilder();
        if (has(SECTION_BINARY)) {
            b.addComponent(new TitledSeparator("Binary Discovery"))
                .addLabeledComponent("Binary name:", binaryNameField)
                .addTooltip("Primary executable name (e.g., \"copilot\", \"opencode\")")
                .addLabeledComponent("Alternate names (comma-separated):", alternateNamesField)
                .addTooltip("Fallback binary names if the primary is not found")
                .addLabeledComponent("Custom binary path:", customBinaryPathField)
                .addTooltip("Override auto-discovery with an absolute path");
        }
        if (has(SECTION_ACP_ARGS)) {
            b.addComponent(new TitledSeparator("ACP Command"))
                .addLabeledComponent("ACP args (space-separated):", acpArgsField)
                .addTooltip("Arguments to activate ACP mode (e.g., \"--acp --stdio\")");
        }
        if (has(SECTION_MCP)) {
            b.addComponent(new TitledSeparator("MCP Injection"))
                .addLabeledComponent("MCP injection method:", mcpMethodCombo)
                .addTooltip("How to tell the agent about the IDE's MCP server")
                .addLabeledComponent("MCP config template:", new JBScrollPane(mcpConfigTemplateArea))
                .addTooltip("JSON template. Placeholders: {mcpPort}, {mcpJarPath}, {javaPath}")
                .addLabeledComponent("Env var name (for ENV_VAR method):", mcpEnvVarNameField);
        }
        if (has(SECTION_PRE_LAUNCH)) {
            b.addComponent(new TitledSeparator("Pre-Launch Hooks"))
                .addLabeledComponent("Prepend instructions to (relative path):", prependInstructionsToField)
                .addTooltip("Relative path from project root to prepend plugin context to on launch. Leave empty to skip.")
                .addLabeledComponent("Bundled agent files (comma-separated):", bundledAgentFilesField)
                .addTooltip("Agent .md filenames to deploy on launch (e.g. ide-explore.md,ide-task.md).");
        }
        if (has(SECTION_AGENT_DIR)) {
            b.addComponent(new TitledSeparator("Agent Selector"))
                .addLabeledComponent("Agents directory (relative path):", agentsDirectoryField)
                .addTooltip("Relative path to agent .md files. Leave empty for no @agent selector.");
        }
        if (has(SECTION_PERMISSIONS)) {
            b.addComponent(new TitledSeparator("Permissions"))
                .addComponent(usePluginPermissionsCb)
                .addTooltip("Route tool calls through the plugin's allow/ask/deny permission system.")
                .addComponent(excludeAgentBuiltInToolsCb)
                .addTooltip("Remove the agent's built-in tools at session start (honoured by OpenCode, ignored by Copilot).");
        }
        if (has(SECTION_FLAGS)) {
            b.addComponent(new TitledSeparator("Feature Flags"))
                .addComponent(supportsModelFlagCb)
                .addComponent(supportsConfigDirCb)
                .addComponent(supportsMcpConfigFlagCb)
                .addComponent(requiresResourceDuplicationCb)
                .addLabeledComponent("Model usage field:", modelUsageFieldField)
                .addTooltip("JSON field name in model metadata for usage info (e.g., \"copilotUsage\")")
                .addLabeledComponent("Tool name regex mapper:", toolNameRegexField)
                .addTooltip("Regex to transform tool names before they reach the UI (applied using replaceAll)")
                .addLabeledComponent("Tool name replacement:", toolNameReplacementField)
                .addTooltip("Replacement string for the tool name regex");
        }
        return b.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    /**
     * Loads profile values into all form fields.
     */
    void load(@NotNull AgentProfile p) {
        if (has(SECTION_BINARY)) {
            binaryNameField.setText(p.getBinaryName());
            alternateNamesField.setText(String.join(", ", p.getAlternateNames()));
            customBinaryPathField.setText(p.getCustomBinaryPath());
        }
        if (has(SECTION_ACP_ARGS)) acpArgsField.setText(String.join(" ", p.getAcpArgs()));
        if (has(SECTION_MCP)) {
            mcpMethodCombo.setSelectedItem(p.getMcpMethod());
            mcpConfigTemplateArea.setText(p.getMcpConfigTemplate());
            mcpConfigTemplateArea.setCaretPosition(0);
            mcpEnvVarNameField.setText(p.getMcpEnvVarName());
        }
        if (has(SECTION_PRE_LAUNCH)) {
            prependInstructionsToField.setText(p.getPrependInstructionsTo() != null ? p.getPrependInstructionsTo() : "");
            bundledAgentFilesField.setText(String.join(",", p.getBundledAgentFiles()));
        }
        if (has(SECTION_AGENT_DIR))
            agentsDirectoryField.setText(p.getAgentsDirectory() != null ? p.getAgentsDirectory() : "");
        if (has(SECTION_PERMISSIONS)) {
            usePluginPermissionsCb.setSelected(p.isUsePluginPermissions());
            excludeAgentBuiltInToolsCb.setSelected(p.isExcludeAgentBuiltInTools());
        }
        if (has(SECTION_FLAGS)) {
            supportsModelFlagCb.setSelected(p.isSupportsModelFlag());
            supportsConfigDirCb.setSelected(p.isSupportsConfigDir());
            supportsMcpConfigFlagCb.setSelected(p.isSupportsMcpConfigFlag());
            requiresResourceDuplicationCb.setSelected(p.isRequiresResourceDuplication());
            modelUsageFieldField.setText(p.getModelUsageField() != null ? p.getModelUsageField() : "");
            toolNameRegexField.setText(p.getToolNameRegex() != null ? p.getToolNameRegex() : "");
            toolNameReplacementField.setText(p.getToolNameReplacement() != null ? p.getToolNameReplacement() : "");
        }
    }

    /**
     * Writes all form field values back into {@code target}.
     */
    void save(@NotNull AgentProfile target) {
        if (has(SECTION_BINARY)) saveBinary(target);
        if (has(SECTION_ACP_ARGS)) target.setAcpArgs(splitSpace(acpArgsField.getText()));
        if (has(SECTION_MCP)) saveMcp(target);
        if (has(SECTION_PRE_LAUNCH)) savePreLaunch(target);
        if (has(SECTION_AGENT_DIR)) saveAgentDir(target);
        if (has(SECTION_PERMISSIONS)) savePermissions(target);
        if (has(SECTION_FLAGS)) saveFlags(target);
    }

    private void saveBinary(@NotNull AgentProfile t) {
        t.setBinaryName(binaryNameField.getText().trim());
        t.setAlternateNames(splitComma(alternateNamesField.getText()));
        t.setCustomBinaryPath(customBinaryPathField.getText().trim());
    }

    private void saveMcp(@NotNull AgentProfile t) {
        McpInjectionMethod method = (McpInjectionMethod) mcpMethodCombo.getSelectedItem();
        if (method != null) t.setMcpMethod(method);
        t.setMcpConfigTemplate(mcpConfigTemplateArea.getText().trim());
        t.setMcpEnvVarName(mcpEnvVarNameField.getText().trim());
    }

    private void savePreLaunch(@NotNull AgentProfile t) {
        String prepend = prependInstructionsToField.getText().trim();
        t.setPrependInstructionsTo(prepend.isEmpty() ? null : prepend);
        String bundled = bundledAgentFilesField.getText().trim();
        t.setBundledAgentFiles(bundled.isEmpty() ? List.of() :
            Arrays.stream(bundled.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private void saveAgentDir(@NotNull AgentProfile t) {
        String dir = agentsDirectoryField.getText().trim();
        t.setAgentsDirectory(dir.isEmpty() ? null : dir);
    }

    private void savePermissions(@NotNull AgentProfile t) {
        t.setUsePluginPermissions(usePluginPermissionsCb.isSelected());
        t.setExcludeAgentBuiltInTools(excludeAgentBuiltInToolsCb.isSelected());
    }

    private void saveFlags(@NotNull AgentProfile t) {
        t.setSupportsModelFlag(supportsModelFlagCb.isSelected());
        t.setSupportsConfigDir(supportsConfigDirCb.isSelected());
        t.setSupportsMcpConfigFlag(supportsMcpConfigFlagCb.isSelected());
        t.setRequiresResourceDuplication(requiresResourceDuplicationCb.isSelected());
        String muf = modelUsageFieldField.getText().trim();
        t.setModelUsageField(muf.isEmpty() ? null : muf);
        String tnr = toolNameRegexField.getText().trim();
        t.setToolNameRegex(tnr.isEmpty() ? null : tnr);
        String tnrp = toolNameReplacementField.getText().trim();
        t.setToolNameReplacement(tnrp.isEmpty() ? null : tnrp);
    }

    /**
     * Returns true if any form field differs from the persisted profile.
     */
    boolean isModified(@NotNull AgentProfile p) {
        return (has(SECTION_BINARY) && isBinaryModified(p))
            || (has(SECTION_ACP_ARGS) && !splitSpace(acpArgsField.getText()).equals(p.getAcpArgs()))
            || (has(SECTION_MCP) && isMcpModified(p))
            || (has(SECTION_PRE_LAUNCH) && isPreLaunchModified(p))
            || (has(SECTION_AGENT_DIR) && !agentDir().equals(nullToEmpty(p.getAgentsDirectory())))
            || (has(SECTION_PERMISSIONS) && isPermissionsModified(p))
            || (has(SECTION_FLAGS) && isFlagsModified(p));
    }

    private boolean isBinaryModified(@NotNull AgentProfile p) {
        return !binaryNameField.getText().trim().equals(p.getBinaryName())
            || !splitComma(alternateNamesField.getText()).equals(p.getAlternateNames())
            || !customBinaryPathField.getText().trim().equals(p.getCustomBinaryPath());
    }

    private boolean isMcpModified(@NotNull AgentProfile p) {
        return mcpMethodCombo.getSelectedItem() != p.getMcpMethod()
            || !mcpConfigTemplateArea.getText().trim().equals(p.getMcpConfigTemplate())
            || !mcpEnvVarNameField.getText().trim().equals(p.getMcpEnvVarName());
    }

    private boolean isPreLaunchModified(@NotNull AgentProfile p) {
        String prepend = prependInstructionsToField.getText().trim();
        String bundled = bundledAgentFilesField.getText().trim();
        List<String> bundledList = bundled.isEmpty() ? List.of() :
            Arrays.stream(bundled.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return !prepend.equals(nullToEmpty(p.getPrependInstructionsTo()))
            || !bundledList.equals(p.getBundledAgentFiles());
    }

    private boolean isPermissionsModified(@NotNull AgentProfile p) {
        return usePluginPermissionsCb.isSelected() != p.isUsePluginPermissions()
            || excludeAgentBuiltInToolsCb.isSelected() != p.isExcludeAgentBuiltInTools();
    }

    private boolean isFlagsModified(@NotNull AgentProfile p) {
        String muf = modelUsageFieldField.getText().trim();
        return supportsModelFlagCb.isSelected() != p.isSupportsModelFlag()
            || supportsConfigDirCb.isSelected() != p.isSupportsConfigDir()
            || supportsMcpConfigFlagCb.isSelected() != p.isSupportsMcpConfigFlag()
            || requiresResourceDuplicationCb.isSelected() != p.isRequiresResourceDuplication()
            || !Objects.equals(muf.isEmpty() ? null : muf, p.getModelUsageField())
            || !Objects.equals(toolNameRegexField.getText().trim().isEmpty() ? null : toolNameRegexField.getText().trim(), p.getToolNameRegex())
            || !Objects.equals(toolNameReplacementField.getText().trim().isEmpty() ? null : toolNameReplacementField.getText().trim(), p.getToolNameReplacement());
    }

    private String agentDir() {
        return agentsDirectoryField.getText().trim();
    }

    private boolean has(int flag) {
        return (sections & flag) != 0;
    }

    @NotNull
    static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    @NotNull
    static List<String> splitComma(@NotNull String s) {
        List<String> result = new ArrayList<>();
        for (String part : s.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    @NotNull
    static List<String> splitSpace(@NotNull String s) {
        List<String> result = new ArrayList<>();
        for (String part : s.split("\\s+")) {
            String t = part.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }
}
