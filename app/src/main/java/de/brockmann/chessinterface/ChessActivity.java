package de.brockmann.chessinterface;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ChessActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 8;
    private GridLayout chessBoardGrid;

    // Standard FEN-ähnliche Startposition
    private final String[] initialBoardSetup = {
            "r","n","b","q","k","b","n","r",
            "p","p","p","p","p","p","p","p",
            " "," "," "," "," "," "," "," ",
            " "," "," "," "," "," "," "," ",
            " "," "," "," "," "," "," "," ",
            " "," "," "," "," "," "," "," ",
            "P","P","P","P","P","P","P","P",
            "R","N","B","Q","K","B","N","R"
    };

    @LayoutRes
    protected abstract int getContentLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chess_activity);

        chessBoardGrid = findViewById(R.id.chess_board_grid);

        // UI buttons
        ImageView menuButton = findViewById(R.id.iv_menu_button);
        Button drawButton  = findViewById(R.id.btn_offer_draw);
        Button resignButton= findViewById(R.id.btn_resign_game);

        View.OnClickListener dummyListener = v -> {
            String txt = (v instanceof Button) ? ((Button) v).getText().toString() : "Menu";
            Toast.makeText(this, txt + " clicked (no action yet)", Toast.LENGTH_SHORT).show();

            if (v.getId() == R.id.iv_menu_button) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        };

        menuButton.setOnClickListener(dummyListener);
        drawButton .setOnClickListener(dummyListener);
        resignButton.setOnClickListener(dummyListener);

        // Build board when the GridLayout is measured
        chessBoardGrid.post(this::initializeBoard);
    }

    private void initializeBoard() {
        setupBoardCells();
        placePiecesOnBoard();
    }

    private void setupBoardCells() {
        int cellSize = chessBoardGrid.getWidth() / BOARD_SIZE; // square board → width == height

        chessBoardGrid.removeAllViews();
        chessBoardGrid.setColumnCount(BOARD_SIZE);
        chessBoardGrid.setRowCount(BOARD_SIZE);

        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            FrameLayout cell = new FrameLayout(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = cellSize;
            lp.height = cellSize;
            lp.columnSpec = GridLayout.spec(i % BOARD_SIZE);
            lp.rowSpec    = GridLayout.spec(i / BOARD_SIZE);
            cell.setLayoutParams(lp);

            View bg = new View(this);
            bg.setBackgroundColor(getCellColor(i));
            cell.addView(bg, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            chessBoardGrid.addView(cell);
        }
    }

    private void placePiecesOnBoard() {
        for (int i = 0; i < initialBoardSetup.length; i++) {
            String code = initialBoardSetup[i];
            if (" ".equals(code)) continue;

            ImageView piece = new ImageView(this);
            int resId = getDrawableIdForPiece(code);
            if (resId == 0) continue;

            piece.setImageResource(resId);
            piece.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
            if (cell != null) {
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);

                int pad = (int) (4 * getResources().getDisplayMetrics().density); // 4 dp
                piece.setPadding(pad, pad, pad, pad);

                cell.addView(piece, p);
            }
        }
    }

    private int getCellColor(int index) {
        int row = index / BOARD_SIZE;
        int col = index % BOARD_SIZE;
        return ((row + col) % 2 == 0) ? Color.parseColor("#FFF8DC")  // light
                : Color.parseColor("#8B4513"); // dark
    }

    private int getDrawableIdForPiece(String code) {
        switch (code) {
            case "p": return R.drawable.ic_pawn_black;
            case "r": return R.drawable.ic_rook_black;
            case "n": return R.drawable.ic_knight_black;
            case "b": return R.drawable.ic_bishop_black;
            case "q": return R.drawable.ic_queen_black;
            case "k": return R.drawable.ic_king_black;
            case "P": return R.drawable.ic_pawn_white;
            case "R": return R.drawable.ic_rook_white;
            case "N": return R.drawable.ic_knight_white;
            case "B": return R.drawable.ic_bishop_white;
            case "Q": return R.drawable.ic_queen_white;
            case "K": return R.drawable.ic_king_white;
            default:  return 0;
        }
    }
}
