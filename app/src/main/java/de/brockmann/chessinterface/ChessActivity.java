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

    // Castling state
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteKingsideRookMoved = false;
    private boolean whiteQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false;
    private boolean blackQueensideRookMoved = false;

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
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteKingsideRookMoved = false;
        whiteQueensideRookMoved = false;
        blackKingsideRookMoved = false;
        blackQueensideRookMoved = false;
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

        // Detect and perform En Passant
        if (currentBoardState[from].equalsIgnoreCase("p") &&
                currentBoardState[to].equals(" ") &&
                enPassantTarget != -1 &&
                Math.abs(enPassantTarget - to) == 8
        ) {
            FrameLayout enPassantTargetCell = (FrameLayout) chessBoardGrid.getChildAt(enPassantTarget);
            enPassantTargetCell.removeAllViews();
        }

        sourceCell.removeView(pieceView);
        if (targetCell.getChildCount() > 0) {
            targetCell.removeAllViews();
        }
        targetCell.addView(pieceView);

        // Internen Zustand aktualisieren
        String piece = currentBoardState[from];

        // Handle castling rook movement
        if (piece.equalsIgnoreCase("k") && Math.abs(to - from) == 2) {
            boolean kingside = to > from;
            int rookFrom = kingside ? from + 3 : from - 4;
            int rookTo = kingside ? from + 1 : from - 1;

            FrameLayout rookSource = (FrameLayout) chessBoardGrid.getChildAt(rookFrom);
            FrameLayout rookTarget = (FrameLayout) chessBoardGrid.getChildAt(rookTo);
            if (rookSource.getChildCount() > 0) {
                ImageView rookView = (ImageView) rookSource.getChildAt(0);
                rookSource.removeView(rookView);
                if (rookTarget.getChildCount() > 0) rookTarget.removeAllViews();
                rookTarget.addView(rookView);
            }

            currentBoardState[rookTo] = currentBoardState[rookFrom];
            currentBoardState[rookFrom] = " ";

            // update rook moved flags
            if (piece.equals("K")) {
                if (kingside) whiteKingsideRookMoved = true; else whiteQueensideRookMoved = true;
            } else if (piece.equals("k")) {
                if (kingside) blackKingsideRookMoved = true; else blackQueensideRookMoved = true;
            }
        }

        currentBoardState[to] = piece;
        currentBoardState[from] = " ";

        // update moved flags
        if (piece.equals("K")) whiteKingMoved = true;
        if (piece.equals("k")) blackKingMoved = true;
        if (piece.equals("R")) {
            if (from == 56) whiteQueensideRookMoved = true;
            if (from == 63) whiteKingsideRookMoved = true;
        }
        if (piece.equals("r")) {
            if (from == 0) blackQueensideRookMoved = true;
            if (from == 7) blackKingsideRookMoved = true;
        }

        // En Passant possible in next move
        if (piece.equalsIgnoreCase("p") && Math.abs(to - from) == 16) {
            enPassantTarget = to;
        }

        // Spieler wechseln
        switchTurn();
        checkGameState();
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


    // --- Zugvalidierung: prüft zunächst pseudolegal und danach, ob der eigene
    // König im Schach bleiben würde ---
    private boolean isMoveValid(int startPos, int endPos) {
        if (!isPseudoLegalMove(startPos, endPos)) return false;
        return !wouldLeaveKingInCheck(startPos, endPos);
    }

    // Pseudolegale Zugvalidierung ohne Berücksichtigung von Schach
    private boolean isPseudoLegalMove(int startPos, int endPos) {
        if (startPos == endPos) return false;

        String pieceCode = currentBoardState[startPos];
        String targetCode = currentBoardState[endPos];

        if (" ".equals(pieceCode)) return false;

        if (getPieceColor(pieceCode.charAt(0)) != currentPlayerTurn) {
            return false;
        }

        if (!" ".equals(targetCode) && getPieceColor(pieceCode.charAt(0)) == getPieceColor(targetCode.charAt(0))) {
            return false;
        }

        int startRow = startPos / BOARD_SIZE;
        int startCol = startPos % BOARD_SIZE;
        int endRow = endPos / BOARD_SIZE;
        int endCol = endPos % BOARD_SIZE;

        char pieceType = Character.toLowerCase(pieceCode.charAt(0));

        switch (pieceType) {
            case 'p':
                return isPawnMoveValid(startRow, startCol, endRow, endCol, endPos, getPieceColor(pieceCode.charAt(0)), targetCode);
            case 'r':
                return isRookMoveValid(startRow, startCol, endRow, endCol) && isPathClear(startPos, endPos);
            case 'n':
                return isKnightMoveValid(startRow, startCol, endRow, endCol);
            case 'b':
                return isBishopMoveValid(startRow, startCol, endRow, endCol) && isPathClear(startPos, endPos);
            case 'q':
                return (isRookMoveValid(startRow, startCol, endRow, endCol) ||
                        isBishopMoveValid(startRow, startCol, endRow, endCol)) &&
                        isPathClear(startPos, endPos);
            case 'k':
                return isCastlingMove(startRow, startCol, endRow, endCol, getPieceColor(pieceCode.charAt(0))) ||
                        isKingMoveValid(startRow, startCol, endRow, endCol);
            default:
                return false;
        }
    }

    private char getPieceColor(char piece) {
        if (Character.isUpperCase(piece)) return 'W';
        if (Character.isLowerCase(piece)) return 'B';
        return ' ';
    }

    private boolean isPawnMoveValid(int startRow, int startCol, int endRow, int endCol, int endPos, char color, String targetCode) {
        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;

        if (color == 'W') {
            if (rowDiff == -1 && colDiff == 0 && " ".equals(targetCode)) return true;
            if (startRow == 6 && rowDiff == -2 && colDiff == 0 && " ".equals(targetCode)) return true;
            if (rowDiff == -1 && Math.abs(colDiff) == 1 && enPassantTarget == endPos+8 && " ".equals(targetCode)) return true;
            return rowDiff == -1 && Math.abs(colDiff) == 1 && !" ".equals(targetCode);
        } else {
            if (rowDiff == 1 && colDiff == 0 && " ".equals(targetCode)) return true;
            if (startRow == 1 && rowDiff == 2 && colDiff == 0 && " ".equals(targetCode) ) return true;
            if (rowDiff == 1 && Math.abs(colDiff) == 1 && enPassantTarget == endPos-8 && " ".equals(targetCode)) return true;
            return rowDiff == 1 && Math.abs(colDiff) == 1 && !" ".equals(targetCode);
        }
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

    private boolean isCastlingMove(int startRow, int startCol, int endRow, int endCol, char color) {
        if (startRow != endRow) return false;
        if (Math.abs(endCol - startCol) != 2) return false;

        int kingStartRow = (color == 'W') ? 7 : 0;
        int kingStartCol = 4;
        if (startRow != kingStartRow || startCol != kingStartCol) return false;

        int startPos = startRow * BOARD_SIZE + startCol;

        if (color == 'W') {
            if (whiteKingMoved) return false;
            if (endCol == 6) { // kingside
                if (whiteKingsideRookMoved) return false;
                if (!"R".equals(currentBoardState[63])) return false;
                if (!isPathClear(startPos, 63)) return false;
                char enemy = 'B';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos+1, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos+2, enemy)) return false;
                return true;
            } else if (endCol == 2) { // queenside
                if (whiteQueensideRookMoved) return false;
                if (!"R".equals(currentBoardState[56])) return false;
                if (!isPathClear(startPos, 56)) return false;
                char enemy = 'B';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-1, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-2, enemy)) return false;
                return true;
            }
        } else {
            if (blackKingMoved) return false;
            if (endCol == 6) {
                if (blackKingsideRookMoved) return false;
                if (!"r".equals(currentBoardState[7])) return false;
                if (!isPathClear(startPos, 7)) return false;
                char enemy = 'W';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos+1, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos+2, enemy)) return false;
                return true;
            } else if (endCol == 2) {
                if (blackQueensideRookMoved) return false;
                if (!"r".equals(currentBoardState[0])) return false;
                if (!isPathClear(startPos, 0)) return false;
                char enemy = 'W';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-1, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-2, enemy)) return false;
                return true;
            }
        }
        return false;
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

    // Prüft, ob das Feld 'pos' von der Farbe 'byColor' angegriffen wird
    private boolean isSquareAttacked(String[] board, int pos, char byColor) {
        int row = pos / BOARD_SIZE;
        int col = pos % BOARD_SIZE;

        int pawnRow = row + (byColor == 'W' ? 1 : -1);
        if (pawnRow >= 0 && pawnRow < BOARD_SIZE) {
            String pawn = byColor == 'W' ? "P" : "p";
            if (col - 1 >= 0 && pawn.equals(board[pawnRow * BOARD_SIZE + (col - 1)])) return true;
            if (col + 1 < BOARD_SIZE && pawn.equals(board[pawnRow * BOARD_SIZE + (col + 1)])) return true;
        }

        int[][] knightMoves = { {1,2}, {2,1}, {-1,2}, {-2,1}, {1,-2}, {2,-1}, {-1,-2}, {-2,-1} };
        for (int[] m : knightMoves) {
            int r = row + m[0];
            int c = col + m[1];
            if (r>=0 && r<BOARD_SIZE && c>=0 && c<BOARD_SIZE) {
                String p = board[r*BOARD_SIZE+c];
                if ((byColor=='W' && "N".equals(p)) || (byColor=='B' && "n".equals(p))) return true;
            }
        }

        int[][] directions = { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        for (int i=0;i<directions.length;i++) {
            int[] d = directions[i];
            int r=row+d[0];
            int c=col+d[1];
            while (r>=0 && r<BOARD_SIZE && c>=0 && c<BOARD_SIZE) {
                String p = board[r*BOARD_SIZE+c];
                if (!" ".equals(p)) {
                    char pc = p.charAt(0);
                    if (byColor=='W' && Character.isUpperCase(pc) || byColor=='B' && Character.isLowerCase(pc)) {
                        char t = Character.toLowerCase(pc);
                        if ((i<4 && (t=='r' || t=='q')) || (i>=4 && (t=='b' || t=='q'))) return true;
                        if (t=='k' && Math.max(Math.abs(r-row),Math.abs(c-col))==1) return true;
                    }
                    break;
                }
                r+=d[0];
                c+=d[1];
            }
        }
        return false;
    }

    private boolean isKingInCheck(char color, String[] board) {
        char kingChar = (color=='W') ? 'K' : 'k';
        int kingPos = -1;
        for (int i=0;i<board.length;i++) {
            if (kingChar==board[i].charAt(0)) { kingPos=i; break; }
        }
        if (kingPos==-1) return false;
        char enemy = (color=='W') ? 'B' : 'W';
        return isSquareAttacked(board, kingPos, enemy);
    }

    private String[] makeMoveCopy(int from, int to) {
        String[] copy = currentBoardState.clone();
        String piece = copy[from];
        copy[from] = " ";
        // en passant capture
        if (piece.equalsIgnoreCase("p") && from%8!=to%8 && " ".equals(copy[to])) {
            int cap = (piece.equals("P")) ? to+8 : to-8;
            if (cap>=0 && cap<64) copy[cap]=" ";
        }
        copy[to] = piece;
        if (piece.equalsIgnoreCase("k") && Math.abs(to-from)==2) {
            boolean ks = to>from;
            int rookFrom = ks ? from+3 : from-4;
            int rookTo   = ks ? from+1 : from-1;
            copy[rookTo] = copy[rookFrom];
            copy[rookFrom] = " ";
        }
        return copy;
    }

    private boolean wouldLeaveKingInCheck(int from, int to) {
        String[] copy = makeMoveCopy(from, to);
        char color = getPieceColor(currentBoardState[from].charAt(0));
        return isKingInCheck(color, copy);
    }

    private boolean hasAnyLegalMove(char player) {
        char save = currentPlayerTurn;
        currentPlayerTurn = player;
        for (int i=0;i<64;i++) {
            if (!" ".equals(currentBoardState[i]) && getPieceColor(currentBoardState[i].charAt(0))==player) {
                for (int j=0;j<64;j++) {
                    if (isMoveValid(i,j)) { currentPlayerTurn=save; return true; }
                }
            }
        }
        currentPlayerTurn = save;
        return false;
    }

    private void checkGameState() {
        char player = currentPlayerTurn;
        boolean inCheck = isKingInCheck(player, currentBoardState);
        boolean hasMove = hasAnyLegalMove(player);
        if (!hasMove) {
            if (inCheck) {
                Toast.makeText(this, "Checkmate!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Stalemate!", Toast.LENGTH_LONG).show();
            }
        } else if (inCheck) {
            Toast.makeText(this, "Check!", Toast.LENGTH_SHORT).show();
        }
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