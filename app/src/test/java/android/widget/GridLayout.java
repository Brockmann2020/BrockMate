package android.widget;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GridLayout extends View {
    private int columnCount;
    private int rowCount;
    private final List<View> children = new ArrayList<>();
    public GridLayout(Context context) {}
    public void setColumnCount(int c){ this.columnCount=c; }
    public void setRowCount(int r){ this.rowCount=r; }
    public void addView(View v){ if(v!=null){ children.add(v); if(v instanceof View){ ((View)v).setParent(() -> this); } } }
    public View getChildAt(int idx){ return children.get(idx); }
    public int getChildCount(){ return children.size(); }
    public void removeAllViews(){ children.clear(); }
    public int getWidth(){ return 0; }

    public static class LayoutParams {
        public int width;
        public int height;
        public Object columnSpec;
        public Object rowSpec;
    }
    public static Object spec(int index){ return index; }
}
