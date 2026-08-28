package de.anniversarycounter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Wird beim allerersten Start statt {@link MainActivity} gezeigt, solange noch keine Sprache
 * gewaehlt wurde (siehe {@link AppSettings#hasLanguage(android.content.Context)}). Die
 * Sprachnamen bleiben absichtlich unuebersetzt, damit sie unabhaengig von der aktuell aktiven
 * App-Sprache lesbar sind.
 */
public class LanguageSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        bindLanguageButton(R.id.btnLangDe, AppSettings.LANG_DE);
        bindLanguageButton(R.id.btnLangEn, AppSettings.LANG_EN);
        bindLanguageButton(R.id.btnLangEs, AppSettings.LANG_ES);
        bindLanguageButton(R.id.btnLangFr, AppSettings.LANG_FR);
    }

    private void bindLanguageButton(int viewId, String languageCode) {
        Button button = findViewById(viewId);
        button.setOnClickListener(v -> {
            AppSettings.setLanguage(this, languageCode);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
