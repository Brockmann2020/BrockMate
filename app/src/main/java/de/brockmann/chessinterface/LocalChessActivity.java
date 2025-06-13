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

        // 1. TimeControl-String holen
        String tc = getIntent().getStringExtra(EXTRA_TIME_CONTROL);
        long initialMillis = parseTimeControlToMillis(tc);

        // 2. Referenzen zu den beiden Clock-Includes
        View topInclude    = findViewById(R.id.clock_top);
        View bottomInclude = findViewById(R.id.clock_bottom);

        // 3. TextViews innerhalb der Inkludes (ID aus view_player_clock.xml)
        TextView tvTop    = topInclude   .findViewById(R.id.tv_clock_time);
        TextView tvBottom = bottomInclude.findViewById(R.id.tv_clock_time);

        // 4. Starttext setzen bzw. "keine Zeitkontrolle"
        if (initialMillis < 0) {
            tvTop.setText("-:--");
            tvBottom.setText("-:--");
        } else {
            String fmt = formatMillis(initialMillis);
            tvTop.setText(fmt);
            tvBottom.setText(fmt);

            // 5. Timer starten (wenn nötig)
            createTimer(initialMillis, tvTop).start();
            createTimer(initialMillis, tvBottom).start();
        }
    }

    // Converts a time control like "M" or "M|I"/"M+I" to milliseconds
    private long parseTimeControlToMillis(String tc) {
        if ("Keine Zeitkontrolle".equals(tc)) {
            return -1L;
        }

        String[] parts = tc.split("[+|]");
        try {
            int minutes = Integer.parseInt(parts[0]);
            return minutes * 60L * 1000;
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

    @Override
    protected void switchPlayer() {
        toggleCurrentPlayer();

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
