package de.brockmann.chessinterface;

import android.os.Bundle;

// AIGameActivity inherits all behavior from ChessActivity
public class AIChessActivity extends ChessActivity {


    @Override
    protected int getContentLayoutId() {
        // no additional UI elements for now
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // nothing else for now – same UI & logic as ChessActivity
    }
}