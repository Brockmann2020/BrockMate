package de.brockmann.chessinterface;

import android.os.Bundle;

// AIGameActivity inherits all behavior from ChessActivity
public class AIChessActivity extends ChessActivity {


    @Override
    protected int getContentLayoutId() {
        return R.layout.ai_game_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // nothing else for now – same UI & logic as ChessActivity
    }
}