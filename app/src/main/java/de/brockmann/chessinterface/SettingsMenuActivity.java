package de.brockmann.chessinterface;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsMenuActivity extends MenuActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.menu_settings_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SwitchCompat darkSwitch = findViewById(R.id.switch_dark_mode);
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("dark_mode", false);
        darkSwitch.setChecked(enabled);
        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }
}
