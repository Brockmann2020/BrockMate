package de.brockmann.chessinterface;

import android.os.Bundle;

public class AnalysisChessActivity extends ChessActivity {

    @Override
    protected int getContentLayoutId() {
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // nothing else for now – same UI & logic as ChessActivity
    }
}
