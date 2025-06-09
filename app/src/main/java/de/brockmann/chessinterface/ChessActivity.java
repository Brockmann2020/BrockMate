package de.brockmann.chessinterface;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.DragEvent;
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

    private final String[] initialBoardSetup = {
            "r", "n", "b", "q", "k", "b", "n", "r",
            "p", "p", "p", "p", "p", "p", "p", "p",
            " ", " ", " ", " ", " ", " ", " ", " ",
            " ", " ", " ", " ", " ", " ", " ", " ",
            " ", " ", " ", " ", " ", " ", " ", " ",
            " ", " ", " ", " ", " ", " ", " ", " ",
            "P", "P", "P", "P", "P", "P", "P", "P",
            "R", "N", "B", "Q", "K", "B", "N", "R"
    };

    private String[] currentBoardState;

    // --- NEUE VARIABLEN FÜR SPIELZUSTAND ---
    private char currentPlayerTurn; // 'W' für Weiß, 'B' für Schwarz
    private String localPlayAction; // Speichert "flip_board" oder "flip_pieces"
    private boolean isBoardFlipped = false; // Zustand der Spiegelung

    private int enPassantTarget = -1;

    @LayoutRes
    protected abstract int getContentLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int layoutId = getContentLayoutId();
        if (layoutId == 0) {
            layoutId = R.layout.chess_activity;
        }
        setContentView(layoutId);

        // --- NEU: Spielaktion aus Intent auslesen ---
        Intent intent = getIntent();
        localPlayAction = intent.getStringExtra(MenuLocalActivity.EXTRA_LOCAL_ACTION);

        chessBoardGrid = findViewById(R.id.chess_board_grid);
        setupDummyButtons();

        chessBoardGrid.post(this::initializeBoard);
    }

    private void initializeBoard() {
        currentBoardState = initialBoardSetup.clone();
        // --- NEU: Startspieler festlegen ---
        currentPlayerTurn = 'W';
        isBoardFlipped = false; // Brett startet immer normal
        enPassantTarget = -1;
        Toast.makeText(this, "White's turn", Toast.LENGTH_SHORT).show();

        setupBoardCells();
        placePiecesOnBoard();
    }

    private void setupBoardCells() {
        int cellSize = chessBoardGrid.getWidth() / BOARD_SIZE;
        chessBoardGrid.removeAllViews();
        chessBoardGrid.setColumnCount(BOARD_SIZE);
        chessBoardGrid.setRowCount(BOARD_SIZE);

        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            FrameLayout cell = new FrameLayout(this);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cellSize;
            lp.height = cellSize;
            lp.columnSpec = GridLayout.spec(i % BOARD_SIZE);
            lp.rowSpec = GridLayout.spec(i / BOARD_SIZE);
            cell.setLayoutParams(lp);

            cell.setBackgroundColor(getCellColor(i));
            cell.setOnDragListener(new ChessDragListener());
            cell.setTag(i);
            chessBoardGrid.addView(cell);
        }
    }

    private void placePiecesOnBoard() {
        for (int i = 0; i < currentBoardState.length; i++) {
            String code = currentBoardState[i];
            if (" ".equals(code)) continue;

            ImageView piece = new ImageView(this);
            int resId = getDrawableIdForPiece(code);
            if (resId == 0) continue;

            piece.setImageResource(resId);
            piece.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            piece.setTag(code);

            // --- GEÄNDERT: OnLongClickListener prüft jetzt, ob der Spieler am Zug ist ---
            piece.setOnLongClickListener(v -> {
                FrameLayout parentCell = (FrameLayout) v.getParent();
                int startPosition = (int) parentCell.getTag();
                String pieceCode = currentBoardState[startPosition];

                // Nur das Bewegen erlauben, wenn die Farbe der Figur mit dem aktuellen Spieler übereinstimmt
                if (getPieceColor(pieceCode.charAt(0)) != currentPlayerTurn) {
                    Toast.makeText(ChessActivity.this, "Not your turn!", Toast.LENGTH_SHORT).show();
                    return false; // Drag-Vorgang wird nicht gestartet
                }

                ClipData.Item item = new ClipData.Item(String.valueOf(startPosition));
                ClipData dragData = new ClipData(
                        "piece",
                        new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                        item
                );

                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(dragData, shadowBuilder, v, 0);
                v.setVisibility(View.INVISIBLE);
                return true;
            });

            FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
            if (cell != null) {
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
                int pad = (int) (4 * getResources().getDisplayMetrics().density);
                piece.setPadding(pad, pad, pad, pad);
                cell.addView(piece, p);
            }
        }
    }

    private class ChessDragListener implements View.OnDragListener {
        private final int highlightColor = Color.argb(100, 255, 255, 0);

        @Override
        public boolean onDrag(View v, DragEvent event) {
            FrameLayout targetCell = (FrameLayout) v;
            View draggedView = (View) event.getLocalState();

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                case DragEvent.ACTION_DRAG_ENTERED:
                    targetCell.getBackground().setColorFilter(highlightColor, PorterDuff.Mode.SRC_ATOP);
                    targetCell.invalidate();
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    targetCell.getBackground().clearColorFilter();
                    targetCell.invalidate();
                    return true;
                case DragEvent.ACTION_DROP:
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    int startPosition = Integer.parseInt(item.getText().toString());
                    int endPosition = (int) targetCell.getTag();

                    if (isMoveValid(startPosition, endPosition)) {
                        performMove(startPosition, endPosition, (ImageView) draggedView);
                    }
                    draggedView.setVisibility(View.VISIBLE);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    targetCell.getBackground().clearColorFilter();
                    targetCell.invalidate();
                    if (!event.getResult()) {
                        draggedView.setVisibility(View.VISIBLE);
                    }
                    return true;
                default:
                    break;
            }
            return false;
        }
    }

    // --- NEUE METHODE: Führt den Zug aus und aktualisiert alles ---
    private void performMove(int from, int to, ImageView pieceView) {
        // UI aktualisieren
        FrameLayout sourceCell = (FrameLayout) chessBoardGrid.getChildAt(from);
        FrameLayout targetCell = (FrameLayout) chessBoardGrid.getChildAt(to);

        sourceCell.removeView(pieceView);
        if (targetCell.getChildCount() > 0) {
            targetCell.removeAllViews();
        }
        targetCell.addView(pieceView);

        // Internen Zustand aktualisieren
        String piece = currentBoardState[from];
        currentBoardState[to] = piece;
        currentBoardState[from] = " ";

        // En Passant
        if (piece.equalsIgnoreCase("p") && Math.abs(to - from) == 16) {
            enPassantTarget = to;
        }

        // Spieler wechseln
        switchTurn();
    }

    // --- NEUE METHODE: Wechselt den Spieler und löst ggf. die Spiegelung aus ---
    private void switchTurn() {
        currentPlayerTurn = (currentPlayerTurn == 'W') ? 'B' : 'W';
        Toast.makeText(this, (currentPlayerTurn == 'W' ? "White's" : "Black's") + " turn", Toast.LENGTH_SHORT).show();

        // Nur spiegeln, wenn eine lokale Spielaktion ausgewählt wurde
        if (localPlayAction != null) {
            isBoardFlipped = !isBoardFlipped; // Zustand umkehren
            applyBoardOrientation();
        }
    }

    // --- NEUE METHODE: Wendet die visuelle Spiegelung/Rotation an ---
    private void applyBoardOrientation() {
        if (localPlayAction == null) return;

        // Nach einer kleinen Verzögerung ausführen, damit der Zug erst sichtbar wird
        chessBoardGrid.postDelayed(() -> {
            if ("flip_board".equals(localPlayAction)) {
                float rotation = isBoardFlipped ? 180f : 0f;
                chessBoardGrid.setRotation(rotation);
                // Jede einzelne Figur zurückdrehen, damit sie nicht auf dem Kopf steht
                for (int i = 0; i < chessBoardGrid.getChildCount(); i++) {
                    FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
                    if (cell.getChildCount() > 0) {
                        cell.getChildAt(0).setRotation(rotation);
                    }
                }
            } else if ("flip_pieces".equals(localPlayAction)) {
                // Tauscht die Figuren auf dem Brett, um die Perspektive zu wechseln
                for (int i = 0; i < chessBoardGrid.getChildCount(); i++) {
                    FrameLayout cell = (FrameLayout) chessBoardGrid.getChildAt(i);
                    if (cell.getChildCount() > 0) {
                        ImageView piece = (ImageView) cell.getChildAt(0);
                        // flip texture by negating scaleX
                        piece.setScaleY(isBoardFlipped ? -1f : 1f);  // mirror when flipped
                    }
                }
            }
        }, 200); // 200ms Verzögerung für einen flüssigeren Übergang
    }


    // --- GEÄNDERT: Zugvalidierung prüft jetzt auch den Spieler ---
    private boolean isMoveValid(int startPos, int endPos) {
        if (startPos == endPos) return false;

        String pieceCode = currentBoardState[startPos];
        String targetCode = currentBoardState[endPos];

        if (" ".equals(pieceCode)) return false;

        // PRÜFUNG 1: Ist der richtige Spieler am Zug?
        if (getPieceColor(pieceCode.charAt(0)) != currentPlayerTurn) {
            return false;
        }

        // PRÜFUNG 2: Wird eine eigene Figur geschlagen?
        if (!" ".equals(targetCode) && getPieceColor(pieceCode.charAt(0)) == getPieceColor(targetCode.charAt(0))) {
            return false;
        }

        int startRow = startPos / BOARD_SIZE;
        int startCol = startPos % BOARD_SIZE;
        int endRow = endPos / BOARD_SIZE;
        int endCol = endPos % BOARD_SIZE;

        char pieceType = Character.toLowerCase(pieceCode.charAt(0));

        switch (pieceType) {
            case 'p': return isPawnMoveValid(startRow, startCol, endRow, endCol, getPieceColor(pieceCode.charAt(0)), targetCode);
            case 'r': return isRookMoveValid(startRow, startCol, endRow, endCol) && isPathClear(startPos, endPos);
            case 'n': return isKnightMoveValid(startRow, startCol, endRow, endCol);
            case 'b': return isBishopMoveValid(startRow, startCol, endRow, endCol) && isPathClear(startPos, endPos);
            case 'q': return (isRookMoveValid(startRow, startCol, endRow, endCol) || isBishopMoveValid(startRow, startCol, endRow, endCol)) && isPathClear(startPos, endPos);
            case 'k': return isKingMoveValid(startRow, startCol, endRow, endCol);
            default: return false;
        }
    }

    private char getPieceColor(char piece) {
        if (Character.isUpperCase(piece)) return 'W';
        if (Character.isLowerCase(piece)) return 'B';
        return ' ';
    }

    private boolean isPawnMoveValid(int startRow, int startCol, int endRow, int endCol, char color, String targetCode) {
        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;

        if (color == 'W') {
            if (rowDiff == -1 && colDiff == 0 && " ".equals(targetCode)) return true;
            if (startRow == 6 && rowDiff == -2 && colDiff == 0 && " ".equals(targetCode)) return true;
            return rowDiff == -1 && Math.abs(colDiff) == 1 && !" ".equals(targetCode);
        } else {
            if (rowDiff == 1 && colDiff == 0 && " ".equals(targetCode)) return true;
            if (startRow == 1 && rowDiff == 2 && colDiff == 0 && " ".equals(targetCode)) return true;
            return rowDiff == 1 && Math.abs(colDiff) == 1 && !" ".equals(targetCode);
        }

        /*if (enPassantTarget != -1) {

        }*/

    }

    private boolean isRookMoveValid(int startRow, int startCol, int endRow, int endCol) {
        return startRow == endRow || startCol == endCol;
    }

    private boolean isKnightMoveValid(int startRow, int startCol, int endRow, int endCol) {
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isBishopMoveValid(int startRow, int startCol, int endRow, int endCol) {
        return Math.abs(startRow - endRow) == Math.abs(startCol - endCol);
    }

    private boolean isKingMoveValid(int startRow, int startCol, int endRow, int endCol) {
        return Math.abs(startRow - endRow) <= 1 && Math.abs(startCol - endCol) <= 1;
    }

    private boolean isPathClear(int startPos, int endPos) {
        int startRow = startPos / BOARD_SIZE, startCol = startPos % BOARD_SIZE;
        int endRow = endPos / BOARD_SIZE, endCol = endPos % BOARD_SIZE;
        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;
        int rowStep = Integer.signum(rowDiff);
        int colStep = Integer.signum(colDiff);
        int currentPos = startPos + rowStep * BOARD_SIZE + colStep;

        while (currentPos != endPos) {
            if (!" ".equals(currentBoardState[currentPos])) {
                return false;
            }
            currentPos += rowStep * BOARD_SIZE + colStep;
        }
        return true;
    }

    private void setupDummyButtons() {
        ImageView menuButton = findViewById(R.id.iv_menu_button);
        Button drawButton = findViewById(R.id.btn_offer_draw);
        Button resignButton = findViewById(R.id.btn_resign_game);

        View.OnClickListener dummyListener = v -> {
            String txt = (v instanceof Button) ? ((Button) v).getText().toString() : "Menu";
            Toast.makeText(this, txt + " clicked (no action yet)", Toast.LENGTH_SHORT).show();

            if (v.getId() == R.id.iv_menu_button) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        };

        if (menuButton != null) menuButton.setOnClickListener(dummyListener);
        if (drawButton != null) drawButton.setOnClickListener(dummyListener);
        if (resignButton != null) resignButton.setOnClickListener(dummyListener);
    }

    private int getCellColor(int index) {
        int row = index / BOARD_SIZE;
        int col = index % BOARD_SIZE;
        return ((row + col) % 2 == 0) ? Color.parseColor("#FFF8DC")
                : Color.parseColor("#8B4513");
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
            default: return 0;
        }
    }
}