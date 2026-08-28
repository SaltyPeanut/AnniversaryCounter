package de.anniversarycounter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Einstellung fuer Schriftart und Textfarbe, getrennt fuer "Text" (Name, Nachricht,
 * Titel) und "Zahlen" (die grosse Zaehler-Anzeige). Ueber den "Wie ... uebernehmen"-Button
 * laesst sich die Einstellung des einen Reiters auf den anderen kopieren.
 *
 * Ohne {@link #EXTRA_COUNTER_ID} wird die globale Einstellung aus {@link AppSettings}
 * bearbeitet (gilt fuer alle Zaehler ohne eigene Schriftart/Farbe). Mit {@link #EXTRA_COUNTER_ID}
 * werden stattdessen die eigenen Werte dieses Zaehlers bearbeitet, die nur greifen, wenn
 * {@link AnniversaryCounter#customAppearance} an ist.
 */
public class AppearanceSettingsActivity extends AppCompatActivity {

    public static final String EXTRA_COUNTER_ID = "counter_id";

    private static final int SWATCH_SIZE_DP = 44;
    private static final int SWATCH_MARGIN_DP = 8;
    private static final int SWATCH_STROKE_DP = 3;

    /** null = globale Einstellung wird bearbeitet, sonst die eigenen Werte dieses Zaehlers. */
    private AnniversaryCounter counter;

    private AppSettings.Target currentTarget = AppSettings.Target.TEXT;
    private int selectedColor;

    private Button btnTabText;
    private Button btnTabNumbers;
    private Button btnCopyFromOther;
    private TextView tvPreview;
    private RadioGroup fontGroup;
    private GridLayout colorGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appearance_settings);

        String counterId = getIntent().getStringExtra(EXTRA_COUNTER_ID);
        if (counterId != null) {
            counter = AnniversaryCounterStore.find(this, counterId);
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvAppearanceTitle);
        tvTitle.setText(counter != null
                ? getString(R.string.appearance_settings_title_counter, counter.displayLabel(this))
                : getString(R.string.appearance_settings_title));

        btnTabText = findViewById(R.id.btnTabText);
        btnTabNumbers = findViewById(R.id.btnTabNumbers);
        btnCopyFromOther = findViewById(R.id.btnCopyFromOther);
        tvPreview = findViewById(R.id.tvPreview);
        fontGroup = findViewById(R.id.fontOptionsGroup);
        colorGrid = findViewById(R.id.colorGrid);

        btnTabText.setOnClickListener(v -> selectTab(AppSettings.Target.TEXT));
        btnTabNumbers.setOnClickListener(v -> selectTab(AppSettings.Target.NUMBERS));
        btnCopyFromOther.setOnClickListener(v -> {
            AppSettings.Target other = otherTarget();
            saveFontFamily(other, fontFamilyFor(currentTarget));
            saveColorValue(other, colorValueFor(currentTarget));
        });

        selectTab(currentTarget);
    }

    /** Roher, gespeicherter Schriftfamilien-Wert (ohne Aufloesung); je nach Modus global oder pro Zaehler. */
    private String fontFamilyFor(AppSettings.Target target) {
        if (counter != null) {
            String family = target == AppSettings.Target.TEXT ? counter.fontFamilyText : counter.fontFamilyNumbers;
            return family == null ? AppSettings.FONT_DEFAULT : family;
        }
        return AppSettings.fontFamily(this, target);
    }

    /** Roher, gespeicherter Farbwert (kann {@link AppSettings#COLOR_DEFAULT} sein). */
    private int colorValueFor(AppSettings.Target target) {
        if (counter != null) {
            return target == AppSettings.Target.TEXT ? counter.colorValueText : counter.colorValueNumbers;
        }
        return AppSettings.textColorValue(this, target);
    }

    private int resolvedColorValue(AppSettings.Target target) {
        int raw = colorValueFor(target);
        return raw == AppSettings.COLOR_DEFAULT ? ContextCompat.getColor(this, R.color.text_primary) : raw;
    }

    private void saveFontFamily(AppSettings.Target target, String family) {
        if (counter != null) {
            if (target == AppSettings.Target.TEXT) {
                counter.fontFamilyText = family;
            } else {
                counter.fontFamilyNumbers = family;
            }
            AnniversaryCounterStore.update(this, counter);
        } else {
            AppSettings.setFontFamily(this, target, family);
        }
    }

    private void saveColorValue(AppSettings.Target target, int color) {
        if (counter != null) {
            if (target == AppSettings.Target.TEXT) {
                counter.colorValueText = color;
            } else {
                counter.colorValueNumbers = color;
            }
            AnniversaryCounterStore.update(this, counter);
        } else {
            AppSettings.setTextColorValue(this, target, color);
        }
    }

    private AppSettings.Target otherTarget() {
        return currentTarget == AppSettings.Target.TEXT
                ? AppSettings.Target.NUMBERS
                : AppSettings.Target.TEXT;
    }

    private void selectTab(AppSettings.Target target) {
        currentTarget = target;
        selectedColor = resolvedColorValue(target);

        boolean isText = target == AppSettings.Target.TEXT;
        btnTabText.setBackgroundResource(isText ? R.drawable.btn_outline_selected : R.drawable.btn_outline);
        btnTabNumbers.setBackgroundResource(isText ? R.drawable.btn_outline : R.drawable.btn_outline_selected);
        tvPreview.setText(isText ? R.string.appearance_preview_text : R.string.appearance_preview_numbers);
        btnCopyFromOther.setText(isText ? R.string.appearance_apply_to_numbers : R.string.appearance_apply_to_text);

        buildFontOptions();
        buildColorSwatches();
        applyPreview();
    }

    private void buildFontOptions() {
        fontGroup.setOnCheckedChangeListener(null);
        fontGroup.removeAllViews();
        String currentFamily = fontFamilyFor(currentTarget);

        for (AppSettings.FontOption option : AppSettings.FONT_OPTIONS) {
            RadioButton button = new RadioButton(this);
            button.setText(option.labelRes);
            button.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            button.setTypeface(option.family.isEmpty()
                    ? Typeface.DEFAULT
                    : Typeface.create(option.family, Typeface.NORMAL));
            button.setPadding(0, dp(8), 0, dp(8));
            button.setTag(option.family);
            button.setChecked(option.family.equals(currentFamily));
            fontGroup.addView(button);
        }

        fontGroup.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            RadioButton checked = radioGroup.findViewById(checkedId);
            if (checked == null) {
                return;
            }
            String family = (String) checked.getTag();
            saveFontFamily(currentTarget, family);
            applyPreview();
        });
    }

    private void buildColorSwatches() {
        colorGrid.removeAllViews();
        int size = dp(SWATCH_SIZE_DP);
        int margin = dp(SWATCH_MARGIN_DP);

        for (int color : AppSettings.COLOR_PALETTE) {
            View swatch = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(params);
            swatch.setContentDescription(getString(R.string.color_swatch_description));
            swatch.setBackground(swatchDrawable(color, color == selectedColor));
            swatch.setOnClickListener(v -> {
                selectedColor = color;
                saveColorValue(currentTarget, color);
                applyPreview();
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    View child = colorGrid.getChildAt(i);
                    child.setBackground(swatchDrawable(
                            AppSettings.COLOR_PALETTE[i], AppSettings.COLOR_PALETTE[i] == selectedColor));
                }
            });
            colorGrid.addView(swatch);
        }
    }

    private GradientDrawable swatchDrawable(int color, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        if (selected) {
            drawable.setStroke(dp(SWATCH_STROKE_DP), Color.WHITE);
        }
        return drawable;
    }

    private void applyPreview() {
        String family = fontFamilyFor(currentTarget);
        tvPreview.setTypeface(family.isEmpty() ? Typeface.DEFAULT : Typeface.create(family, Typeface.NORMAL));
        tvPreview.setTextColor(selectedColor);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
