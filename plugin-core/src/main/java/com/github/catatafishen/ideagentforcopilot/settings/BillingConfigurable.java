package com.github.catatafishen.ideagentforcopilot.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class BillingConfigurable implements Configurable {

    public static final String ID = "com.github.catatafishen.ideagentforcopilot.billing";

    private JBCheckBox showCopilotUsageCb;
    private BillingSettings settings;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Billing Data";
    }

    @Override
    public @Nullable JComponent createComponent() {
        settings = BillingSettings.getInstance();

        showCopilotUsageCb = new JBCheckBox("Show Copilot usage graph in toolbar");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(showCopilotUsageCb);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return showCopilotUsageCb.isSelected() != settings.isShowCopilotUsage();
    }

    @Override
    public void apply() {
        settings.setShowCopilotUsage(showCopilotUsageCb.isSelected());
    }

    @Override
    public void reset() {
        showCopilotUsageCb.setSelected(settings.isShowCopilotUsage());
    }
}
