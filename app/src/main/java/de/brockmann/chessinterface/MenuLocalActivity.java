package de.brockmann.chessinterface;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;

public class MenuLocalActivity extends MenuActivity {

    public static final String EXTRA_LOCAL_ACTION    =
            "de.brockmann.chessinterface.LOCAL_ACTION";
    public static final String EXTRA_TIME_CONTROL   =
            "de.brockmann.chessinterface.TIME_CONTROL";

    @Override
    protected int getContentLayoutId() {
        return R.layout.menu_local_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Spinner spinner = findViewById(R.id.spinner_time_control);

        findViewById(R.id.btn_flip_board)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(this, LocalChessActivity.class);
                    intent.putExtra(EXTRA_LOCAL_ACTION, "flip_board");
                    intent.putExtra(EXTRA_TIME_CONTROL, spinner.getSelectedItem().toString());
                    startActivity(intent);
                });

        findViewById(R.id.btn_flip_pieces)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(this, LocalChessActivity.class);
                    intent.putExtra(EXTRA_LOCAL_ACTION, "flip_pieces");
                    intent.putExtra(EXTRA_TIME_CONTROL, spinner.getSelectedItem().toString());
                    startActivity(intent);
                });
    }
}
