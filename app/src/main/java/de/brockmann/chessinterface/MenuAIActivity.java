package de.brockmann.chessinterface;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MenuAIActivity extends MenuActivity {

    public static final String EXTRA_AI_STRENGTH =
            "de.brockmann.chessinterface.EXTRA_AI_STRENGTH";
    public static final String EXTRA_AI_COLOR =
            "de.brockmann.chessinterface.EXTRA_AI_COLOR";

    @Override
    protected int getContentLayoutId() {
        return R.layout.menu_ai_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SeekBar seekBar = findViewById(R.id.seekBar_strength);
        TextView valueView = findViewById(R.id.tv_strength_value);
        Button whiteBtn = findViewById(R.id.btn_start_ai_white);
        Button blackBtn = findViewById(R.id.btn_start_ai_black);

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

        whiteBtn.setOnClickListener(v -> startGame('W', seekBar.getProgress()));
        blackBtn.setOnClickListener(v -> startGame('B', seekBar.getProgress()));
    }

    private void startGame(char playerColor, int progress) {
        int strength = 800 + progress;
        Intent intent = new Intent(this, AIChessActivity.class);
        intent.putExtra(EXTRA_AI_STRENGTH, strength);
        char aiColor = playerColor == 'W' ? 'B' : 'W';
        intent.putExtra(EXTRA_AI_COLOR, aiColor);
        startActivity(intent);
    }
}
