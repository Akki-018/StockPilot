package com.stockpilot.util;

import javafx.scene.Scene;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Centralized manager for application themes (Light Industrial and Dark Industrial).
 * Persists the user preference using the Java Preferences API and applies stylesheets dynamically.
 */
public final class ThemeManager {

    public enum Theme {
        LIGHT("Light Industrial", "/styles/theme-light.css"),
        DARK("Dark Industrial", "/styles/theme-dark.css");

        private final String displayName;
        private final String stylesheetPath;

        Theme(String displayName, String stylesheetPath) {
            this.displayName = displayName;
            this.stylesheetPath = stylesheetPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getStylesheetPath() {
            return stylesheetPath;
        }
    }

    private static final String PREF_KEY_THEME = "stockpilot_active_theme";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final List<Consumer<Theme>> LISTENERS = new ArrayList<>();

    private static Theme currentTheme = loadSavedTheme();
    private static Scene boundScene;

    private ThemeManager() {
        // Utility class; prevent instantiation.
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    /**
     * Binds the application Scene to this ThemeManager and applies the active stylesheets.
     */
    public static void bindScene(Scene scene) {
        boundScene = scene;
        applyTheme(currentTheme);
    }

    /**
     * Toggles between Light Industrial and Dark Industrial themes.
     */
    public static void toggleTheme() {
        setTheme(currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    /**
     * Sets the active theme, persists the choice, and updates the bound scene and listeners.
     */
    public static void setTheme(Theme theme) {
        if (theme == null) {
            theme = Theme.LIGHT;
        }
        currentTheme = theme;
        try {
            PREFERENCES.put(PREF_KEY_THEME, theme.name());
        } catch (Exception ignored) {
            // Non-critical fallback if environment restricts user preferences registry
        }
        applyTheme(theme);
        notifyListeners(theme);
    }

    /**
     * Registers a listener to be notified immediately when the theme changes.
     */
    public static void addThemeListener(Consumer<Theme> listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    private static void notifyListeners(Theme theme) {
        for (Consumer<Theme> listener : LISTENERS) {
            try {
                listener.accept(theme);
            } catch (Exception ignored) {
                // Ensure other listeners still execute safely
            }
        }
    }

    private static void applyTheme(Theme theme) {
        if (boundScene == null) {
            return;
        }

        URL baseUrl = ThemeManager.class.getResource("/styles/base.css");
        URL themeUrl = ThemeManager.class.getResource(theme.getStylesheetPath());

        boundScene.getStylesheets().clear();
        if (baseUrl != null) {
            boundScene.getStylesheets().add(baseUrl.toExternalForm());
        }
        if (themeUrl != null) {
            boundScene.getStylesheets().add(themeUrl.toExternalForm());
        }
    }

    private static Theme loadSavedTheme() {
        try {
            String saved = PREFERENCES.get(PREF_KEY_THEME, Theme.LIGHT.name());
            return Theme.valueOf(saved);
        } catch (Exception e) {
            return Theme.LIGHT;
        }
    }
}
