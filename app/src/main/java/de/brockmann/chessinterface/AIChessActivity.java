package de.brockmann.chessinterface;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Chess activity against the Stockfish engine.
 */
public class AIChessActivity extends ChessActivity {

    private StockfishClient engine;
    private int aiStrength;
    private final char aiColor = 'B';

    @Override
    protected int getContentLayoutId() {
        return R.layout.chess_ai_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aiStrength = getIntent().getIntExtra(MenuAIActivity.EXTRA_AI_STRENGTH, 800);
        engine = new StockfishClient();
        if (engine.start()) {
            engine.setElo(aiStrength);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.stop();
    }

    @Override
    protected void switchPlayer() {
        toggleCurrentPlayer();
        if (currentPlayerTurn == aiColor) {
            makeAIMove();
        }
    }

    private void makeAIMove() {
        new Thread(() -> {
            String fen = getFEN();
            String best = engine.getBestMove(fen, 1000);
            if (best == null || best.length() < 4) return;
            int from = algebraicToIndex(best.substring(0, 2));
            int to = algebraicToIndex(best.substring(2, 4));
            runOnUiThread(() -> {
                FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(from);
                if (cell == null || cell.getChildCount() == 0) return;
                ImageView piece = (ImageView) cell.getChildAt(0);
                performMove(from, to, piece);
            });
        }).start();
    }

    private int algebraicToIndex(String sq) {
        int file = sq.charAt(0) - 'a';
        int rank = sq.charAt(1) - '1';
        int row = 7 - rank;
        return row * 8 + file;
    }
}
