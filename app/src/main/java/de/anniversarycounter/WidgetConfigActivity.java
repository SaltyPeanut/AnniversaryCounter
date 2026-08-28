package de.anniversarycounter;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Konfigurationsdialog, der beim Hinzufuegen eines Widgets (und bei erneutem
 * Aufruf ueber "Widget konfigurieren") angezeigt wird. Legt pro Widget-Instanz
 * fest, welche Einheiten gezeigt werden und ob die Nachricht als Ueberschrift
 * erscheint.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Spinner spCounter;
    private List<AnniversaryCounter> counters;
    private CheckBox cbYears;
    private CheckBox cbMonths;
    private CheckBox cbDays;
    private CheckBox cbHours;
    private CheckBox cbShowSubtitle;
    private CheckBox cbShowBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_config);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        counters = AnniversaryCounterStore.loadAll(this);
        if (counters.isEmpty()) {
            Toast.makeText(this, R.string.widget_no_counters, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spCounter = findViewById(R.id.spCounter);
        cbYears = findViewById(R.id.cbYears);
        cbMonths = findViewById(R.id.cbMonths);
        cbDays = findViewById(R.id.cbDays);
        cbHours = findViewById(R.id.cbHours);
        cbShowSubtitle = findViewById(R.id.cbShowSubtitle);
        cbShowBackground = findViewById(R.id.cbShowBackground);
        Button btnSave = findViewById(R.id.btnSaveWidget);

        List<String> labels = new ArrayList<>();
        for (AnniversaryCounter counter : counters) {
            labels.add(counter.displayLabel(this));
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, labels);
        spCounter.setAdapter(spinnerAdapter);

        String currentCounterId = AnniversaryCounterWidgetProvider.loadCounterId(this, appWidgetId);
        int selectIndex = 0;
        for (int i = 0; i < counters.size(); i++) {
            if (counters.get(i).id.equals(currentCounterId)) {
                selectIndex = i;
                break;
            }
        }
        spCounter.setSelection(selectIndex);

        Set<DateDiff.Unit> current = AnniversaryCounterWidgetProvider.loadUnits(this, appWidgetId);
        cbYears.setChecked(current.contains(DateDiff.Unit.YEARS));
        cbMonths.setChecked(current.contains(DateDiff.Unit.MONTHS));
        cbDays.setChecked(current.contains(DateDiff.Unit.DAYS));
        cbHours.setChecked(current.contains(DateDiff.Unit.HOURS));
        cbShowSubtitle.setChecked(AnniversaryCounterWidgetProvider.loadShowSubtitle(this, appWidgetId));
        cbShowBackground.setChecked(AnniversaryCounterWidgetProvider.loadShowBackground(this, appWidgetId));
        updateBackgroundCheckboxState(counters.get(selectIndex));

        spCounter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateBackgroundCheckboxState(counters.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSave.setOnClickListener(v -> save());
    }

    /** Der Hintergrund-Toggle ist nur nutzbar, wenn der gewaehlte Zaehler ein eigenes Bild hat. */
    private void updateBackgroundCheckboxState(AnniversaryCounter counter) {
        boolean hasBackground = BackgroundImageStore.hasCustomBackground(this, counter.id);
        cbShowBackground.setEnabled(hasBackground);
        if (!hasBackground) {
            cbShowBackground.setChecked(false);
        }
    }

    private void save() {
        Set<DateDiff.Unit> units = EnumSet.noneOf(DateDiff.Unit.class);
        if (cbYears.isChecked()) {
            units.add(DateDiff.Unit.YEARS);
        }
        if (cbMonths.isChecked()) {
            units.add(DateDiff.Unit.MONTHS);
        }
        if (cbDays.isChecked()) {
            units.add(DateDiff.Unit.DAYS);
        }
        if (cbHours.isChecked()) {
            units.add(DateDiff.Unit.HOURS);
        }

        if (units.isEmpty()) {
            Toast.makeText(this, R.string.widget_select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }

        AnniversaryCounter selected = counters.get(spCounter.getSelectedItemPosition());
        AnniversaryCounterWidgetProvider.saveCounterId(this, appWidgetId, selected.id);
        AnniversaryCounterWidgetProvider.saveUnits(this, appWidgetId, units);
        AnniversaryCounterWidgetProvider.saveShowSubtitle(this, appWidgetId, cbShowSubtitle.isChecked());
        AnniversaryCounterWidgetProvider.saveShowBackground(this, appWidgetId, cbShowBackground.isChecked());
        AnniversaryCounterWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), appWidgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}
