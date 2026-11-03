package de.anniversarycounter;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Globale Einstellung fuer Datums-, Uhrzeit- und Zahlenformat. Standard ist jeweils
 * "Automatisch" (nach App-Sprache bzw. 12h/24h-Systemeinstellung), siehe {@link DisplayFormats}.
 * Jede Option zeigt ein Beispiel im jeweiligen Format; oben steht eine Live-Vorschau.
 */
public class FormatSettingsActivity extends AppCompatActivity {

    /** Fester Beispielzeitpunkt: Tag != Monat, damit TT/MM und MM/TT unterscheidbar sind; Nachmittag fuer 12h. */
    private static final LocalDateTime SAMPLE_DATE_TIME = LocalDateTime.of(2026, 11, 3, 14, 30);
    private static final long SAMPLE_NUMBER = 1_234_567L;

    private TextView tvPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_format_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvPreview = findViewById(R.id.tvFormatPreview);

        buildOptions(findViewById(R.id.dateStyleGroup),
                AppSettings.DateStyle.values(),
                AppSettings.dateStyle(this),
                this::dateLabel,
                style -> AppSettings.setDateStyle(this, style));

        buildOptions(findViewById(R.id.timeStyleGroup),
                AppSettings.TimeStyle.values(),
                AppSettings.timeStyle(this),
                this::timeLabel,
                style -> AppSettings.setTimeStyle(this, style));

        buildOptions(findViewById(R.id.numberStyleGroup),
                AppSettings.NumberStyle.values(),
                AppSettings.numberStyle(this),
                this::numberLabel,
                style -> AppSettings.setNumberStyle(this, style));

        updatePreview();
    }

    private <E extends Enum<E>> void buildOptions(RadioGroup group, E[] values, E current,
                                                  Function<E, String> label, Consumer<E> save) {
        group.removeAllViews();
        for (E value : values) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setText(label.apply(value));
            button.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            button.setPadding(0, dp(8), 0, dp(8));
            button.setTag(value);
            group.addView(button);
            if (value == current) {
                group.check(button.getId());
            }
        }
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            RadioButton checked = radioGroup.findViewById(checkedId);
            if (checked == null) {
                return;
            }
            @SuppressWarnings("unchecked")
            E value = (E) checked.getTag();
            save.accept(value);
            updatePreview();
            AnniversaryCounterWidgetProvider.updateAllWidgets(this);
        });
    }

    private String dateLabel(AppSettings.DateStyle style) {
        String example = DisplayFormats.formatDate(this, SAMPLE_DATE_TIME.toLocalDate(), style);
        return style == AppSettings.DateStyle.AUTO ? getString(R.string.format_auto, example) : example;
    }

    private String timeLabel(AppSettings.TimeStyle style) {
        String example = DisplayFormats.formatTime(this, SAMPLE_DATE_TIME.toLocalTime(), style);
        switch (style) {
            case H24:
                return getString(R.string.format_time_24h, example);
            case H12:
                return getString(R.string.format_time_12h, example);
            case AUTO:
            default:
                return getString(R.string.format_time_auto_system, example);
        }
    }

    private String numberLabel(AppSettings.NumberStyle style) {
        String example = DisplayFormats.formatNumber(this, SAMPLE_NUMBER, style);
        switch (style) {
            case AUTO:
                return getString(R.string.format_auto, example);
            case NONE:
                return getString(R.string.format_number_none, example);
            default:
                return example;
        }
    }

    private void updatePreview() {
        String start = getString(R.string.start_prefix, DisplayFormats.formatDateTime(this, SAMPLE_DATE_TIME));
        String total = getString(R.string.total_seconds, DisplayFormats.formatNumber(this, SAMPLE_NUMBER));
        tvPreview.setText(start + "\n" + total);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
