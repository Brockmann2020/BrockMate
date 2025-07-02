package de.brockmann.chessinterface;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class AnalysisChessActivity extends ChessActivity {

    private StockfishClient engine;
    private BestMoveArrowView arrowView;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = 0;

    @Override
    protected int getContentLayoutId() {
        return R.layout.chess_analysis_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        engine = new StockfishClient();
        engine.start();
        arrowView = findViewById(R.id.best_move_arrow);

        Button prev = findViewById(R.id.btn_prev_move);
        Button next = findViewById(R.id.btn_next_move);
        prev.setOnClickListener(v -> gotoPrevious());
        next.setOnClickListener(v -> gotoNext());

        chessBoardGrid.post(() -> {
            history.clear();
            history.add(getFEN());
            historyIndex = 0;
            updateBestMove();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.stop();
    }

    @Override
    protected void switchPlayer() {
        toggleCurrentPlayer();
        // discard forward history if new move played
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        history.add(getFEN());
        historyIndex = history.size() - 1;
        updateBestMove();
    }

    private void gotoPrevious() {
        if (historyIndex > 0) {
            historyIndex--;
            loadFromFEN(history.get(historyIndex));
            updateBestMove();
        }
    }

    private void gotoNext() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            loadFromFEN(history.get(historyIndex));
            updateBestMove();
        }
    }

    private void updateBestMove() {
        if (engine == null) return;
        new Thread(() -> {
            String fen = getFEN();
            String best = engine.getBestMove(fen, 10);
            if (best == null || best.length() < 4) {
                runOnUiThread(() -> arrowView.clearArrow());
                return;
            }
            int from = algebraicToIndex(best.substring(0, 2));
            int to = algebraicToIndex(best.substring(2, 4));
            runOnUiThread(() -> showArrow(from, to));
        }).start();
    }

    private void showArrow(int from, int to) {
        FrameLayout fromCell = (FrameLayout) chessBoardGrid.getChildAt(from);
        FrameLayout toCell = (FrameLayout) chessBoardGrid.getChildAt(to);
        if (fromCell == null || toCell == null) {
            arrowView.clearArrow();
            return;
        }
        int[] boardPos = new int[2];
        arrowView.getLocationOnScreen(boardPos);
        int[] fromPos = new int[2];
        int[] toPos = new int[2];
        fromCell.getLocationOnScreen(fromPos);
        toCell.getLocationOnScreen(toPos);
        float sx = fromPos[0] - boardPos[0] + fromCell.getWidth() / 2f;
        float sy = fromPos[1] - boardPos[1] + fromCell.getHeight() / 2f;
        float ex = toPos[0] - boardPos[0] + toCell.getWidth() / 2f;
        float ey = toPos[1] - boardPos[1] + toCell.getHeight() / 2f;
        arrowView.setArrow(sx, sy, ex, ey);
    }

    private int algebraicToIndex(String sq) {
        int file = sq.charAt(0) - 'a';
        int rank = sq.charAt(1) - '1';
        int row = 7 - rank;
        return row * 8 + file;
    }
}
