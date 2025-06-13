package android.view;

public class View {
    private Object tag;
    private ViewParent parent;
    public void setTag(Object tag) { this.tag = tag; }
    public Object getTag() { return tag; }
    public void setLayoutParams(Object lp) {}
    public void setOnDragListener(Object l) {}
    public void setOnClickListener(Object l) {}
    public void setOnLongClickListener(Object l) {}
    public void setRotation(float rotation) {}
    public ViewParent getParent() { return parent; }
    protected void setParent(ViewParent parent) { this.parent = parent; }
}

interface ViewParent {}
