package de.anniversarycounter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Auswahlmaske fuer den Kalender-Import: zeigt alle jaehrlichen Termine an, der Nutzer waehlt
 * aus, welche zu Zaehlern werden. Bereits vorhandene Eintraege sind nicht waehlbar, damit ein
 * zweiter Import keine Duplikate erzeugt.
 */
public class CalendarImportActivity extends AppCompatActivity {

    private static final int REQUEST_CALENDAR_PERMISSION = 1;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2;
    private static final String STATE_SELECTED_KEYS = "selected_keys";

    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmpty;
    private View optionsGroup;
    private CheckBox cbCountdown;
    private CheckBox cbShowAge;
    private View actionsRow;
    private Button btnSelectAll;
    private Button btnImport;

    private CalendarImportAdapter adapter;
    private Set<String> restoredSelection = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_import);

        recyclerView = findViewById(R.id.rvCalendarEvents);
        progress = findViewById(R.id.progressCalendar);
        tvEmpty = findViewById(R.id.tvCalendarEmpty);
        optionsGroup = findViewById(R.id.calendarOptions);
        cbCountdown = findViewById(R.id.cbImportCountdown);
        cbShowAge = findViewById(R.id.cbImportShowAge);
        actionsRow = findViewById(R.id.calendarActionsRow);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnImport = findViewById(R.id.btnImportSelected);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSelectAll.setOnClickListener(v -> adapter.setAllSelected(!allSelected()));
        btnImport.setOnClickListener(v -> importSelected());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            ArrayList<String> keys = savedInstanceState.getStringArrayList(STATE_SELECTED_KEYS);
            if (keys != null) {
                restoredSelection = new HashSet<>(keys);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            loadEvents();
        } else if (savedInstanceState == null) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR}, REQUEST_CALENDAR_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            finish();
            return;
        }
        if (requestCode != REQUEST_CALENDAR_PERMISSION) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadEvents();
        } else {
            Toast.makeText(this, R.string.calendar_import_permission_denied, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_SELECTED_KEYS,
                adapter != null ? adapter.selectedKeys() : new ArrayList<>(restoredSelection));
    }

    private void loadEvents() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<CalendarBirthdayReader.Candidate> candidates = CalendarBirthdayReader.read(this);
            markExisting(candidates);
            runOnUiThread(() -> {
                if (!isDestroyed()) {
                    showEvents(candidates);
                }
            });
        }).start();
    }

    private void markExisting(List<CalendarBirthdayReader.Candidate> candidates) {
        Set<String> existing = new HashSet<>();
        for (AnniversaryCounter counter : AnniversaryCounterStore.loadAll(this)) {
            if (counter.name != null) {
                existing.add(CalendarBirthdayReader.Candidate.key(counter.name, counter.start.toLocalDate()));
            }
        }
        for (CalendarBirthdayReader.Candidate candidate : candidates) {
            candidate.alreadyImported = existing.contains(candidate.key());
        }
    }

    private void showEvents(List<CalendarBirthdayReader.Candidate> candidates) {
        progress.setVisibility(View.GONE);
        if (candidates.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        adapter = new CalendarImportAdapter(this, candidates, restoredSelection, this::updateActions);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
        optionsGroup.setVisibility(View.VISIBLE);
        actionsRow.setVisibility(View.VISIBLE);
        updateActions();
    }

    private boolean allSelected() {
        return adapter.selectedCount() == adapter.selectableCount();
    }

    private void updateActions() {
        int count = adapter.selectedCount();
        btnImport.setEnabled(count > 0);
        btnImport.setText(getString(R.string.calendar_import_button, count));
        btnSelectAll.setEnabled(adapter.selectableCount() > 0);
        btnSelectAll.setText(count > 0 && allSelected()
                ? R.string.calendar_import_deselect_all
                : R.string.calendar_import_select_all);
    }

    private void importSelected() {
        List<CalendarBirthdayReader.Candidate> selected = adapter.selectedCandidates();
        List<AnniversaryCounter> all = AnniversaryCounterStore.loadAll(this);
        List<AnniversaryCounter> imported = new ArrayList<>();
        for (CalendarBirthdayReader.Candidate candidate : selected) {
            AnniversaryCounter counter = new AnniversaryCounter(
                    UUID.randomUUID().toString(), candidate.title, candidate.start(), null);
            // Der Name des Termins reicht als Beschriftung, die Standard-Ueberschrift waere nur doppelt.
            counter.showMessage = false;
            counter.countdownToNext = cbCountdown.isChecked();
            counter.showAge = cbShowAge.isChecked();
            // Importiert werden alle jaehrlichen Termine, nicht nur Geburtstage.
            counter.isBirthday = counter.showAge;
            counter.enabledStandardMilestones.add(Milestone.Standard.YEARLY.name());
            all.add(counter);
            imported.add(counter);
        }
        AnniversaryCounterStore.saveAll(this, all);
        for (AnniversaryCounter counter : imported) {
            MilestoneScheduler.scheduleForCounter(this, counter);
        }

        Toast.makeText(this, getResources().getQuantityString(
                R.plurals.calendar_import_success, selected.size(), selected.size()),
                Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            return; // finish() erfolgt in onRequestPermissionsResult
        }
        finish();
    }
}
