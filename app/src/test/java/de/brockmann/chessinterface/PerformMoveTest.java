package de.brockmann.chessinterface;

import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class PerformMoveTest {

    private static int idx(String square){
        int col = square.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(square.charAt(1));
        return row * 8 + col;
    }

    private ChessActivity createActivity(String[] board, int enPassant) throws Exception {
        ChessActivity activity = new ChessActivity() {
            @Override
            protected int getContentLayoutId() { return 0; }
        };

        GridLayout grid = new GridLayout(null);
        grid.setColumnCount(8);
        grid.setRowCount(8);
        for (int i=0;i<64;i++) {
            FrameLayout cell = new FrameLayout(null);
            cell.setTag(i);
            if (board[i] != null && !" ".equals(board[i])) {
                ImageView img = new ImageView(null);
                img.setTag(board[i]);
                cell.addView(img);
            }
            grid.addView(cell);
        }

        setField(activity, "chessBoardGrid", grid);
        setField(activity, "currentBoardState", board);
        setField(activity, "enPassantTarget", enPassant);
        return activity;
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = ChessActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object getField(Object obj, String name) throws Exception {
        Field f = ChessActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static void invokePerformMove(ChessActivity a, int from, int to, ImageView piece) throws Exception {
        Method m = ChessActivity.class.getDeclaredMethod("performMove", int.class, int.class, ImageView.class);
        m.setAccessible(true);
        m.invoke(a, from, to, piece);
    }

    @Test
    public void testNormalMove() throws Exception {
        String[] board = new String[64];
        for(int i=0;i<64;i++) board[i] = " ";
        board[idx("e2")] = "P";
        ChessActivity a = createActivity(board, -1);
        FrameLayout fromCell = (FrameLayout)((GridLayout)getField(a,"chessBoardGrid")).getChildAt(idx("e2"));
        ImageView piece = (ImageView)fromCell.getChildAt(0);
        invokePerformMove(a, idx("e2"), idx("e4"), piece);
        String[] after = (String[]) getField(a,"currentBoardState");
        assertEquals(" ", after[idx("e2")]);
        assertEquals("P", after[idx("e4")]);
    }

    @Test
    public void testCaptureMove() throws Exception {
        String[] board = new String[64];
        for(int i=0;i<64;i++) board[i] = " ";
        board[idx("e4")] = "P";
        board[idx("d5")] = "p";
        ChessActivity a = createActivity(board, -1);
        FrameLayout fromCell = (FrameLayout)((GridLayout)getField(a,"chessBoardGrid")).getChildAt(idx("e4"));
        ImageView piece = (ImageView)fromCell.getChildAt(0);
        invokePerformMove(a, idx("e4"), idx("d5"), piece);
        String[] after = (String[]) getField(a,"currentBoardState");
        assertEquals(" ", after[idx("e4")]);
        assertEquals("P", after[idx("d5")]);
    }

    @Test
    public void testEnPassantCapture() throws Exception {
        String[] board = new String[64];
        for(int i=0;i<64;i++) board[i] = " ";
        board[idx("e5")] = "P";
        board[idx("d5")] = "p"; // pawn just moved two squares
        ChessActivity a = createActivity(board, idx("d5"));
        FrameLayout fromCell = (FrameLayout)((GridLayout)getField(a,"chessBoardGrid")).getChildAt(idx("e5"));
        ImageView piece = (ImageView)fromCell.getChildAt(0);
        invokePerformMove(a, idx("e5"), idx("d6"), piece);
        String[] after = (String[]) getField(a,"currentBoardState");
        assertEquals(" ", after[idx("e5")]);
        assertEquals("P", after[idx("d6")]);
        assertEquals(" ", after[idx("d5")]);
    }

    @Test
    public void testCastlingKingside() throws Exception {
        String[] board = new String[64];
        for(int i=0;i<64;i++) board[i] = " ";
        board[idx("e1")] = "K";
        board[idx("h1")] = "R";
        ChessActivity a = createActivity(board, -1);
        FrameLayout fromCell = (FrameLayout)((GridLayout)getField(a,"chessBoardGrid")).getChildAt(idx("e1"));
        ImageView piece = (ImageView)fromCell.getChildAt(0);
        invokePerformMove(a, idx("e1"), idx("g1"), piece);
        String[] after = (String[]) getField(a,"currentBoardState");
        assertEquals(" ", after[idx("e1")]);
        assertEquals("K", after[idx("g1")]);
        assertEquals("R", after[idx("f1")]);
    }
}
