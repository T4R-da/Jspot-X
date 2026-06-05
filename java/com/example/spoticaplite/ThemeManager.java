package com.example.spoticaplite;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Reads the saved theme preference and applies it via AppCompatDelegate.
 * Supports: "dark" (default), "light", "system".
 */
public class ThemeManager {

    private static final String PREF_KEY = "theme";

    public static void applyTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(PREF_KEY, "dark");

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "dark":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    public static void saveTheme(Context context, String theme) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_KEY, theme)
                .apply();
    }
}
