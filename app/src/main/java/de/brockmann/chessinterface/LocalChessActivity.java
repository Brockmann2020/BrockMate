package de.brockmann.chessinterface;

import static de.brockmann.chessinterface.MenuLocalActivity.EXTRA_TIME_CONTROL;

import android.os.Bundle;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.FrameLayout;

import java.util.Locale;

public class LocalChessActivity extends ChessActivity {

    /*private ChronometerView clockWhite;
    private ChronometerView clockBlack;*/

    private String localPlayAction; // "flip_board" or "flip_pieces"
    private boolean isBoardFlipped = false;

    // --- clock state ---
    private CountDownTimer whiteTimer;
    private CountDownTimer blackTimer;
    private long whiteRemaining;
    private long blackRemaining;
    private int incrementMillis;
    private TextView tvTop;
    private TextView tvBottom;
    
    private void onTimeExpired(boolean whiteExpired) {
        if (whiteTimer != null) { whiteTimer.cancel(); }
        if (blackTimer != null) { blackTimer.cancel(); }
        whiteTimer = null;
        blackTimer = null;
        if (whiteExpired) {
            showGameEndOverlay("Black won on time");
        } else {
            showGameEndOverlay("White won on time");
        }
    }

    @Override
    protected int getContentLayoutId() {
        return R.layout.chess_local_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // read how the board should be flipped
        localPlayAction = getIntent().getStringExtra(MenuLocalActivity.EXTRA_LOCAL_ACTION);
        isBoardFlipped = false;

        // 1. TimeControl-String holen und parsen
        String tc = getIntent().getStringExtra(EXTRA_TIME_CONTROL);
        long initialMillis = parseTimeControl(tc);

        // 2. Referenzen zu den beiden Clock-Includes
        View topInclude    = findViewById(R.id.clock_top);
        View bottomInclude = findViewById(R.id.clock_bottom);

        // 3. TextViews innerhalb der Inkludes (ID aus view_player_clock.xml)
        tvTop    = topInclude   .findViewById(R.id.tv_clock_time);
        tvBottom = bottomInclude.findViewById(R.id.tv_clock_time);

        // 4. Starttext setzen bzw. "keine Zeitkontrolle"
        if (initialMillis < 0) {
            tvTop.setText("-:--");
            tvBottom.setText("-:--");
        } else {
            whiteRemaining = initialMillis;
            blackRemaining = initialMillis;
            String fmt = formatMillis(initialMillis);
            tvTop.setText(fmt);
            tvBottom.setText(fmt);

            // 5. Timer starten (nur weiß beginnt)
            startWhiteTimer();
        }
    }

    // Converts a time control like "M" or "M|I"/"M+I" and stores increment
    private long parseTimeControl(String tc) {
        incrementMillis = 0;
        if ("Keine Zeitkontrolle".equals(tc)) {
            return -1L;
        }

        String[] parts = tc.split("[+|]");
        try {
            int minutes = Integer.parseInt(parts[0]);
            if (parts.length > 1) {
                int inc = Integer.parseInt(parts[1]);
                incrementMillis = inc * 1000;
            }
            return minutes * 60L * 1000L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    // Formats ms to "MM:SS"
    private String formatMillis(long ms) {
        long m = (ms/1000) / 60;
        long s = (ms/1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void startWhiteTimer() {
        if (whiteTimer != null) whiteTimer.cancel();
        whiteTimer = new CountDownTimer(whiteRemaining, 1000) {
            @Override public void onTick(long ms) {
                whiteRemaining = ms;
                tvBottom.setText(formatMillis(ms));
            }
            @Override public void onFinish() {
                whiteRemaining = 0;
                tvBottom.setText("00:00");
                onTimeExpired(true);
            }
        }.start();
    }

    private void startBlackTimer() {
        if (blackTimer != null) blackTimer.cancel();
        blackTimer = new CountDownTimer(blackRemaining, 1000) {
            @Override public void onTick(long ms) {
                blackRemaining = ms;
                tvTop.setText(formatMillis(ms));
            }
            @Override public void onFinish() {
                blackRemaining = 0;
                tvTop.setText("00:00");
                onTimeExpired(false);
            }
        }.start();
    }

    @Override
    protected void switchPlayer() {
        if (whiteTimer != null || blackTimer != null) {
            char moved = currentPlayerTurn;
            if (moved == 'W') {
                if (whiteTimer != null) whiteTimer.cancel();
                whiteRemaining += incrementMillis;
            } else {
                if (blackTimer != null) blackTimer.cancel();
                blackRemaining += incrementMillis;
            }
            toggleCurrentPlayer();
            if (currentPlayerTurn == 'W') {
                startWhiteTimer();
            } else {
                startBlackTimer();
            }
        } else {
            toggleCurrentPlayer();
        }

        if (localPlayAction != null) {
            isBoardFlipped = !isBoardFlipped;
            applyBoardOrientation();
        }
    }

    private void applyBoardOrientation() {
        if (localPlayAction == null) return;

        chessBoardGrid.postDelayed(() -> {
            if ("flip_board".equals(localPlayAction)) {
                float rotation = isBoardFlipped ? 180f : 0f;
                chessBoardGrid.setRotation(rotation);
                for (int i = 0; i < chessBoardGrid.getChildCount(); i++) {
                    FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
                    if (cell.getChildCount() > 0) {
                        cell.getChildAt(0).setRotation(rotation);
                    }
                }
            } else if ("flip_pieces".equals(localPlayAction)) {
                for (int i = 0; i < chessBoardGrid.getChildCount(); i++) {
                    FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
                    if (cell.getChildCount() > 0) {
                        ImageView piece = (ImageView) cell.getChildAt(0);
                        piece.setScaleY(isBoardFlipped ? -1f : 1f);
                    }
                }
            }
        }, 200);
    }
}
