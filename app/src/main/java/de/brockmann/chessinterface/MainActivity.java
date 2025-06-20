package de.brockmann.chessinterface;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends MenuActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // wire buttons to their targets
        findViewById(R.id.btn_new_game)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, MenuAIActivity.class)));

        findViewById(R.id.btn_options)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, MenuLocalActivity.class)));

        findViewById(R.id.btn_analysis)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, AnalysisChessActivity.class)));

        findViewById(R.id.btn_settings)
                .setOnClickListener(v ->
                        startActivity(new Intent(this, SettingsMenuActivity.class)));
    }
}
