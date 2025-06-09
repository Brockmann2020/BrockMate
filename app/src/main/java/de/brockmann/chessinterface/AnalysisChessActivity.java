package de.brockmann.chessinterface;

import android.os.Bundle;

public class AnalysisChessActivity extends ChessActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.chess_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // nothing else for now – same UI & logic as ChessActivity
    }
}
