package de.brockmann.chessinterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Very small fallback engine used when the online Stockfish
 * service is unavailable. It performs a simple minimax search
 * with piece values to a limited depth. It only implements a
 * subset of chess rules (no castling, en-passant or promotion),
 * but is sufficient to point out a reasonable move so that
 * the analysis arrow still shows something.
 */
public class SimpleChessEngine {
    private static final int BOARD_SIZE = 8;

    private static class Move {
        int fr, fc, tr, tc;
        Move(int fr, int fc, int tr, int tc) { this.fr = fr; this.fc = fc; this.tr = tr; this.tc = tc; }
    }

    private static class Board {
        char[][] b = new char[BOARD_SIZE][BOARD_SIZE];
        char toMove;
    }

    public String bestMove(String fen, int depth) {
        Board board = parseFEN(fen);
        Move best = search(board, depth).move;
        if (best == null) return null;
        return toAlgebraic(best.fr, best.fc) + toAlgebraic(best.tr, best.tc);
    }

    private static class ScoreMove {
        int score; Move move;
        ScoreMove(int s, Move m) { score = s; move = m; }
    }

    private ScoreMove search(Board board, int depth) {
        List<Move> moves = generateMoves(board, board.toMove);
        if (moves.isEmpty() || depth == 0) {
            return new ScoreMove(evaluate(board), null);
        }
        ScoreMove best = new ScoreMove(board.toMove == 'w' ? Integer.MIN_VALUE : Integer.MAX_VALUE, null);
        for (Move m : moves) {
            Board clone = cloneBoard(board);
            applyMove(clone, m);
            clone.toMove = (board.toMove == 'w') ? 'b' : 'w';
            int score = search(clone, depth - 1).score;
            if (board.toMove == 'w') {
                if (score > best.score) { best.score = score; best.move = m; }
            } else {
                if (score < best.score) { best.score = score; best.move = m; }
            }
        }
        return best;
    }

    private int evaluate(Board board) {
        int sum = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                char p = board.b[r][c];
                if (p == '.') continue;
                int val = pieceValue(Character.toLowerCase(p));
                sum += Character.isUpperCase(p) ? val : -val;
            }
        }
        return sum;
    }

    private int pieceValue(char p) {
        switch (p) {
            case 'p': return 100;
            case 'n': return 320;
            case 'b': return 330;
            case 'r': return 500;
            case 'q': return 900;
            case 'k': return 20000;
            default: return 0;
        }
    }

    private Board parseFEN(String fen) {
        Board board = new Board();
        String[] parts = fen.split(" ");
        String[] rows = parts[0].split("/");
        for (int r = 0; r < BOARD_SIZE; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    int empty = ch - '0';
                    for (int i = 0; i < empty; i++) board.b[r][c++] = '.';
                } else {
                    board.b[r][c++] = ch;
                }
            }
        }
        board.toMove = parts.length > 1 && parts[1].equals("b") ? 'b' : 'w';
        return board;
    }

    private Board cloneBoard(Board b) {
        Board n = new Board();
        for (int r = 0; r < BOARD_SIZE; r++)
            System.arraycopy(b.b[r], 0, n.b[r], 0, BOARD_SIZE);
        n.toMove = b.toMove;
        return n;
    }

    private void applyMove(Board b, Move m) {
        b.b[m.tr][m.tc] = b.b[m.fr][m.fc];
        b.b[m.fr][m.fc] = '.';
    }

    private List<Move> generateMoves(Board board, char color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                char p = board.b[r][c];
                if (p == '.') continue;
                if (color == 'w' && Character.isLowerCase(p)) continue;
                if (color == 'b' && Character.isUpperCase(p)) continue;
                switch (Character.toLowerCase(p)) {
                    case 'p': addPawnMoves(board, moves, r, c, color); break;
                    case 'n': addKnightMoves(board, moves, r, c, color); break;
                    case 'b': addSlidingMoves(board, moves, r, c, color, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                    case 'r': addSlidingMoves(board, moves, r, c, color, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                    case 'q': addSlidingMoves(board, moves, r, c, color, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}}); break;
                    case 'k': addKingMoves(board, moves, r, c, color); break;
                }
            }
        }
        return moves;
    }

    private void addPawnMoves(Board b, List<Move> moves, int r, int c, char color) {
        int dir = color == 'w' ? -1 : 1;
        int start = color == 'w' ? 6 : 1;
        int nr = r + dir;
        if (inside(nr, c) && b.b[nr][c] == '.') moves.add(new Move(r,c,nr,c));
        nr = r + 2*dir;
        if (r == start && inside(nr, c) && b.b[r+dir][c]=='.' && b.b[nr][c]=='.') moves.add(new Move(r,c,nr,c));
        int[] cols = {c-1, c+1};
        for (int nc : cols) {
            if (inside(r+dir, nc) && b.b[r+dir][nc] != '.' && isEnemy(b.b[r+dir][nc], color))
                moves.add(new Move(r,c,r+dir,nc));
        }
    }

    private void addKnightMoves(Board b, List<Move> moves, int r, int c, char color) {
        int[][] d = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        for (int[] o : d) {
            int nr=r+o[0], nc=c+o[1];
            if (!inside(nr,nc)) continue;
            if (b.b[nr][nc]=='.' || isEnemy(b.b[nr][nc],color)) moves.add(new Move(r,c,nr,nc));
        }
    }

    private void addSlidingMoves(Board b, List<Move> moves, int r, int c, char color, int[][] dirs) {
        for (int[] d : dirs) {
            int nr=r+d[0], nc=c+d[1];
            while (inside(nr,nc)) {
                if (b.b[nr][nc]=='.') {
                    moves.add(new Move(r,c,nr,nc));
                } else {
                    if (isEnemy(b.b[nr][nc],color)) moves.add(new Move(r,c,nr,nc));
                    break;
                }
                nr+=d[0]; nc+=d[1];
            }
        }
    }

    private void addKingMoves(Board b, List<Move> moves, int r, int c, char color) {
        for (int dr=-1; dr<=1; dr++)
            for (int dc=-1; dc<=1; dc++) {
                if (dr==0 && dc==0) continue;
                int nr=r+dr,nc=c+dc;
                if (!inside(nr,nc)) continue;
                if (b.b[nr][nc]=='.' || isEnemy(b.b[nr][nc],color)) moves.add(new Move(r,c,nr,nc));
            }
    }

    private boolean inside(int r, int c) { return r>=0 && r<BOARD_SIZE && c>=0 && c<BOARD_SIZE; }
    private boolean isEnemy(char piece, char color) { return (color=='w' && Character.isLowerCase(piece)) || (color=='b' && Character.isUpperCase(piece)); }

    private String toAlgebraic(int r, int c) {
        return "" + (char)('a'+c) + (8-r);
    }
}
