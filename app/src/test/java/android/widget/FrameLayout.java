package android.widget;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FrameLayout extends View {
    public FrameLayout(Context context) {}
    private final List<View> children = new ArrayList<>();
    public void addView(View v) { if(v!=null){ children.add(v); if(v instanceof View){ ((View)v).setParent(() -> this); } }}
    public void removeView(View v) { children.remove(v); }
    public void removeAllViews() { children.clear(); }
    public int getChildCount() { return children.size(); }
    public View getChildAt(int index) { return children.get(index); }

    public static class LayoutParams {
        public static final int MATCH_PARENT = -1;
        public int width;
        public int height;
        public int gravity;
        public LayoutParams(int w,int h,int g){width=w;height=h;gravity=g;}
        public LayoutParams(int w,int h){this(w,h,0);}
    }
}
