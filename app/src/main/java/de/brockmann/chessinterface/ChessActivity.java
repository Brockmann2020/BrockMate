package de.brockmann.chessinterface;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ChessActivity extends AppCompatActivity {

    private static final int BOARD_SIZE = 8;
    protected GridLayout chessBoardGrid;
    private View gameEndOverlay;
    private TextView gameEndMessage;
    private View drawOfferOverlay;
    private View resignConfirmOverlay;

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

    // track fullmove number for FEN generation
    protected int fullmoveNumber = 1;

    // --- NEUE VARIABLEN FÜR SPIELZUSTAND ---
    protected char currentPlayerTurn; // 'W' für Weiß, 'B' für Schwarz

    private int enPassantTarget = -1;

    // Castling state
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteKingsideRookMoved = false;
    private boolean whiteQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false;
    private boolean blackQueensideRookMoved = false;

    // Draw detection state
    private int halfmoveClock = 0;
    private final java.util.Map<String, Integer> positionCount = new java.util.HashMap<>();

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

        chessBoardGrid = findViewById(R.id.chess_board_grid);
        gameEndOverlay = findViewById(R.id.game_end_overlay);
        drawOfferOverlay = findViewById(R.id.draw_offer_overlay);
        resignConfirmOverlay = findViewById(R.id.resign_confirm_overlay);
        if (gameEndOverlay != null) {
            gameEndMessage = gameEndOverlay.findViewById(R.id.tv_game_end_message);
            Button newGame = gameEndOverlay.findViewById(R.id.btn_new_game);
            Button mainMenu = gameEndOverlay.findViewById(R.id.btn_main_menu);
            newGame.setOnClickListener(v -> {
                initializeBoard();
                hideGameEndOverlay();
            });
            mainMenu.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            });
        }

        if (drawOfferOverlay != null) {
            Button accept = drawOfferOverlay.findViewById(R.id.btn_accept_draw);
            Button decline = drawOfferOverlay.findViewById(R.id.btn_decline_draw);
            accept.setOnClickListener(v -> {
                hideDrawOfferOverlay();
                showGameEndOverlay("Draw agreed");
            });
            decline.setOnClickListener(v -> hideDrawOfferOverlay());
        }

        if (resignConfirmOverlay != null) {
            Button yes = resignConfirmOverlay.findViewById(R.id.btn_resign_yes);
            Button no = resignConfirmOverlay.findViewById(R.id.btn_resign_no);
            yes.setOnClickListener(v -> {
                hideResignConfirmOverlay();
                String winner = currentPlayerTurn == 'W' ? "Black" : "White";
                showGameEndOverlay(winner + " won by resignation");
            });
            no.setOnClickListener(v -> hideResignConfirmOverlay());
        }
        setupDummyButtons();

        chessBoardGrid.post(this::initializeBoard);
    }

    private void initializeBoard() {
        hideGameEndOverlay();
        currentBoardState = initialBoardSetup.clone();
        // --- NEU: Startspieler festlegen ---
        currentPlayerTurn = 'W';
        fullmoveNumber = 1;
        enPassantTarget = -1;
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteKingsideRookMoved = false;
        whiteQueensideRookMoved = false;
        blackKingsideRookMoved = false;
        blackQueensideRookMoved = false;
        halfmoveClock = 0;
        positionCount.clear();
        Toast.makeText(this, "White's turn", Toast.LENGTH_SHORT).show();

        setupBoardCells();
        placePiecesOnBoard();
        recordCurrentPosition();
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
    protected void performMove(int from, int to, ImageView pieceView) {
        // UI aktualisieren
        FrameLayout sourceCell = (FrameLayout) chessBoardGrid.getChildAt(from);
        FrameLayout targetCell = (FrameLayout) chessBoardGrid.getChildAt(to);

        String piece = currentBoardState[from];
        String targetBefore = currentBoardState[to];
        boolean enPassantCapture = false;
        // Detect and perform En Passant
        if (piece.equalsIgnoreCase("p") &&
                " ".equals(targetBefore) &&
                enPassantTarget != -1 &&
                Math.abs(enPassantTarget - to) == 8
        ) {
            FrameLayout enPassantTargetCell = (FrameLayout) chessBoardGrid.getChildAt(enPassantTarget);
            enPassantTargetCell.removeAllViews();
            enPassantCapture = true;
        }

        sourceCell.removeView(pieceView);
        if (targetCell.getChildCount() > 0) {
            targetCell.removeAllViews();
        }
        targetCell.addView(pieceView);

        // Internen Zustand aktualisieren

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
        if (enPassantCapture) {
            int capPos = piece.equals("P") ? to + 8 : to - 8;
            if (capPos >= 0 && capPos < 64) currentBoardState[capPos] = " ";
        }

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
        enPassantTarget = -1;
        if (piece.equalsIgnoreCase("p") && Math.abs(to - from) == 16) {
            enPassantTarget = to;
        }

        boolean isPawnMove = piece.equalsIgnoreCase("p");
        boolean isCapture = !" ".equals(targetBefore) || enPassantCapture;
        if (isPawnMove || isCapture) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }

        // Spieler wechseln
        switchPlayer();
        checkGameState();
    }

    // --- NEUE METHODE: Wechselt den Spieler. Weitere Aktionen übernimmt die Unterklasse ---
    protected abstract void switchPlayer();

    protected void toggleCurrentPlayer() {
        boolean wasBlack = currentPlayerTurn == 'B';
        currentPlayerTurn = (currentPlayerTurn == 'W') ? 'B' : 'W';
        if (wasBlack && currentPlayerTurn == 'W') {
            fullmoveNumber++;
        }
        Toast.makeText(this,
                (currentPlayerTurn == 'W' ? "White's" : "Black's") + " turn",
                Toast.LENGTH_SHORT).show();
    }

    /** Returns the current board state as FEN string for the engine. */
    protected String getFEN() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < BOARD_SIZE; row++) {
            int empty = 0;
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = currentBoardState[row * BOARD_SIZE + col];
                if (" ".equals(piece)) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    sb.append(piece);
                }
            }
            if (empty > 0) sb.append(empty);
            if (row < BOARD_SIZE - 1) sb.append('/');
        }

        sb.append(' ');
        sb.append(currentPlayerTurn == 'W' ? 'w' : 'b');
        sb.append(' ');

        StringBuilder castling = new StringBuilder();
        if (!whiteKingMoved && !whiteKingsideRookMoved && "R".equals(currentBoardState[63]))
            castling.append('K');
        if (!whiteKingMoved && !whiteQueensideRookMoved && "R".equals(currentBoardState[56]))
            castling.append('Q');
        if (!blackKingMoved && !blackKingsideRookMoved && "r".equals(currentBoardState[7]))
            castling.append('k');
        if (!blackKingMoved && !blackQueensideRookMoved && "r".equals(currentBoardState[0]))
            castling.append('q');
        sb.append(castling.length() == 0 ? "-" : castling.toString());
        sb.append(' ');

        if (enPassantTarget == -1) {
            sb.append('-');
        } else {
            int r = enPassantTarget / BOARD_SIZE;
            int c = enPassantTarget % BOARD_SIZE;
            sb.append((char)('a' + c));
            sb.append((char)('8' - r));
        }

        sb.append(' ');
        sb.append(halfmoveClock);
        sb.append(' ');
        sb.append(fullmoveNumber);
        return sb.toString();
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
                return !isSquareAttacked(currentBoardState, startPos + 2, enemy);
            } else if (endCol == 2) { // queenside
                if (whiteQueensideRookMoved) return false;
                if (!"R".equals(currentBoardState[56])) return false;
                if (!isPathClear(startPos, 56)) return false;
                char enemy = 'B';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-1, enemy)) return false;
                return !isSquareAttacked(currentBoardState, startPos - 2, enemy);
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
                return !isSquareAttacked(currentBoardState, startPos + 2, enemy);
            } else if (endCol == 2) {
                if (blackQueensideRookMoved) return false;
                if (!"r".equals(currentBoardState[0])) return false;
                if (!isPathClear(startPos, 0)) return false;
                char enemy = 'W';
                if (isSquareAttacked(currentBoardState, startPos, enemy)) return false;
                if (isSquareAttacked(currentBoardState, startPos-1, enemy)) return false;
                return !isSquareAttacked(currentBoardState, startPos - 2, enemy);
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
        recordCurrentPosition();

        if (halfmoveClock >= 100) {
            showGameEndOverlay("Draw by 50-move rule!");
            return;
        }

        if (positionCount.getOrDefault(getPositionKey(), 0) >= 3) {
            showGameEndOverlay("Draw by threefold repetition!");
            return;
        }

        char player = currentPlayerTurn;
        boolean inCheck = isKingInCheck(player, currentBoardState);
        boolean hasMove = hasAnyLegalMove(player);
        if (!hasMove) {
            if (inCheck) {
                String winner = (player == 'W') ? "Black" : "White";
                showGameEndOverlay(winner + " won");
            } else {
                showGameEndOverlay("Stalemate!");
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

    protected void showGameEndOverlay(String message) {
        if (gameEndOverlay == null) return;
        gameEndMessage.setText(message);
        gameEndOverlay.setVisibility(View.VISIBLE);
    }

    protected void hideGameEndOverlay() {
        if (gameEndOverlay != null) {
            gameEndOverlay.setVisibility(View.GONE);
        }
    }

    protected void showDrawOfferOverlay() {
        if (drawOfferOverlay != null) {
            drawOfferOverlay.setVisibility(View.VISIBLE);
        }
    }

    protected void hideDrawOfferOverlay() {
        if (drawOfferOverlay != null) {
            drawOfferOverlay.setVisibility(View.GONE);
        }
    }

    protected void showResignConfirmOverlay() {
        if (resignConfirmOverlay != null) {
            resignConfirmOverlay.setVisibility(View.VISIBLE);
        }
    }

    protected void hideResignConfirmOverlay() {
        if (resignConfirmOverlay != null) {
            resignConfirmOverlay.setVisibility(View.GONE);
        }
    }

    // --- Helper methods for draw detection ---
    private String getPositionKey() {
        StringBuilder sb = new StringBuilder();
        for (String s : currentBoardState) sb.append(s);
        sb.append(currentPlayerTurn);
        sb.append(whiteKingMoved).append(blackKingMoved);
        sb.append(whiteKingsideRookMoved).append(whiteQueensideRookMoved);
        sb.append(blackKingsideRookMoved).append(blackQueensideRookMoved);
        sb.append(enPassantTarget);
        return sb.toString();
    }

    private void recordCurrentPosition() {
        String key = getPositionKey();
        int c = positionCount.getOrDefault(key, 0) + 1;
        positionCount.put(key, c);
    }

    /** Returns a clone of the internal board array. */
    protected String[] getBoardStateCopy() {
        return currentBoardState.clone();
    }

    /**
     * Loads a position from a FEN string and updates the board and all
     * relevant state. Only a subset of FEN fields is respected.
     */
    protected void loadFromFEN(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length < 2) return;

        // board
        String[] rows = parts[0].split("/");
        currentBoardState = new String[64];
        int idx = 0;
        for (String row : rows) {
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                if (Character.isDigit(ch)) {
                    int empty = ch - '0';
                    for (int j = 0; j < empty; j++) currentBoardState[idx++] = " ";
                } else {
                    currentBoardState[idx++] = String.valueOf(ch);
                }
            }
        }

        // active color
        currentPlayerTurn = parts[1].equals("w") ? 'W' : 'B';

        // castling rights
        String castling = parts.length > 2 ? parts[2] : "-";
        whiteKingsideRookMoved = !castling.contains("K");
        whiteQueensideRookMoved = !castling.contains("Q");
        blackKingsideRookMoved = !castling.contains("k");
        blackQueensideRookMoved = !castling.contains("q");
        whiteKingMoved = whiteKingsideRookMoved && whiteQueensideRookMoved;
        blackKingMoved = blackKingsideRookMoved && blackQueensideRookMoved;

        // en passant
        if (parts.length > 3 && !"-".equals(parts[3])) {
            String ep = parts[3];
            int file = ep.charAt(0) - 'a';
            int rank = 8 - Character.getNumericValue(ep.charAt(1));
            enPassantTarget = rank * BOARD_SIZE + file;
        } else {
            enPassantTarget = -1;
        }

        if (parts.length > 4) {
            try { halfmoveClock = Integer.parseInt(parts[4]); } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 5) {
            try { fullmoveNumber = Integer.parseInt(parts[5]); } catch (NumberFormatException ignored) {}
        }

        setupBoardCells();
        placePiecesOnBoard();
    }

    protected boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        } else {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
    }
}