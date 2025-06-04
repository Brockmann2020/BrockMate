package de.brockmann.chessinterface;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MenuAIActivity extends MenuActivity {

    public static final String EXTRA_AI_STRENGTH =
            "de.brockmann.chessinterface.EXTRA_AI_STRENGTH";

    @Override
    protected int getContentLayoutId() {
        return R.layout.menu_ai_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SeekBar seekBar = findViewById(R.id.seekBar_strength);
        TextView valueView = findViewById(R.id.tv_strength_value);
        Button startBtn = findViewById(R.id.btn_start_ai);

        // update label when slider moves
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int strength = 800 + progress;
                valueView.setText("Spielstärke: " + strength);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // launch AI game with chosen strength
        startBtn.setOnClickListener(v -> {
            int strength = 800 + seekBar.getProgress();
            Intent intent = new Intent(this, AIChessActivity.class);
            intent.putExtra(EXTRA_AI_STRENGTH, strength);
            startActivity(intent);
        });
    }
}
