package de.brockmann.chessinterface;

import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public abstract class MenuActivity extends AppCompatActivity {

    /** Subclasses return their own content-layout (buttons etc). */
    @LayoutRes
    protected abstract int getContentLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        // inflate subclass content into the frame
        int layoutId = getContentLayoutId();
        if (layoutId != 0) {
            LayoutInflater.from(this)
                    .inflate(layoutId,
                            (ViewGroup) findViewById(R.id.menu_content_frame),
                            true);
        }
    }
}
