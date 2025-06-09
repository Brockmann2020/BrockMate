package de.brockmann.chessinterface;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

import java.util.Locale;

public class LocalChessActivity extends ChessActivity {

    /*private ChronometerView clockWhite;
    private ChronometerView clockBlack;*/
    private boolean whiteToMove = true;

    @Override
    protected int getContentLayoutId() {
        return R.layout.chess_local_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    private CountDownTimer createTimer(long millis, TextView tgt) {
        return new CountDownTimer(millis, 1000) {
            @Override public void onTick(long ms) {
                long m = (ms/1000) / 60;
                long s = (ms/1000) % 60;
                tgt.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
            }
            @Override public void onFinish() {
                tgt.setText("00:00");
            }
        };
    }
}
