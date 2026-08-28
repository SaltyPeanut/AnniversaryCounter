package de.anniversarycounter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

/** Untermenü zum Setzen/Entfernen des eigenen Hintergrundbilds eines einzelnen Zaehlers. */
public class BackgroundSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int REQUEST_PICK_BACKGROUND = 200;

    private AnniversaryCounter counter;
    private ImageView ivPreview;
    private Button btnRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_settings);

        String counterId = getIntent().getStringExtra(EXTRA_COUNTER_ID);
        counter = AnniversaryCounterStore.find(this, counterId);
        if (counter == null) {
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvBackgroundTitle);
        tvTitle.setText(getString(R.string.background_settings_title, counter.displayLabel(this)));

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ivPreview = findViewById(R.id.ivBackgroundPreview);
        btnRemove = findViewById(R.id.btnRemoveBackground);
        Button btnPick = findViewById(R.id.btnPickBackground);

        refreshPreview();

        btnPick.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            startActivityForResult(pick, REQUEST_PICK_BACKGROUND);
        });

        btnRemove.setOnClickListener(v -> {
            BackgroundImageStore.clear(this, counter.id);
            refreshPreview();
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_BACKGROUND || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        try {
            BackgroundImageStore.saveFromUri(this, counter.id, data.getData());
            refreshPreview();
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        } catch (IOException e) {
            Toast.makeText(this, R.string.background_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshPreview() {
        boolean hasCustom = BackgroundImageStore.hasCustomBackground(this, counter.id);
        if (hasCustom) {
            ivPreview.setImageURI(Uri.fromFile(BackgroundImageStore.backgroundFile(this, counter.id)));
        } else {
            ivPreview.setImageResource(R.drawable.bg_hearts);
        }
        btnRemove.setEnabled(hasCustom);
    }
}
