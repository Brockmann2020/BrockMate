package de.brockmann.chessinterface;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ChessApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
