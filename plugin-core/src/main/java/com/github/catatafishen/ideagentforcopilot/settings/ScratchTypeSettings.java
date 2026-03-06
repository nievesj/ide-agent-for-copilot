package com.github.catatafishen.ideagentforcopilot.settings;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-level settings for extra language alias→extension mappings.
 * <p>
 * The primary resolution uses IntelliJ's registered {@link Language} registry
 * (matching by ID and display name). These custom mappings serve as overrides
 * for aliases the Language API cannot resolve on its own (e.g. "bash" → "sh",
 * "golang" → "go", "c++" → "cpp").
 */
@Service(Service.Level.APP)
@State(name = "ScratchTypeSettings", storages = @Storage("ideAgentScratchTypes.xml"))
public final class ScratchTypeSettings implements PersistentStateComponent<ScratchTypeSettings.State> {

    private State myState = new State();

    public static ScratchTypeSettings getInstance() {
        return ApplicationManager.getApplication().getService(ScratchTypeSettings.class);
    }

    public Map<String, String> getMappings() {
        return myState.mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        myState.mappings = new LinkedHashMap<>(mappings);
    }

    /**
     * Resolves a language label (e.g. from a markdown code block) to a file
     * extension. Tries the IntelliJ Language registry first, then falls back
     * to custom alias mappings, and finally treats the input itself as an
     * extension.
     */
    public String resolve(String language) {
        if (language == null || language.isEmpty()) return "txt";

        String lower = language.toLowerCase();

        // 1. Try IntelliJ's Language registry (by ID, then by display name)
        String ext = resolveViaLanguageRegistry(lower);
        if (ext != null) return ext;

        // 2. Try custom alias overrides
        String mapped = myState.mappings.get(lower);
        if (mapped != null) return mapped;

        // 3. Fall back to using the input as the extension
        return lower;
    }

    /**
     * Searches IntelliJ's registered languages for one matching the given
     * label (case-insensitive). Returns the default file extension, or null
     * if no match is found.
     */
    static String resolveViaLanguageRegistry(String label) {
        for (Language lang : Language.getRegisteredLanguages()) {
            if (lang.getID().equalsIgnoreCase(label)
                || lang.getDisplayName().equalsIgnoreCase(label)) {
                LanguageFileType ft = lang.getAssociatedFileType();
                if (ft != null && !ft.getDefaultExtension().isEmpty()) {
                    return ft.getDefaultExtension();
                }
            }
        }
        return null;
    }

    @Override
    public @NotNull State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public static class State {
        public Map<String, String> mappings = getDefaults();
    }

    /**
     * Only aliases that the Language registry cannot resolve on its own.
     * Common code-fence labels that don't match any Language ID or display name.
     */
    public static Map<String, String> getDefaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bash", "sh");
        m.put("zsh", "sh");
        m.put("shell", "sh");
        m.put("golang", "go");
        m.put("c++", "cpp");
        m.put("yml", "yaml");
        m.put("kts", "kts");
        m.put("jsx", "jsx");
        m.put("tsx", "tsx");
        m.put("mjs", "js");
        m.put("mts", "ts");
        return m;
    }
}
